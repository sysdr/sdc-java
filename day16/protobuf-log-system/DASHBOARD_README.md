# Real-Time Dashboard

A modern, real-time dashboard for monitoring the Protobuf Log System.

## Features

- **Real-Time Metrics**: Auto-refreshes every 3 seconds
- **Service Status**: Monitor health of all three services (Producer, Consumer, Gateway)
- **Key Metrics**:
  - Total log events (JSON + Protobuf)
  - Format comparison (JSON vs Protobuf)
  - Kafka publish success rate
  - HTTP request counts
  - JVM memory usage
- **Visual Charts**:
  - Throughput trends over time
  - Format distribution (pie chart)

## Color Scheme

The dashboard uses a modern blue gradient theme with:
- Primary: Blue tones (#1e3c72, #2a5298)
- Success: Green (#4ade80, #34d399)
- Error: Red (#ef4444)
- Accent: Light blue (#60a5fa, #a0d2ff)

**No purple or blur colors used** as requested.

## Usage

### Option 1: Using the HTTP Server (Recommended)

```bash
./serve-dashboard.sh
```

Then open your browser to: http://localhost:8083/dashboard.html

### Option 2: Using Python Directly

```bash
cd /home/systemdr/git/sdc-java/day16/protobuf-log-system
python3 -m http.server 8083
```

Then open: http://localhost:8083/dashboard.html

### Option 3: Open Directly (May have CORS issues)

Simply open `dashboard.html` in your browser. Note: This may not work due to CORS restrictions when fetching metrics from localhost.

## Requirements

- All three services must be running:
  - Producer on port 8081
  - Consumer on port 8082
  - Gateway on port 8080
- Modern web browser with JavaScript enabled
- Python 3 (for the HTTP server script)

## Dashboard Layout

1. **Header**: System status and last update time
2. **Service Status Cards**: Health status of each service
3. **Metrics Grid**: Key performance indicators
4. **Charts Section**: 
   - Throughput line chart (JSON vs Protobuf over time)
   - Format comparison doughnut chart

## Troubleshooting

### Services Not Showing

- Ensure all services are running: `ps aux | grep java`
- Check service ports are accessible: `curl http://localhost:8081/actuator/health`

### Metrics Not Updating

- Verify Prometheus endpoints are accessible
- Check browser console for CORS errors
- Ensure services have actuator endpoints enabled

### Dashboard Not Loading

- Make sure you're accessing via HTTP (not file://)
- Use the provided server script to avoid CORS issues
- Check that port 8083 is not already in use

## Customization

The dashboard can be customized by editing `dashboard.html`:
- Refresh interval: Change `3000` in `setInterval(updateDashboard, 3000)`
- Chart history: Modify the `20` in chart data limits
- Colors: Update CSS variables in the `<style>` section

