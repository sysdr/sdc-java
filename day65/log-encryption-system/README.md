# Day 65: Field-Level Encryption for Sensitive Log Data

Production-ready distributed log processing system with field-level encryption, key rotation, and role-based access control.

## Architecture Overview

```
┌─────────────┐    ┌──────────┐    ┌───────────────┐    ┌──────────┐
│Log Producer │───▶│  Kafka   │───▶│ Log Consumer  │───▶│PostgreSQL│
│  (8080)     │    │  (9092)  │    │ + Encryption  │    │  (5432)  │
└─────────────┘    └──────────┘    │    (8082)     │    └──────────┘
                                    └───────┬───────┘
                                            │
                                            ▼
                                    ┌───────────────┐
                                    │  Encryption   │◀───┐
                                    │   Service     │    │
                                    │   (8081)      │    │
                                    └───────┬───────┘    │
                                            │            │
                                            ▼            │
                                        ┌───────┐        │
                                        │ Redis │        │
                                        │ (Keys)│        │
                                        └───────┘        │
                                                         │
┌─────────────┐                                         │
│Query Service│─────────────────────────────────────────┘
│   (8083)    │    (Decrypt with RBAC)
└─────────────┘
```

## Key Features

### 1. **AES-256-GCM Encryption**
- Field-level encryption (not full payload)
- Unique IV per field for security
- Authenticated encryption with GCM mode

### 2. **Automatic Key Rotation**
- Keys rotate every 30 days
- 24-hour overlap during rotation
- Zero-downtime key updates
- Old keys remain valid for decryption

### 3. **Role-Based Access Control**
- ADMIN: Full access to all PII fields
- SUPPORT: Email, name, phone only
- ANALYST: Email, name only
- COMPLIANCE: All sensitive fields except phone

### 4. **Searchable Encryption**
- HMAC-SHA256 hashes for deterministic search
- Find logs by encrypted email without decryption
- Query indexed encrypted fields

### 5. **Audit Logging**
- Every decryption logged with user context
- Access denied events tracked
- Compliance-ready audit trail

## Quick Start

### Prerequisites
- Docker & Docker Compose
- 8GB RAM minimum
- Ports 8080-8083, 9090, 3000, 5432, 6379, 9092 available

### Deploy System

```bash
# Generate all files and start services
./setup.sh

# Wait for services to be ready (30-60 seconds)
# Watch logs
docker-compose logs -f
```

### Verify Deployment

```bash
# Check service health
curl http://localhost:8081/actuator/health  # Encryption Service
curl http://localhost:8080/actuator/health  # Log Producer
curl http://localhost:8082/actuator/health  # Log Consumer
curl http://localhost:8083/actuator/health  # Query Service
```

## Usage Examples

### 1. Ingest Log with PII

```bash
curl -X POST http://localhost:8080/api/logs/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "user_registration",
    "severity": "INFO",
    "publicFields": {
      "action": "register",
      "success": "true",
      "ip": "192.168.1.100"
    },
    "piiFields": {
      "user.email": "alice@example.com",
      "user.ssn": "987-65-4321",
      "user.phone": "+1-555-9876"
    },
    "metadata": {
      "source": "mobile-app",
      "version": "2.1.0"
    }
  }'
```

### 2. Query Logs (ADMIN Role)

```bash
# ADMIN sees all decrypted fields
curl http://localhost:8083/api/query/logs?eventType=user_registration \
  -H "X-User-Id: admin-001" \
  -H "X-User-Role: ADMIN" \
  -H "X-User-Email: admin@company.com"
```

Response:
```json
[{
  "eventId": "uuid-here",
  "eventType": "user_registration",
  "severity": "INFO",
  "publicFields": {
    "action": "register",
    "success": "true"
  },
  "piiFields": {
    "user.email": "alice@example.com",
    "user.ssn": "987-65-4321",
    "user.phone": "+1-555-9876"
  }
}]
```

### 3. Query Logs (ANALYST Role)

```bash
# ANALYST sees redacted SSN and phone
curl http://localhost:8083/api/query/logs?eventType=user_registration \
  -H "X-User-Id: analyst-042" \
  -H "X-User-Role: ANALYST" \
  -H "X-User-Email: analyst@company.com"
```

Response:
```json
[{
  "eventId": "uuid-here",
  "piiFields": {
    "user.email": "alice@example.com",
    "user.ssn": "[REDACTED]",
    "user.phone": "[REDACTED]"
  }
}]
```

## Testing

### Integration Tests

```bash
# Run full encryption flow test
./integration-tests/test-encryption-flow.sh
```

Tests:
1. ✅ Ingest log with PII fields
2. ✅ ADMIN can decrypt all fields
3. ✅ ANALYST sees redacted restricted fields
4. ✅ Audit log captures all decryption events

### Load Testing

```bash
# Send 10,000 events to test throughput
./load-test.sh
```

Expected results:
- **Throughput**: 3,000-5,000 events/sec
- **Encryption latency**: <5ms p99
- **End-to-end latency**: <50ms p99

## Monitoring

### Prometheus Metrics

Access: http://localhost:9090

Key metrics:
- `encryption_operations_total` - Total encryptions
- `decryption_operations_total` - Total decryptions
- `encryption_operation_duration_seconds` - Encryption latency
- `log_events_processed_total` - Consumer throughput

### Grafana Dashboards

Access: http://localhost:3000 (admin/admin)

Pre-configured dashboards:
1. **Encryption Performance** - Ops/sec, latency percentiles
2. **Key Rotation Status** - Current key version, rotation schedule
3. **Access Control Audit** - Decryption by role, access denied events
4. **System Health** - Consumer lag, error rates

## System Design Insights

### Why Field-Level Over Full Payload?

| Approach | Storage | Search | Debuggability | Overhead |
|----------|---------|--------|---------------|----------|
| Full Payload | +40% | Impossible | Blind | Low |
| Field-Level | +15% | Partial | Good | Medium |

**Decision**: Field-level encryption wins for operational visibility.

### Key Rotation Strategy

**Problem**: Rotating keys breaks decryption of old data.

**Solution**: Version-based keys with overlap period:
- New encryptions use key v47
- Old data uses keys v45, v46 (still valid)
- After 30 days, purge keys older than v45

### RBAC vs ABAC

**RBAC** (Role-Based): Simple, 4 roles predefined
**ABAC** (Attribute-Based): Complex, dynamic policies

**Choice**: RBAC for simplicity. ABAC for future context-aware policies (e.g., "SUPPORT can decrypt email only during business hours").

## Troubleshooting

### Consumer Lag

```bash
# Check Kafka consumer lag
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group log-encryption-consumer
```

**Fix**: Scale consumers horizontally (increase concurrency in config).

### Encryption Errors

```bash
# Check encryption service logs
docker logs encryption-service --tail 100 -f
```

Common issues:
- Redis unavailable → Keys not cached → Fallback to KMS (slower)
- Invalid IV length → Check GCM configuration

### Access Denied Spikes

```bash
# Query audit logs
docker logs query-service | grep "Access DENIED"
```

**Cause**: Role permissions changed, users attempting unauthorized access.

## Production Checklist

- [ ] Use external KMS (AWS KMS, HashiCorp Vault) instead of in-memory keys
- [ ] Enable TLS for all service-to-service communication
- [ ] Store audit logs in tamper-proof write-once storage
- [ ] Implement key backup and disaster recovery
- [ ] Set up alerts for `encryption_error_rate > 0.01`
- [ ] Configure log retention policies (encrypt-then-delete after N days)
- [ ] Add rate limiting to prevent decryption abuse
- [ ] Implement geographic key sharding for GDPR compliance

## Scale Considerations

### At 50,000 events/sec:
- **Kafka**: 3-node cluster, 10 partitions
- **Encryption Service**: 4 instances behind load balancer
- **Redis**: Cluster mode with replication
- **PostgreSQL**: Read replicas for query service

### At 500,000 events/sec:
- **Batching**: Increase to 500 events/batch
- **Async encryption**: Use message queue between consumer and storage
- **Key caching**: Increase Redis memory, reduce TTL from 5min to 10min
- **Sharding**: Partition logs by tenant/region

## License

MIT - Educational purposes only
