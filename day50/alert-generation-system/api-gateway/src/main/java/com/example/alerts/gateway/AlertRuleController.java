package com.example.alerts.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alert-rules")
@Slf4j
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleRepository alertRuleRepository;

    @GetMapping
    public List<AlertRule> getAllRules() {
        return alertRuleRepository.findAll();
    }

    @GetMapping("/{id}")
    public AlertRule getRule(@PathVariable String id) {
        return alertRuleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rule not found"));
    }

    @PostMapping
    public AlertRule createRule(@RequestBody AlertRule rule) {
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        if (rule.getEnabled() == null) {
            rule.setEnabled(true);
        }
        
        AlertRule saved = alertRuleRepository.save(rule);
        log.info("Created alert rule: {}", saved.getId());
        return saved;
    }

    @PutMapping("/{id}")
    public AlertRule updateRule(@PathVariable String id, @RequestBody AlertRule rule) {
        AlertRule existing = alertRuleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rule not found"));
        
        existing.setName(rule.getName());
        existing.setDescription(rule.getDescription());
        existing.setType(rule.getType());
        existing.setCondition(rule.getCondition());
        existing.setThreshold(rule.getThreshold());
        existing.setWindowMinutes(rule.getWindowMinutes());
        existing.setSeverity(rule.getSeverity());
        existing.setEnabled(rule.getEnabled());
        existing.setUpdatedAt(LocalDateTime.now());
        
        return alertRuleRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void deleteRule(@PathVariable String id) {
        alertRuleRepository.deleteById(id);
        log.info("Deleted alert rule: {}", id);
    }

    @PostMapping("/{id}/enable")
    public AlertRule enableRule(@PathVariable String id) {
        AlertRule rule = alertRuleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rule not found"));
        rule.setEnabled(true);
        rule.setUpdatedAt(LocalDateTime.now());
        return alertRuleRepository.save(rule);
    }

    @PostMapping("/{id}/disable")
    public AlertRule disableRule(@PathVariable String id) {
        AlertRule rule = alertRuleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Rule not found"));
        rule.setEnabled(false);
        rule.setUpdatedAt(LocalDateTime.now());
        return alertRuleRepository.save(rule);
    }
}
