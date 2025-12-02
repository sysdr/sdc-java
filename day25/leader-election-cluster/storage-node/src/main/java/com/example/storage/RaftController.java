package com.example.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;

@RestController
@RequestMapping("/raft")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class RaftController {
    
    private final RaftNode raftNode;
    private volatile Instant lastHeartbeat = Instant.now();
    private volatile String votedFor = null;
    private volatile long currentTerm = 0;
    
    @PostMapping("/vote")
    public synchronized ResponseEntity<VoteResponse> handleVoteRequest(@RequestBody VoteRequest request) {
        log.debug("Vote request from {} for term {}", request.candidateId(), request.term());
        
        // Reject if term is old
        if (request.term() < currentTerm) {
            return ResponseEntity.ok(new VoteResponse(currentTerm, false));
        }
        
        // Update term if newer
        if (request.term() > currentTerm) {
            currentTerm = request.term();
            votedFor = null;
        }
        
        // Grant vote if haven't voted or voted for this candidate
        boolean grantVote = (votedFor == null || votedFor.equals(request.candidateId()));
        
        if (grantVote) {
            votedFor = request.candidateId();
            lastHeartbeat = Instant.now();
            log.info("Granted vote to {} for term {}", request.candidateId(), request.term());
            return ResponseEntity.ok(new VoteResponse(currentTerm, true));
        }
        
        return ResponseEntity.ok(new VoteResponse(currentTerm, false));
    }
    
    @PostMapping("/heartbeat")
    public synchronized ResponseEntity<HeartbeatResponse> handleHeartbeat(@RequestBody HeartbeatRequest request) {
        log.trace("Heartbeat from leader {} for term {}", request.leaderId(), request.term());
        
        if (request.term() < currentTerm) {
            return ResponseEntity.ok(new HeartbeatResponse(currentTerm, false));
        }
        
        if (request.term() >= currentTerm) {
            currentTerm = request.term();
            votedFor = null;
            lastHeartbeat = Instant.now();
        }
        
        return ResponseEntity.ok(new HeartbeatResponse(currentTerm, true));
    }
    
    @PostMapping("/append")
    public ResponseEntity<AppendResponse> handleAppendEntries(@RequestBody AppendRequest request) {
        log.debug("Append request from {} with {} entries", request.leaderId(), request.entries().size());
        
        if (request.term() < currentTerm) {
            return ResponseEntity.ok(new AppendResponse(currentTerm, false));
        }
        
        // In production, validate and append entries
        // For this demo, acknowledge success
        return ResponseEntity.ok(new AppendResponse(currentTerm, true));
    }
    
    @GetMapping("/status")
    public ResponseEntity<RaftStatus> getStatus() {
        return ResponseEntity.ok(new RaftStatus(
            raftNode.getState(),
            raftNode.getCurrentTerm(),
            raftNode.getCurrentLeader(),
            Instant.now().minusMillis(lastHeartbeat.toEpochMilli()).toEpochMilli()
        ));
    }
}

record RaftStatus(NodeState state, long term, String leader, long timeSinceLastHeartbeat) {}
