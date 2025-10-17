# Simple Monitoring Dashboard

A lightweight, web-based monitoring dashboard for the Log Processing System that replaces Grafana and Prometheus with a simple, self-contained solution.

## Features

- **Real-time Service Health Monitoring** - Shows status of all Spring Boot services and infrastructure
- **System Metrics** - Displays key performance indicators like request counts, error rates, and memory usage
- **Interactive Charts** - Real-time charts for throughput, response times, memory usage, and error rates
- **Responsive Design** - Works on desktop and mobile devices
- **No External Dependencies** - Self-contained solution using only Node.js and Express

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Spring Boot   │    │   Spring Boot   │    │   Spring Boot   │
│  API Gateway    │    │  Log Producer   │    │  Log Consumer   │
│    :8080        │    │    :8081        │    │    :8082        │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────┴─────────────┐
                    │                           │
                    │    Metrics Server         │
                    │    :3001                  │
                    │                           │
                    │  ┌─────────────────────┐  │
                    │  │   Web Dashboard     │  │
                    │  │   (HTML/JS/Charts)  │  │
                    │  └─────────────────────┘  │
                    └───────────────────────────┘
```

## Quick Start

### Using Docker (Recommended)

1. Start the entire system:
   ```bash
   ./start-services.sh
   ```

2. Access the dashboard:
   ```
   http://localhost:3001
   ```

### Manual Setup

1. Install dependencies:
   ```bash
   cd simple-dashboard
   npm install
   ```

2. Start the metrics server:
   ```bash
   npm start
   ```

3. Access the dashboard:
   ```
   http://localhost:3001
   ```

## API Endpoints

The dashboard provides several REST API endpoints for programmatic access:

- `GET /api/metrics` - All collected metrics from all services
- `GET /api/health` - Service health status
- `GET /api/aggregated` - Aggregated system metrics
- `GET /api/timeseries` - Time series data for charts

### Example API Response

```json
{
  "services": {
    "api-gateway": {
      "name": "API Gateway",
      "health": {
        "status": "up",
        "details": {
          "status": "UP",
          "components": {
            "circuitBreakers": {"status": "UP"},
            "diskSpace": {"status": "UP"}
          }
        }
      },
      "metrics": {
        "gateway_requests_total": 1250,
        "jvm_memory_used_bytes{area=\"heap\"}": 52428800
      }
    }
  },
  "lastUpdate": "2024-01-15T10:30:00.000Z"
}
```

## Dashboard Components

### 1. Service Health Status
- Real-time status of all services (API Gateway, Log Producer, Log Consumer)
- Infrastructure health (Kafka, PostgreSQL, Redis)
- Color-coded status indicators (Green=Up, Red=Down, Orange=Unknown)

### 2. System Metrics
- Total request count
- Error rate percentage
- Average memory usage
- Real-time updates every 5 seconds

### 3. Interactive Charts
- **Request Throughput**: Shows requests per second for each service
- **Response Times**: Displays p95 response times
- **Memory Usage**: JVM heap usage for each service
- **Error Rates**: Error percentages over time

## Configuration

The dashboard automatically discovers services using the following configuration:

```javascript
const SERVICES = {
    'api-gateway': { port: 8080, name: 'API Gateway' },
    'log-producer': { port: 8081, name: 'Log Producer' },
    'log-consumer': { port: 8082, name: 'Log Consumer' }
};

const INFRASTRUCTURE = {
    'kafka': { port: 9092, name: 'Kafka' },
    'postgres': { port: 5432, name: 'PostgreSQL' },
    'redis': { port: 6379, name: 'Redis' }
};
```

## Data Collection

The dashboard collects metrics by:

1. **Health Checks**: Polling `/actuator/health` endpoints every 5 seconds
2. **Metrics Collection**: Fetching Prometheus metrics from `/actuator/prometheus` endpoints
3. **Infrastructure Monitoring**: Basic port connectivity checks for infrastructure services
4. **Data Caching**: Storing recent metrics in memory for fast access

## Advantages over Grafana/Prometheus

- **Simplicity**: Single Node.js application, no complex configuration
- **Lightweight**: Minimal resource usage compared to Prometheus + Grafana
- **Self-contained**: No external dependencies or database setup
- **Fast Setup**: Ready to use immediately after starting
- **Customizable**: Easy to modify and extend with additional metrics
- **Mobile-friendly**: Responsive design works on all devices

## Troubleshooting

### Services Not Showing as Up
- Ensure Spring Boot services are running and accessible
- Check that actuator endpoints are enabled in application.yml
- Verify port numbers match the configuration

### Charts Not Updating
- Check browser console for JavaScript errors
- Ensure the metrics server is running on port 3001
- Verify CORS settings if accessing from different domains

### High Memory Usage
- The dashboard caches metrics in memory
- Restart the metrics server to clear cache
- Consider reducing the update frequency if needed

## Development

To modify the dashboard:

1. Edit `index.html` for UI changes
2. Modify `metrics-server.js` for backend logic
3. Update `package.json` for dependencies
4. Rebuild Docker image: `docker-compose build dashboard`

## License

MIT License - Feel free to use and modify as needed.
