package com.example.routing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class RoutingRuleManager {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CopyOnWriteArrayList<RoutingRule> rules = new CopyOnWriteArrayList<>();
    
    private static final String RULES_KEY = "routing:rules";
    
    public RoutingRuleManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    @PostConstruct
    public void loadRules() {
        try {
            // Initialize default rules if Redis is empty
            if (Boolean.FALSE.equals(redisTemplate.hasKey(RULES_KEY))) {
                initializeDefaultRules();
            }
            
            // Load rules from Redis
            Set<String> ruleJsons = redisTemplate.opsForSet().members(RULES_KEY);
            if (ruleJsons != null) {
                rules.clear();
                for (String json : ruleJsons) {
                    RoutingRule rule = objectMapper.readValue(json, RoutingRule.class);
                    rules.add(rule);
                }
                rules.sort(Comparator.comparingInt(RoutingRule::getPriority));
                log.info("Loaded {} routing rules", rules.size());
            }
        } catch (Exception e) {
            log.error("Failed to load routing rules", e);
        }
    }
    
    private void initializeDefaultRules() {
        try {
            // Security rule
            RoutingRule securityRule = new RoutingRule();
            securityRule.setName("security-critical");
            securityRule.setPriority(1);
            securityRule.setSeverities(List.of("ERROR", "FATAL"));
            securityRule.setSources(List.of("auth-service", "payment-api"));
            securityRule.setDestinations(List.of("logs-security", "logs-critical"));
            
            // Performance rule
            RoutingRule performanceRule = new RoutingRule();
            performanceRule.setName("performance-metrics");
            performanceRule.setPriority(2);
            performanceRule.setTypes(List.of("metric"));
            performanceRule.setDestinations(List.of("logs-performance"));
            
            // Application error rule
            RoutingRule appErrorRule = new RoutingRule();
            appErrorRule.setName("application-errors");
            appErrorRule.setPriority(3);
            appErrorRule.setSeverities(List.of("ERROR", "FATAL"));
            appErrorRule.setTypes(List.of("application"));
            appErrorRule.setDestinations(List.of("logs-application", "logs-critical"));
            
            // System logs rule
            RoutingRule systemRule = new RoutingRule();
            systemRule.setName("system-logs");
            systemRule.setPriority(4);
            systemRule.setTypes(List.of("system", "audit"));
            systemRule.setDestinations(List.of("logs-system"));
            
            // Default rule
            RoutingRule defaultRule = new RoutingRule();
            defaultRule.setName("default-routing");
            defaultRule.setPriority(999);
            defaultRule.setDestinations(List.of("logs-default"));
            
            // Save to Redis
            saveRule(securityRule);
            saveRule(performanceRule);
            saveRule(appErrorRule);
            saveRule(systemRule);
            saveRule(defaultRule);
            
            log.info("Initialized default routing rules");
        } catch (Exception e) {
            log.error("Failed to initialize default rules", e);
        }
    }
    
    private void saveRule(RoutingRule rule) {
        try {
            String json = objectMapper.writeValueAsString(rule);
            redisTemplate.opsForSet().add(RULES_KEY, json);
        } catch (Exception e) {
            log.error("Failed to save rule", e);
        }
    }
    
    public List<String> evaluateRouting(LogEvent event) {
        List<String> destinations = new ArrayList<>();
        
        for (RoutingRule rule : rules) {
            if (rule.matches(event)) {
                destinations.addAll(rule.getDestinations());
                log.debug("Event {} matched rule: {}", event.getId(), rule.getName());
            }
        }
        
        // If no rules matched, use default
        if (destinations.isEmpty()) {
            destinations.add("logs-default");
        }
        
        // Remove duplicates
        return destinations.stream().distinct().toList();
    }
    
    public List<RoutingRule> getRules() {
        return new ArrayList<>(rules);
    }
}
