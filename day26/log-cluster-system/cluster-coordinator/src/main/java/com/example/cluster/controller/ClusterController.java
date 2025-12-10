package com.example.cluster.controller;

import com.example.cluster.model.MembershipDigest;
import com.example.cluster.model.NodeState;
import com.example.cluster.service.ClusterMembershipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/cluster")
public class ClusterController {
    
    private final ClusterMembershipService membershipService;
    
    public ClusterController(ClusterMembershipService membershipService) {
        this.membershipService = membershipService;
    }
    
    @PostMapping("/gossip")
    public ResponseEntity<Void> receiveGossip(@RequestBody MembershipDigest digest) {
        membershipService.receiveGossip(digest);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/anti-entropy")
    public ResponseEntity<MembershipDigest> antiEntropy(@RequestBody MembershipDigest digest) {
        membershipService.receiveGossip(digest);
        // Return our complete view for anti-entropy repair
        MembershipDigest response = new MembershipDigest();
        response.setMembers(membershipService.getMembershipView());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/membership")
    public ResponseEntity<Map<String, NodeState>> getMembership() {
        return ResponseEntity.ok(membershipService.getMembershipView());
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getClusterStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalNodes", membershipService.getMembershipView().size());
        status.put("healthyNodes", membershipService.getHealthyNodeCount());
        status.put("hasQuorum", membershipService.hasQuorum());
        status.put("membership", membershipService.getMembershipView());
        return ResponseEntity.ok(status);
    }
}
