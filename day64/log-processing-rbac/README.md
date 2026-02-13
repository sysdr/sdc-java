# Day 64: Role-Based Access Control (RBAC) for Distributed Log Processing

## Architecture Overview

This system implements production-grade RBAC with:
- **JWT-based authentication** with refresh token rotation
- **Fine-grained authorization** with multi-tenant isolation
- **Audit logging** for compliance
- **Circuit breaker patterns** for resilience
- **Permission caching** for performance

### System Components

1. **Auth Service** (Port 8081)
   - User authentication with BCrypt password hashing
   - JWT token generation (15-min access, 7-day refresh)
   - Token refresh with rotation
   - Redis-backed token blacklisting

2. **API Gateway** (Port 8080)
   - JWT validation and user context extraction
   - Authorization enforcement
   - Audit event publishing to Kafka
   - Circuit breaker for downstream services

3. **Log Query Service** (Port 8083)
   - Mock log query implementation
   - Team-based data isolation

4. **Audit Service** (Port 8084)
   - Kafka consumer for audit events
   - Append-only PostgreSQL storage
   - Compliance reporting

### Technology Stack

- **Java 17** + **Spring Boot 3.2**
- **PostgreSQL** for user and audit data
- **Redis** for permission caching
- **Kafka** for audit event streaming
- **Prometheus** + **Grafana** for monitoring
- **Resilience4j** for circuit breakers

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Maven 3.9+
- Java 17+

### Build and Run

```bash
# Generate all project files (if using setup.sh script)
chmod +x setup.sh
./setup.sh

# OR manually:
docker-compose up -d
```

System will be ready in ~2 minutes.

### Test the System

```bash
# Run integration tests
./integration-tests/test_rbac.sh

# Run load test (requires apache-bench)
./load-test.sh
```

## API Usage

### 1. Login

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "uuid-uuid",
  "username": "admin",
  "roles": ["ADMIN", "SRE"],
  "teams": ["platform", "security"]
}
```

### 2. Query Logs

```bash
curl -X POST http://localhost:8080/api/logs/query \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "ERROR",
    "team": "payments",
    "maxResults": 100
  }'
```

### 3. Refresh Token

```bash
curl -X POST http://localhost:8081/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "uuid-uuid"
  }'
```

### 4. Logout

```bash
curl -X POST http://localhost:8081/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "uuid-uuid"
  }'
```

## Authorization Model

### Roles

- **ADMIN**: Full system access, can manage users
- **SRE**: Read all logs across teams, modify dashboards
- **DEVELOPER**: Read logs for assigned teams only
- **ANALYST**: Read anonymized logs

### Permission Checks

1. JWT validation (signature + expiry)
2. Role-based access (does role permit action?)
3. Resource-level access (is user in team or has explicit grant?)
4. Cached permissions (5-minute TTL in Redis)

### Multi-Tenant Isolation

Logs are isolated by team. Query rewriting ensures users only access authorized teams:

```
User query: "ERROR in last 24h"
Rewritten:  "ERROR in last 24h AND _index IN (logs-payments-*, logs-fraud-*)"
```

## Monitoring

### Metrics (Prometheus)

Access: http://localhost:9090

Key metrics:
- `http_server_requests_seconds_count{uri="/auth/login"}` - Login rate
- `http_server_requests_seconds_count{status="403"}` - Authorization failures
- `resilience4j_circuitbreaker_state` - Circuit breaker status

### Dashboards (Grafana)

Access: http://localhost:3000 (admin/admin)

- Authentication request rate
- Authorization failures
- Circuit breaker state
- Audit log throughput

## Performance Characteristics

### Latency Breakdown

- JWT validation: 0.5ms (CPU)
- Permission cache hit: 1ms (Redis)
- Permission cache miss: 15ms (Redis + PostgreSQL)
- Audit logging: 3ms async

**Total auth overhead**: 2-5ms per request

### Capacity

At 10,000 RPS:
- CPU: 5 cores for JWT validation
- Database: 500 queries/sec on cache misses (5% miss rate)
- Audit: 5MB/sec → 432GB/day

### Scalability

- **Horizontal**: All services are stateless, scale behind load balancer
- **Database**: Read replicas for auth queries, partition audit logs by month
- **Cache**: Redis cluster for high availability

## Testing

### Integration Tests

```bash
./integration-tests/test_rbac.sh
```

Tests:
- ✅ Admin login and authorization
- ✅ Developer team access
- ✅ Cross-team access denial
- ✅ Invalid token rejection

### Load Test

```bash
./load-test.sh
```

Sends 1000 requests with 10 concurrent connections. Monitor in Grafana.

## Production Considerations

### Security

- **Token security**: Short-lived JWTs (15min), refresh rotation
- **Password storage**: BCrypt with salt
- **Audit immutability**: Append-only PostgreSQL table

### Failure Modes

1. **Auth service down**: Circuit opens → cached permissions (5min grace)
2. **Redis down**: Direct database queries (50ms latency)
3. **Kafka down**: Audit events buffer in memory (1MB)

### Compliance

- **GDPR**: User deletion triggers token revocation
- **SOC2**: All data access logged with user attribution
- **HIPAA**: Audit logs retained 7 years (configure in code)

## Troubleshooting

### Service won't start

```bash
docker-compose logs <service-name>
docker-compose restart <service-name>
```

### Database connection errors

```bash
# Recreate database
docker-compose down -v
docker-compose up -d
```

### Permission denied errors

Check user's roles and teams:
```bash
# Query PostgreSQL
docker-compose exec postgres psql -U postgres -d authdb -c "SELECT * FROM users WHERE username='developer';"
```

## Next Steps

Tomorrow (Day 65): Field-level encryption for sensitive log data
- Encrypt PII fields at rest
- Transparent decryption for authorized users
- Key rotation strategies

## License

Educational use - System Design Course Day 64
