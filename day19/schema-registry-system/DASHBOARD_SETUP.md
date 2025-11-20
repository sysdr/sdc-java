# Dashboard Setup Instructions

## CORS Fix - Required Steps

The dashboard requires CORS (Cross-Origin Resource Sharing) to be enabled on the services. Follow these steps:

### 1. Rebuild Spring Boot Services

The CORS configuration has been added to both services. You need to rebuild them:

```bash
# Rebuild the project
mvn clean package -DskipTests

# Then restart your services:
# Terminal 1:
cd schema-registry && mvn spring-boot:run

# Terminal 2:
cd validation-gateway && mvn spring-boot:run
```

### 2. Start the Dashboard Server

The dashboard server includes a CORS proxy for Prometheus and Grafana:

```bash
./serve-dashboard.sh
```

This will:
- Start the dashboard server on port 8080
- Start the CORS proxy on port 8083 (for Prometheus/Grafana)

### 3. Access the Dashboard

Open in your browser:
- **http://localhost:8080/dashboard.html**

Or if using WSL2 from Windows:
- **http://127.0.0.1:8080/dashboard.html**
- **http://172.18.63.108:8080/dashboard.html** (use your WSL2 IP)

### 4. Manual CORS Proxy (Optional)

If you want to run the CORS proxy separately:

```bash
python3 cors-proxy.py 8083
```

## What Was Fixed

1. **Added CORS Configuration** to:
   - `schema-registry` service (allows requests from dashboard)
   - `validation-gateway` service (allows requests from dashboard)

2. **Created CORS Proxy** for:
   - Prometheus (port 9090)
   - Grafana (port 3000)

3. **Updated Dashboard** to:
   - Use proxy for Prometheus/Grafana requests
   - Handle CORS errors gracefully
   - Fallback to direct connections if proxy fails

## Troubleshooting

### Still seeing CORS errors?

1. **Make sure services are rebuilt** - CORS config only works after rebuild
2. **Check services are running**:
   - Schema Registry: http://localhost:8081/actuator/health
   - Validation Gateway: http://localhost:8082/actuator/health
   - Prometheus: http://localhost:9090
   - Grafana: http://localhost:3000

3. **Check CORS proxy is running**:
   - Should be on port 8083
   - Check: `curl http://localhost:8083/prometheus/api/v1/status/config`

4. **Browser console** - Check for specific error messages

### Services show as offline?

- Make sure all services are running
- Check firewall settings
- Verify ports are not blocked

## Notes

- The CORS configuration allows all origins (`*`) for development
- In production, you should restrict CORS to specific origins
- The proxy server is a simple development tool and should not be used in production

