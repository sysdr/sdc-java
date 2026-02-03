# Day 59: Active-Passive Failover System

A production-ready implementation of active-passive failover for distributed log processing with automatic leader election, state migration, and sub-second recovery times.

## System Architecture

```
┌─────────────────┐         ┌──────────────────┐
│   API Gateway   │────────▶│      Kafka       │
│   (Port 8080)   │         │  (log-events)    │
└─────────────────┘         └──────────────────┘
                                     │
                    ┌────────────────┴────────────────┐
                    │                                  │
                    ▼                                  ▼
         ┌──────────────────┐            ┌──────────────────┐
         │  Consumer 1      │            │  Consumer 2      │
         │  LEADER ✓        │            │  STANDBY         │
         │  (Port 8081)     │            │  (Port 8082)     │
         └──────────────────┘            └──────────────────┘
                    │                                  │
                    └────────────────┬─────────────────┘
                                     │
                    ┌────────────────┴────────────────┐
                    │                                  │
                    ▼                                  ▼
         ┌──────────────────┐            ┌──────────────────┐
         │    ZooKeeper     │            │      Redis       │
         │ Leader Election  │            │   State Store    │
         └──────────────────┘            └──────────────────┘
                                                  │
                                                  ▼
                                       ┌──────────────────┐
                                       │   PostgreSQL     │
                                       │  (Log Storage)   │
                                       └──────────────────┘
```

## Key Features

- **ZooKeeper-based Leader Election**: Distributed consensus using Apache Curator
- **Automatic Failover**: Sub-6-second recovery with zero message loss
- **State Migration**: Seamless handoff of partition offsets and in-flight messages
- **Heartbeat Monitoring**: Multi-signal health checks with fencing tokens
- **Idempotent Processing**: At-least-once delivery with deduplication
- **Circuit Breaker**: Resilience4j integration for fault tolerance
- **Observability**: Prometheus metrics + Grafana dashboards

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- 8GB RAM minimum
- Ports: 8080-8082, 9090, 3000, 2181, 9092, 6379, 5432

## Quick Start

### 1. Generate and Setup Infrastructure

```bash
# Start all infrastructure services
./setup.sh
```

This starts: Kafka, ZooKeeper, Redis, PostgreSQL, Prometheus, Grafana

### 2. Build Applications

```bash
mvn clean package
```

### 3. Start Services

**Terminal 1 - API Gateway:**
```bash
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

**Terminal 2 - Consumer Instance 1:**
```bash
java -jar log-consumer/target/log-consumer-1.0.0.jar
```

**Terminal 3 - Consumer Instance 2:**
```bash
SERVER_PORT=8082 java -jar log-consumer/target/log-consumer-1.0.0.jar
```

### 4. Verify System

```bash
# Check system status
curl http://localhost:8080/api/system/status

# Check which consumer is leader
curl http://localhost:8081/api/failover/status
curl http://localhost:8082/api/failover/status
```

## Testing Failover

### 1. Run Integration Tests

```bash
./integration-tests/test-failover.sh
```

This publishes 10 test messages and verifies processing.

### 2. Manual Failover Test

```bash
# 1. Identify current leader
LEADER_PORT=$(curl -s http://localhost:8081/api/failover/status | jq '.isLeader' | grep -q true && echo 8081 || echo 8082)
echo "Current leader on port: $LEADER_PORT"

# 2. Publish test messages
for i in {1..100}; do
    curl -s -X POST http://localhost:8080/api/logs \
        -H "Content-Type: application/json" \
        -d "{\"level\":\"INFO\",\"message\":\"Pre-failover message $i\",\"source\":\"test\"}"
    sleep 0.1
done

# 3. Kill the leader process (in its terminal: Ctrl+C)

# 4. Watch the other consumer logs - it should:
#    - Detect heartbeat timeout (within 5 seconds)
#    - Acquire leadership via ZooKeeper
#    - Resume processing from last committed offset
#    - Process remaining messages

# 5. Verify no message loss
# Check PostgreSQL for message continuity
```

### 3. Load Testing

```bash
./load-test.sh
```

Generates 3,000 messages over 60 seconds at 50 RPS.

**During load test:**
- Kill the leader consumer
- Watch Grafana dashboard for recovery metrics
- Verify RTO (Recovery Time Objective) < 10 seconds
- Confirm RPO (Recovery Point Objective) = 0 messages lost

## Monitoring

### Prometheus Metrics

Access Prometheus at http://localhost:9090

**Key Metrics:**
```
# Failover events
failover_events_total

# Recovery time (histogram)
failover_recovery_time_seconds

# Messages processed
messages_processed_total

# Leader status (1=leader, 0=standby)
leader_status

# Current epoch
leader_epoch

# Heartbeat age
leader_heartbeat_age_seconds
```

### Grafana Dashboards

Access Grafana at http://localhost:3000 (admin/admin)

**Recommended Panels:**
1. Leadership Timeline (shows failover events)
2. Message Processing Rate (before/after failover)
3. Consumer Lag (should spike during failover, then recover)
4. Heartbeat Health
5. Recovery Time Distribution (P50/P95/P99)

**Sample Queries:**
```promql
# Current leader
leader_status

# Failover frequency
rate(failover_events_total[5m])

# Messages per second
rate(messages_processed_total[1m])

# Recovery time P95
histogram_quantile(0.95, failover_recovery_time_seconds)
```

## Architecture Deep Dive

### Leader Election Flow

1. Consumer starts → Connects to ZooKeeper
2. Creates ephemeral sequential node `/failover/leader/{instance-id}`
3. Lowest sequence number → becomes leader
4. Leader starts Kafka consumption, publishes heartbeat
5. Standby instances monitor heartbeat via Redis
6. Leader crashes → ephemeral node deleted
7. ZooKeeper notifies remaining instances
8. New leader elected, assumes processing

### State Migration

**Stored in Redis:**
- Current partition assignments
- Last committed offset per partition
- In-flight message IDs (5-minute TTL)
- Total messages processed
- Leadership epoch

**On Failover:**
1. New leader loads state from Redis
2. Validates consistency (state age < 30s)
3. Resumes from last committed offset
4. Deduplicates in-flight messages using Redis set
5. Increments epoch, updates fencing token

### Heartbeat Protocol

**Leader:** Updates `leader:heartbeat` in Redis every 1 second
```
Format: {timestamp}:{epoch}:{instanceId}
Example: 1706876543210:5:abc-123-def
```

**Standby:** Checks heartbeat every 2 seconds
- Age > 5 seconds → initiates failover
- Uses epoch for fencing (rejects stale messages)

### Anti-Patterns Avoided

❌ **Database row locks** for leader election (connection pool masks failures)  
✅ **ZooKeeper ephemeral nodes** (session timeout = reliable failure detection)

❌ **Synchronous state replication** (adds latency to message processing)  
✅ **Async state snapshots** with eventual consistency

❌ **Immediate failover** on first missed heartbeat (false positive during GC)  
✅ **3 consecutive misses** + grace period

## Failure Scenarios Tested

### 1. Leader Process Crash
- **Detection time:** 5 seconds (3 × heartbeat interval)
- **Election time:** 1 second (ZooKeeper latency)
- **Resume time:** <6 seconds total
- **Messages lost:** 0

### 2. Network Partition
- **Split-brain prevention:** Fencing tokens (epoch)
- **Partitioned leader:** Releases lock when can't update Redis
- **Recovery:** Rejoins as standby after partition heals

### 3. ZooKeeper Unavailability
- **Impact:** No new elections, last leader continues
- **Alert:** Fires immediately
- **Manual intervention:** Required if leader then fails

### 4. Cascading Failure
- **Scenario:** New leader immediately crashes
- **Behavior:** Third instance takes over
- **Circuit breaker:** Prevents downstream overload

### 5. Message Broker Failure
- **Kafka down:** Consumers circuit-open
- **Retry strategy:** Exponential backoff
- **Recovery:** Resume on broker restart

## Production Considerations

### Performance Tuning

**ZooKeeper Session Timeout:**
```yaml
# Current: 4 seconds (detection latency)
# Reduce to 2s for faster failover (more false positives)
# Increase to 10s for stable networks
```

**Heartbeat Configuration:**
```yaml
failover:
  heartbeat-interval: 1000  # ms
  heartbeat-timeout: 5000   # ms
  
# Tune based on GC pause times
# If frequent false failovers, increase timeout
```

**State Snapshot Size:**
- 50 partitions × 100 bytes = 5 KB
- In-flight tracking: ~100 KB (1000 msgs)
- Total: <200 KB per snapshot
- Redis memory: <1 MB

### Scalability

**Horizontal Scaling:**
- Add more standby instances (up to 10 recommended)
- Only one leader, others wait in hot standby
- Leadership rotates automatically on failure

**Partition Strategy:**
- More partitions = finer-grained parallelism
- But more state to migrate on failover
- Recommendation: Start with 3-5 partitions per consumer

### Security

**Production Checklist:**
- [ ] Enable ZooKeeper ACLs
- [ ] Kafka SASL/SSL authentication
- [ ] Redis password protection
- [ ] PostgreSQL SSL connections
- [ ] API gateway rate limiting
- [ ] Network policies (firewall rules)

## Troubleshooting

### Issue: Frequent Failovers (>3/hour)

**Causes:**
- Network instability
- GC pauses causing heartbeat delays
- Under-provisioned infrastructure

**Solutions:**
```bash
# Check GC logs
java -Xlog:gc* -jar log-consumer-1.0.0.jar

# Increase heartbeat timeout
# In application.yml:
failover:
  heartbeat-timeout: 10000  # 10 seconds
```

### Issue: Messages Processed Twice

**Cause:** Failover during message processing (expected!)

**Verification:**
```sql
SELECT message_id, COUNT(*) 
FROM log_events 
GROUP BY message_id 
HAVING COUNT(*) > 1;
```

**Solution:** Already handled via deduplication (check Redis sets)

### Issue: Split-Brain Detected

**Symptom:** Two instances both claim to be leader

**Check:**
```bash
curl http://localhost:8081/api/failover/status
curl http://localhost:8082/api/failover/status
# Both show isLeader: true
```

**Diagnosis:**
```bash
# Check epochs - higher epoch wins
# Messages from lower epoch should be rejected

# Verify ZooKeeper state
docker-compose exec zookeeper zkCli.sh
ls /failover/leader
```

**Fix:** Restart both consumers to re-elect cleanly

## Key Metrics to Watch in Production

| Metric | Threshold | Action |
|--------|-----------|--------|
| `failover_events_total` | >3/hour | Investigate network/GC |
| `failover_recovery_time_seconds` P95 | >10s | Tune config or scale |
| `messages_lost_total` | >0 | CRITICAL: Debug immediately |
| `leader_heartbeat_age_seconds` | >3s | Pre-emptive alert |
| Consumer lag | >1000 | Scale consumers or partitions |

## Extending the System

### Add More Consumer Instances

```bash
# Terminal 4
SERVER_PORT=8083 java -jar log-consumer/target/log-consumer-1.0.0.jar

# Only one will be leader, others standby
# Automatic failover to any healthy instance
```

### Multi-Region Setup

See Day 60 for geographic failover extension:
- Cross-datacenter replication
- Region-aware leader election
- CAP theorem trade-offs

### Custom State

Extend `ConsumerState` model:
```java
@Data
public class ConsumerState {
    // Existing fields...
    
    // Add custom application state
    private Map<String, String> customMetadata;
    private List<String> pendingOperations;
}
```

## Learning Outcomes

After completing this lesson, you should understand:

✅ How distributed consensus enables automatic failover  
✅ Trade-offs between consistency and availability  
✅ Importance of idempotent processing  
✅ Fencing tokens prevent split-brain scenarios  
✅ Observability is critical for production failover  
✅ Grace periods reduce cascading failures  
✅ State migration complexity vs benefits

## Interview Talking Points

**"How do you handle failover in distributed systems?"**
> "I implement active-passive failover using ZooKeeper for leader election. The key insight is separating failure detection (heartbeats) from consensus (ZooKeeper). This lets you tune detection sensitivity without compromising on consistency. I use fencing tokens to prevent split-brain, and migrate state externally to Redis for fast recovery."

**"What's your RTO and RPO?"**
> "Recovery Time Objective is sub-10 seconds, measured as P95. Recovery Point Objective is zero messages lost via at-least-once delivery with deduplication. I achieve this by atomically committing offsets to both Kafka and Redis, and using idempotency checks on replay."

**"How do you prevent cascading failures during failover?"**
> "Three mechanisms: First, a grace period prevents dual consumption during split-brain. Second, circuit breakers on the failover path stop overload propagation. Third, exponential backoff on downstream connections lets services recover gradually."

## References

- [ZooKeeper Recipes](https://zookeeper.apache.org/doc/r3.7.1/recipes.html)
- [Kafka Consumer Groups](https://kafka.apache.org/documentation/#consumerconfigs)
- [Netflix Zuul Failover](https://netflixtechblog.com/zuul-2-the-netflix-journey-to-asynchronous-non-blocking-systems-45947377fb5c)
- [Resilience4j Documentation](https://resilience4j.readme.io/)

## Next Steps

**Day 60:** Multi-region replication with cross-datacenter failover and consistency models (AP vs CP in CAP theorem)

---

Built with ❤️ for production-grade distributed systems
