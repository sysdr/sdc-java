# Quorum Log Storage Dashboard

A comprehensive web dashboard for monitoring and interacting with the Quorum-Based Log Storage System.

## Features

### System Overview
- Visual architecture diagram showing all components
- Consistency level explanations (ONE, QUORUM, ALL)
- Real-time service status monitoring

### Operations
- **Write Operations**: Write data with configurable consistency levels
  - ONE: Fast writes (~5ms)
  - QUORUM: Balanced writes (~15ms)
  - ALL: Strong consistency (~50ms)
- **Read Operations**: Read data with consistency control
- **Operation History**: Track all operations with latency metrics

### Prometheus Integration
- **Query Explorer**: Execute PromQL queries directly
- **Predefined Queries**: Common metrics ready to use
- **Query Range**: Visualize metrics over time with graphs
- **Query Examples**: Learn from example queries

### Grafana Integration
- Direct link to Grafana dashboard
- Documentation of all Grafana panels
- Alert rules reference

## Running the Dashboard

```bash
cd dashboard
npm install
npm start
```

The dashboard will be available at: **http://localhost:8888**

## Configuration

The dashboard connects to:
- API Gateway: http://localhost:8080
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

You can override these by setting environment variables:
```bash
API_GATEWAY_URL=http://localhost:8080 \
PROMETHEUS_URL=http://localhost:9090 \
GRAFANA_URL=http://localhost:3000 \
npm start
```

## Usage

1. **System Overview**: View system architecture and service status
2. **Operations**: Test write and read operations with different consistency levels
3. **Prometheus**: Query metrics and visualize data
4. **Grafana**: Access advanced Grafana dashboards

## Design

The dashboard uses a professional color scheme:
- Primary: Green (#2d8659)
- Secondary: Orange (#d97706)
- Accent: Teal (#059669)
- Background: Light green/yellow gradient (no blue or purple)

