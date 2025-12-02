package com.example.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/write")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class WriteController {
    
    private final RaftNode raftNode;
    
    @PostMapping
    public ResponseEntity<WriteResponse> write(@RequestBody WriteRequest request) {
        log.info("Write request: {} bytes from {}", request.data().length(), request.source());
        
        WriteResponse response = raftNode.handleWrite(request);
        
        if (response.success()) {
            return ResponseEntity.ok(response);
        } else if (response.leaderId() != null) {
            // Return redirect information
            return ResponseEntity.status(307).body(response);
        } else {
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @GetMapping("/leader")
    public ResponseEntity<LeaderInfo> getLeader() {
        return ResponseEntity.ok(new LeaderInfo(
            raftNode.isLeader(),
            raftNode.getCurrentLeader(),
            raftNode.getCurrentTerm()
        ));
    }
}

record LeaderInfo(boolean isLeader, String leaderId, long term) {}
