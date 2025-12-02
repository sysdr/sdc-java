package com.example.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class RaftNode {
    
    private volatile NodeState state = NodeState.FOLLOWER;
    private volatile long currentTerm = 0;
    private volatile String votedFor = null;
    private volatile String currentLeader = null;
    private volatile Instant lastHeartbeat = Instant.now();
    private volatile long commitIndex = 0;
    private volatile long lastApplied = 0;
    
    @Value("${node.id}")
    private String nodeId;
    
    @Value("${node.port}")
    private int nodePort;
    
    @Autowired
    private ClusterConfig clusterConfig;
    
    private List<String> clusterNodes;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private final Random random = new Random();
    private final RestTemplate restTemplate = new RestTemplate();
    private final LogEntryRepository logRepository;
    private final MetricsService metricsService;
    
    private ScheduledFuture<?> electionTimeout;
    private ScheduledFuture<?> heartbeatTask;
    
    // Next index to send to each follower
    private final Map<String, Long> nextIndex = new ConcurrentHashMap<>();
    // Highest index replicated on each follower
    private final Map<String, Long> matchIndex = new ConcurrentHashMap<>();
    
    public RaftNode(LogEntryRepository logRepository, MetricsService metricsService) {
        this.logRepository = logRepository;
        this.metricsService = metricsService;
    }
    
    @PostConstruct
    public void initialize() {
        clusterNodes = clusterConfig.getNodesList();
        log.info("Initializing Raft node: {} on port {}", nodeId, nodePort);
        log.info("Cluster nodes: {}", clusterNodes);
        scheduleElectionTimeout();
    }
    
    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
    
    private void scheduleElectionTimeout() {
        if (electionTimeout != null) {
            electionTimeout.cancel(false);
        }
        
        // Random timeout between 150-300ms
        long timeoutMs = 150 + random.nextInt(150);
        electionTimeout = scheduler.schedule(this::startElection, timeoutMs, TimeUnit.MILLISECONDS);
        log.debug("Election timeout scheduled for {}ms", timeoutMs);
    }
    
    private synchronized void startElection() {
        if (state == NodeState.LEADER) {
            return;
        }
        
        log.info("Starting election for term {}", currentTerm + 1);
        currentTerm++;
        state = NodeState.CANDIDATE;
        votedFor = nodeId;
        lastHeartbeat = Instant.now();
        metricsService.incrementElections();
        
        AtomicInteger votesReceived = new AtomicInteger(1); // Vote for self
        int requiredVotes = (clusterNodes.size() + 1) / 2 + 1;
        
        CountDownLatch voteLatch = new CountDownLatch(clusterNodes.size());
        
        for (String node : clusterNodes) {
            scheduler.submit(() -> {
                try {
                    if (requestVote(node)) {
                        votesReceived.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.warn("Failed to request vote from {}: {}", node, e.getMessage());
                } finally {
                    voteLatch.countDown();
                }
            });
        }
        
        // Wait for votes with timeout
        try {
            voteLatch.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        if (votesReceived.get() >= requiredVotes && state == NodeState.CANDIDATE) {
            becomeLeader();
        } else {
            log.info("Election failed. Votes: {}/{}", votesReceived.get(), requiredVotes);
            state = NodeState.FOLLOWER;
            scheduleElectionTimeout();
        }
    }
    
    private boolean requestVote(String nodeUrl) {
        try {
            String url = String.format("%s/raft/vote", nodeUrl);
            VoteRequest request = new VoteRequest(currentTerm, nodeId, getLastLogIndex(), getLastLogTerm());
            VoteResponse response = restTemplate.postForObject(url, request, VoteResponse.class);
            return response != null && response.voteGranted();
        } catch (Exception e) {
            return false;
        }
    }
    
    private synchronized void becomeLeader() {
        log.info("Node {} became leader for term {}", nodeId, currentTerm);
        state = NodeState.LEADER;
        currentLeader = nodeId;
        metricsService.recordLeaderElection(nodeId);
        
        // Initialize nextIndex and matchIndex for all followers
        long lastLogIndex = getLastLogIndex();
        for (String node : clusterNodes) {
            nextIndex.put(node, lastLogIndex + 1);
            matchIndex.put(node, 0L);
        }
        
        startHeartbeats();
        scheduleElectionTimeout(); // Keep timeout running to detect partitions
    }
    
    private void startHeartbeats() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
        
        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            if (state != NodeState.LEADER) {
                return;
            }
            
            sendHeartbeats();
        }, 0, 50, TimeUnit.MILLISECONDS);
    }
    
    private void sendHeartbeats() {
        AtomicInteger successfulHeartbeats = new AtomicInteger(1); // Count self
        CountDownLatch latch = new CountDownLatch(clusterNodes.size());
        
        for (String node : clusterNodes) {
            scheduler.submit(() -> {
                try {
                    if (sendHeartbeat(node)) {
                        successfulHeartbeats.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.warn("Heartbeat to {} failed: {}", node, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(40, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Step down if cannot reach majority
        int requiredNodes = (clusterNodes.size() + 1) / 2 + 1;
        if (successfulHeartbeats.get() < requiredNodes) {
            log.warn("Lost majority contact. Stepping down. {}/{} nodes reachable", 
                     successfulHeartbeats.get(), clusterNodes.size() + 1);
            stepDown(currentTerm);
        }
        
        metricsService.recordHeartbeat(successfulHeartbeats.get(), clusterNodes.size() + 1);
    }
    
    private boolean sendHeartbeat(String nodeUrl) {
        try {
            String url = String.format("%s/raft/heartbeat", nodeUrl);
            Long prevLogIndex = nextIndex.getOrDefault(nodeUrl, 0L) - 1;
            Long prevLogTerm = prevLogIndex > 0 ? getLogTermAtIndex(prevLogIndex) : 0L;
            
            HeartbeatRequest request = new HeartbeatRequest(
                currentTerm, nodeId, prevLogIndex, prevLogTerm, 
                Collections.emptyList(), commitIndex
            );
            
            HeartbeatResponse response = restTemplate.postForObject(url, request, HeartbeatResponse.class);
            
            if (response != null && response.success()) {
                return true;
            } else if (response != null && response.term() > currentTerm) {
                stepDown(response.term());
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    private synchronized void stepDown(long newTerm) {
        if (newTerm > currentTerm) {
            log.info("Stepping down. New term: {}", newTerm);
            currentTerm = newTerm;
            votedFor = null;
        }
        
        if (state == NodeState.LEADER && heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
        
        state = NodeState.FOLLOWER;
        currentLeader = null;
        scheduleElectionTimeout();
    }
    
    public synchronized WriteResponse handleWrite(WriteRequest request) {
        if (state != NodeState.LEADER) {
            return WriteResponse.redirect(currentLeader);
        }
        
        try {
            // Create log entry
            LogEntry entry = new LogEntry();
            entry.setTerm(currentTerm);
            entry.setLogIndex(getLastLogIndex() + 1);
            entry.setData(request.data());
            entry.setSource(request.source());
            entry.setTimestamp(Instant.now());
            entry.setCommitted(false);
            
            // Write locally
            logRepository.save(entry);
            metricsService.recordWrite();
            
            // Replicate to followers
            int replicas = replicateToFollowers(entry);
            
            int requiredReplicas = (clusterNodes.size() + 1) / 2 + 1;
            if (replicas >= requiredReplicas) {
                entry.setCommitted(true);
                logRepository.save(entry);
                commitIndex = entry.getLogIndex();
                return WriteResponse.success(entry.getId(), nodeId);
            }
            
            return WriteResponse.failure("Failed to replicate to majority");
        } catch (Exception e) {
            log.error("Write failed", e);
            return WriteResponse.failure(e.getMessage());
        }
    }
    
    private int replicateToFollowers(LogEntry entry) {
        AtomicInteger replicas = new AtomicInteger(1); // Count leader
        CountDownLatch latch = new CountDownLatch(clusterNodes.size());
        
        for (String node : clusterNodes) {
            scheduler.submit(() -> {
                try {
                    if (replicateEntry(node, entry)) {
                        replicas.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return replicas.get();
    }
    
    private boolean replicateEntry(String nodeUrl, LogEntry entry) {
        try {
            String url = String.format("%s/raft/append", nodeUrl);
            AppendRequest request = new AppendRequest(
                currentTerm, nodeId, entry.getLogIndex() - 1,
                entry.getTerm(), List.of(entry), commitIndex
            );
            
            AppendResponse response = restTemplate.postForObject(url, request, AppendResponse.class);
            return response != null && response.success();
        } catch (Exception e) {
            return false;
        }
    }
    
    private long getLastLogIndex() {
        Long maxIndex = logRepository.findMaxLogIndex();
        return maxIndex != null ? maxIndex : 0L;
    }
    
    private long getLastLogTerm() {
        long lastIndex = getLastLogIndex();
        return lastIndex > 0 ? getLogTermAtIndex(lastIndex) : 0L;
    }
    
    private long getLogTermAtIndex(long index) {
        // Simplified - in production, query by index
        return currentTerm;
    }
    
    public NodeState getState() {
        return state;
    }
    
    public long getCurrentTerm() {
        return currentTerm;
    }
    
    public String getCurrentLeader() {
        return currentLeader;
    }
    
    public boolean isLeader() {
        return state == NodeState.LEADER;
    }
}

// Request/Response records
record VoteRequest(long term, String candidateId, long lastLogIndex, long lastLogTerm) {}
record VoteResponse(long term, boolean voteGranted) {}
record HeartbeatRequest(long term, String leaderId, long prevLogIndex, long prevLogTerm, 
                        List<LogEntry> entries, long leaderCommit) {}
record HeartbeatResponse(long term, boolean success) {}
record AppendRequest(long term, String leaderId, long prevLogIndex, long prevLogTerm,
                     List<LogEntry> entries, long leaderCommit) {}
record AppendResponse(long term, boolean success) {}
record WriteRequest(String data, String source) {}
record WriteResponse(boolean success, Long entryId, String leaderId, String message) {
    public static WriteResponse success(Long entryId, String leaderId) {
        return new WriteResponse(true, entryId, leaderId, "Write successful");
    }
    public static WriteResponse failure(String message) {
        return new WriteResponse(false, null, null, message);
    }
    public static WriteResponse redirect(String leaderId) {
        return new WriteResponse(false, null, leaderId, "Not leader. Redirect to: " + leaderId);
    }
}
