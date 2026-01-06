package com.example.kafka.admin;

import org.apache.kafka.clients.admin.TopicDescription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    @Autowired
    private TopicAdminService topicAdminService;

    @GetMapping
    public ResponseEntity<Set<String>> listTopics() {
        try {
            Set<String> topics = topicAdminService.listTopics();
            return ResponseEntity.ok(topics);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{topicName}")
    public ResponseEntity<TopicDescription> describeTopic(@PathVariable String topicName) {
        try {
            TopicDescription description = topicAdminService.describeTopic(topicName);
            return ResponseEntity.ok(description);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<String> createTopic(
            @RequestParam String topicName,
            @RequestParam(defaultValue = "12") int partitions,
            @RequestParam(defaultValue = "3") short replicationFactor,
            @RequestBody(required = false) Map<String, String> configs) {
        try {
            topicAdminService.createTopic(topicName, partitions, replicationFactor, configs);
            return ResponseEntity.ok("Topic created: " + topicName);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating topic: " + e.getMessage());
        }
    }

    @DeleteMapping("/{topicName}")
    public ResponseEntity<String> deleteTopic(@PathVariable String topicName) {
        try {
            topicAdminService.deleteTopic(topicName);
            return ResponseEntity.ok("Topic deleted: " + topicName);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting topic: " + e.getMessage());
        }
    }
}
