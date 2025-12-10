package com.example.cluster.service;

import com.example.cluster.model.NodeState;
import com.example.cluster.model.NodeState.NodeStatus;
import com.example.cluster.model.MembershipDigest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ClusterMembershipService {
    private static final Logger logger = LoggerFactory.getLogger(ClusterMembershipService.class);
    
    private final ConcurrentHashMap<String, NodeState> membershipView;
    private final RestTemplate restTemplate;
    private final HealthScoreService healthScoreService;
    private final PhiAccrualFailureDetector failureDetector;
    private final MeterRegistry meterRegistry;
    
    @Value("${cluster.node.id}")
    private String nodeId;
    
    @Value("${cluster.node.port:8081}")
    private int nodePort;
    
    @Value("${cluster.heartbeat.interval:1000}")
    private long heartbeatInterval;
    
    @Value("${cluster.failure.timeout:15000}")
    private long failureTimeout;
    
    @Value("${cluster.gossip.fanout:3}")
    private int gossipFanout;
    
    private int generation;
    private Counter heartbeatsSent;
    private Counter heartbeatsReceived;
    private Counter failuresDetected;
    
    public ClusterMembershipService(RestTemplate restTemplate, 
                                    HealthScoreService healthScoreService,
                                    PhiAccrualFailureDetector failureDetector,
                                    MeterRegistry meterRegistry) {
        this.membershipView = new ConcurrentHashMap<>();
        this.restTemplate = restTemplate;
        this.healthScoreService = healthScoreService;
        this.failureDetector = failureDetector;
        this.meterRegistry = meterRegistry;
        this.generation = 1;
    }
    
    @PostConstruct
    public void initialize() {
        // Register this node in the cluster
        NodeState myState = new NodeState(nodeId, getLocalIpAddress(), nodePort);
        myState.setAvailabilityZone(System.getenv("AVAILABILITY_ZONE"));
        membershipView.put(nodeId, myState);
        
        // Initialize metrics
        heartbeatsSent = Counter.builder("cluster.heartbeats.sent")
            .description("Number of heartbeats sent")
            .register(meterRegistry);
        heartbeatsReceived = Counter.builder("cluster.heartbeats.received")
            .description("Number of heartbeats received")
            .register(meterRegistry);
        failuresDetected = Counter.builder("cluster.failures.detected")
            .description("Number of node failures detected")
            .register(meterRegistry);
        
        logger.info("Cluster coordinator initialized with node ID: {}", nodeId);
    }
    
    @Scheduled(fixedRateString = "${cluster.heartbeat.interval:1000}")
    public void sendHeartbeat() {
        try {
            NodeState myState = membershipView.get(nodeId);
            myState.setGeneration(generation);
            myState.setHealthScore(healthScoreService.getCurrentScore());
            myState.updateHeartbeat();
            
            gossipToRandomPeers(myState);
            heartbeatsSent.increment();
            
        } catch (Exception e) {
            logger.error("Error sending heartbeat", e);
        }
    }
    
    @Scheduled(fixedRate = 5000)
    public void detectFailures() {
        long currentTime = System.currentTimeMillis();
        
        membershipView.values().forEach(node -> {
            if (node.getNodeId().equals(nodeId)) return;
            
            long timeSinceLastHeartbeat = node.getMillisSinceLastHeartbeat();
            double phi = failureDetector.phi(timeSinceLastHeartbeat);
            node.setPhiScore(phi);
            
            // Phi threshold of 8 corresponds to ~99.9% confidence of failure
            if (phi > 8.0 && node.getStatus() == NodeStatus.HEALTHY) {
                markNodeAsSuspected(node);
            } else if (timeSinceLastHeartbeat > failureTimeout * 2 
                       && node.getStatus() == NodeStatus.SUSPECTED) {
                markNodeAsFailed(node);
            }
        });
    }
    
    @Scheduled(fixedRate = 10000)
    public void antiEntropyRepair() {
        // Full state exchange with random peer to catch missed gossip
        List<String> peers = getHealthyPeers();
        if (!peers.isEmpty()) {
            String randomPeer = peers.get(new Random().nextInt(peers.size()));
            try {
                MembershipDigest fullDigest = createFullDigest();
                String url = String.format("http://%s/cluster/anti-entropy", 
                    membershipView.get(randomPeer).getIpAddress());
                restTemplate.postForObject(url, fullDigest, MembershipDigest.class);
            } catch (Exception e) {
                logger.debug("Anti-entropy with {} failed: {}", randomPeer, e.getMessage());
            }
        }
    }
    
    public void receiveGossip(MembershipDigest digest) {
        heartbeatsReceived.increment();
        
        digest.getMembers().forEach((nodeId, receivedState) -> {
            membershipView.compute(nodeId, (key, existingState) -> {
                if (existingState == null) {
                    logger.info("Discovered new node: {}", nodeId);
                    return receivedState;
                } else if (receivedState.getGeneration() > existingState.getGeneration()) {
                    logger.info("Updating node {} from generation {} to {}", 
                        nodeId, existingState.getGeneration(), receivedState.getGeneration());
                    return receivedState;
                }
                return existingState;
            });
        });
    }
    
    private void gossipToRandomPeers(NodeState myState) {
        List<String> peers = selectRandomPeers(gossipFanout);
        MembershipDigest digest = createDigest(myState);
        
        peers.forEach(peerId -> {
            try {
                NodeState peer = membershipView.get(peerId);
                String url = String.format("http://%s:%d/cluster/gossip", 
                    peer.getIpAddress(), peer.getPort());
                restTemplate.postForObject(url, digest, Void.class);
            } catch (Exception e) {
                logger.debug("Gossip to {} failed: {}", peerId, e.getMessage());
            }
        });
    }
    
    private List<String> selectRandomPeers(int count) {
        List<String> peers = getHealthyPeers();
        Collections.shuffle(peers);
        return peers.stream().limit(count).collect(Collectors.toList());
    }
    
    private List<String> getHealthyPeers() {
        return membershipView.entrySet().stream()
            .filter(e -> !e.getKey().equals(nodeId))
            .filter(e -> e.getValue().getStatus() == NodeStatus.HEALTHY)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    private MembershipDigest createDigest(NodeState myState) {
        MembershipDigest digest = new MembershipDigest(nodeId, generation);
        digest.addMember(nodeId, myState);
        return digest;
    }
    
    private MembershipDigest createFullDigest() {
        MembershipDigest digest = new MembershipDigest(nodeId, generation);
        membershipView.forEach(digest::addMember);
        return digest;
    }
    
    private void markNodeAsSuspected(NodeState node) {
        logger.warn("Node {} suspected (phi={}), last heartbeat {} ms ago", 
            node.getNodeId(), node.getPhiScore(), node.getMillisSinceLastHeartbeat());
        node.setStatus(NodeStatus.SUSPECTED);
        failuresDetected.increment();
    }
    
    private void markNodeAsFailed(NodeState node) {
        logger.error("Node {} marked as FAILED, last heartbeat {} ms ago", 
            node.getNodeId(), node.getMillisSinceLastHeartbeat());
        node.setStatus(NodeStatus.FAILED);
    }
    
    public Map<String, NodeState> getMembershipView() {
        return new HashMap<>(membershipView);
    }
    
    public int getHealthyNodeCount() {
        return (int) membershipView.values().stream()
            .filter(n -> n.getStatus() == NodeStatus.HEALTHY)
            .count();
    }
    
    public boolean hasQuorum() {
        int totalNodes = membershipView.size();
        int healthyNodes = getHealthyNodeCount();
        return healthyNodes >= (totalNodes / 2) + 1;
    }
    
    private String getLocalIpAddress() {
        return System.getenv("NODE_IP") != null ? 
            System.getenv("NODE_IP") : "localhost";
    }
}
