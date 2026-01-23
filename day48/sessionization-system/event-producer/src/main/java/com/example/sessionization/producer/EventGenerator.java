package com.example.sessionization.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventGenerator {
    private static final Logger log = LoggerFactory.getLogger(EventGenerator.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    
    // Track active user sessions
    private final Map<String, SessionContext> activeSessions = new ConcurrentHashMap<>();
    
    private static final String[] EVENT_TYPES = {
        "PAGE_VIEW", "CLICK", "ADD_TO_CART", "SEARCH", "PURCHASE"
    };
    
    private static final String[] PAGES = {
        "/home", "/products", "/cart", "/checkout", "/profile", "/search"
    };
    
    private static final String[] DEVICES = {"mobile", "desktop", "tablet"};
    private static final String[] LOCATIONS = {"US", "UK", "CA", "AU", "DE"};

    public EventGenerator(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 100) // Generate events every 100ms (10 events/sec per user)
    public void generateEvents() {
        try {
            // Simulate 50 concurrent users
            int activeUsers = 50;
            
            for (int i = 0; i < activeUsers; i++) {
                String userId = "user-" + random.nextInt(100);
                
                // 80% chance to continue existing session, 20% chance to start new
                SessionContext context = activeSessions.computeIfAbsent(userId, 
                    k -> new SessionContext(UUID.randomUUID().toString()));
                
                // Randomly end sessions (simulate inactivity)
                if (random.nextDouble() < 0.02) { // 2% chance to end session
                    activeSessions.remove(userId);
                    context = new SessionContext(UUID.randomUUID().toString());
                    activeSessions.put(userId, context);
                }
                
                UserEvent event = generateEvent(userId, context);
                String json = objectMapper.writeValueAsString(event);
                
                kafkaTemplate.send("user-events", userId, json);
                context.incrementEventCount();
                
                if (context.getEventCount() % 10 == 0) {
                    log.info("Generated event for {}: session={}, type={}, total={}", 
                        userId, context.getSessionId(), event.getEventType(), context.getEventCount());
                }
            }
        } catch (Exception e) {
            log.error("Error generating events", e);
        }
    }

    private UserEvent generateEvent(String userId, SessionContext context) {
        // Simulate realistic user behavior patterns
        String eventType = selectEventType(context);
        String page = selectPage(eventType, context);
        
        context.setLastPage(page);
        
        return new UserEvent(
            UUID.randomUUID().toString(),
            userId,
            context.getSessionId(),
            eventType,
            page,
            System.currentTimeMillis(),
            DEVICES[random.nextInt(DEVICES.length)],
            LOCATIONS[random.nextInt(LOCATIONS.length)],
            generateMetadata(eventType)
        );
    }

    private String selectEventType(SessionContext context) {
        // Simulate conversion funnel behavior
        if (context.getEventCount() < 3) {
            return "PAGE_VIEW";
        } else if (context.getEventCount() < 8) {
            return random.nextDouble() < 0.3 ? "SEARCH" : "PAGE_VIEW";
        } else if (context.getEventCount() < 12) {
            double rand = random.nextDouble();
            if (rand < 0.4) return "PAGE_VIEW";
            else if (rand < 0.7) return "CLICK";
            else return "ADD_TO_CART";
        } else {
            double rand = random.nextDouble();
            if (rand < 0.2) return "PURCHASE";
            else if (rand < 0.4) return "ADD_TO_CART";
            else if (rand < 0.6) return "CLICK";
            else return "PAGE_VIEW";
        }
    }

    private String selectPage(String eventType, SessionContext context) {
        return switch (eventType) {
            case "SEARCH" -> "/search";
            case "ADD_TO_CART", "PURCHASE" -> "/cart";
            default -> context.getLastPage() != null && random.nextDouble() < 0.6 
                ? context.getLastPage() 
                : PAGES[random.nextInt(PAGES.length)];
        };
    }

    private String generateMetadata(String eventType) {
        return switch (eventType) {
            case "SEARCH" -> "{\"query\":\"product\",\"results\":150}";
            case "ADD_TO_CART" -> "{\"productId\":\"P" + random.nextInt(1000) + "\",\"price\":" + (10 + random.nextInt(990)) + "}";
            case "PURCHASE" -> "{\"orderId\":\"O" + random.nextInt(10000) + "\",\"amount\":" + (50 + random.nextInt(450)) + "}";
            default -> "{\"duration\":" + random.nextInt(60) + "}";
        };
    }

    private static class SessionContext {
        private final String sessionId;
        private int eventCount = 0;
        private String lastPage;

        public SessionContext(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getSessionId() {
            return sessionId;
        }

        public int getEventCount() {
            return eventCount;
        }

        public void incrementEventCount() {
            this.eventCount++;
        }

        public String getLastPage() {
            return lastPage;
        }

        public void setLastPage(String lastPage) {
            this.lastPage = lastPage;
        }
    }
}
