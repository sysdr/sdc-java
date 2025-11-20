# Schema Registry System

A production-grade schema registry for managing and validating log formats in distributed systems. This implementation provides centralized schema storage, versioning, compatibility checking, and runtime validation.

## Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌────────────┐
│  Producers  │────▶│  Schema Registry │◀────│ Consumers  │
└─────────────┘     └────────┬─────────┘     └────────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
              ┌─────▼─────┐    ┌──────▼─────┐
              │  Redis    │    │ PostgreSQL │
              │  (Cache)  │    │ (Storage)  │
              └───────────┘    └────────────┘
```

## Components

- **schema-registry**: Core service for schema management (port 8081)
- **schema-client**: Client library for applications
- **validation-gateway**: Runtime validation service (port 8082)

## Quick Start

### 1. Start Infrastructure

```bash
./setup.sh
```

This starts PostgreSQL, Redis, Prometheus, and Grafana.

### 2. Build the Project

```bash
mvn clean package -DskipTests
```

### 3. Start Services

Terminal 1:
```bash
cd schema-registry
mvn spring-boot:run
```

Terminal 2:
```bash
cd validation-gateway
mvn spring-boot:run
```

### 4. Test the System

```bash
# Run integration tests
./integration-tests/run-tests.sh

# Run load tests
./load-test.sh
```

## API Usage

### Register a Schema

```bash
curl -X POST http://localhost:8081/subjects/my-logs/versions \
  -H "Content-Type: application/json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"Log\",\"fields\":[{\"name\":\"msg\",\"type\":\"string\"}]}",
    "schemaType": "AVRO"
  }'
```

Response: `{"id": 1}`

### Get Schema by ID

```bash
curl http://localhost:8081/schemas/ids/1
```

### Get Latest Schema

```bash
curl http://localhost:8081/subjects/my-logs/versions/latest
```

### List All Subjects

```bash
curl http://localhost:8081/subjects
```

### Check Compatibility

```bash
curl -X POST http://localhost:8081/subjects/my-logs/compatibility \
  -H "Content-Type: application/json" \
  -d '{
    "schema": "...",
    "schemaType": "AVRO"
  }'
```

### Set Compatibility Mode

```bash
curl -X PUT http://localhost:8081/subjects/my-logs/config \
  -H "Content-Type: application/json" \
  -d '{"compatibility": "FULL"}'
```

## Compatibility Modes

- **BACKWARD**: New schema can read old data (default)
- **FORWARD**: Old schema can read new data
- **FULL**: Both backward and forward compatible
- **NONE**: No compatibility checking

## Monitoring

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Health**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8081/actuator/prometheus

## Key Metrics

- `schema.registrations` - Total schema registrations
- `schema.compatibility.failures` - Failed compatibility checks
- `schema.registration.time` - Registration latency
- `validation.success` / `validation.failure` - Validation outcomes
- `validation.time` - Validation latency

## Performance

Target performance characteristics:
- Cached schema lookups: < 1ms p99
- Database lookups: < 50ms p99
- Schema registration: < 200ms
- Cache hit ratio: > 95%

## Project Structure

```
schema-registry-system/
├── schema-registry/          # Core registry service
├── schema-client/            # Client library
├── validation-gateway/       # Validation service
├── infrastructure/           # Monitoring config
├── integration-tests/        # Test scripts
├── docker-compose.yml        # Infrastructure
└── README.md
```

## Extending the System

### Adding New Schema Types

1. Add type to `SchemaType` enum
2. Implement validation in `SchemaValidatorService`
3. Add compatibility rules in `CompatibilityCheckerService`

### Custom Compatibility Rules

Override `CompatibilityCheckerService` methods for custom logic.

## Troubleshooting

### Service won't start
- Check PostgreSQL: `docker logs schema-postgres`
- Check Redis: `docker logs schema-redis`
- Verify ports 5432, 6379, 8081, 8082 are available

### Low cache hit ratio
- Check Redis connection
- Increase cache TTL if appropriate
- Monitor memory usage

### Compatibility failures
- Review schema evolution rules
- Check compatibility mode settings
- Use compatibility test endpoint before registering

## License

MIT
