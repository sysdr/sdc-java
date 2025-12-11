# Dashboard Server

A web-based dashboard for the Distributed Log Query System that provides:

- **Query Operations**: Execute all types of log queries (time-range, service, log-level, combined filters)
- **System Health Monitoring**: Real-time health checks for coordinator and partition nodes
- **Statistics**: View system statistics and partition metadata
- **Prometheus Demo**: Example queries showing how to use Prometheus for monitoring
- **Grafana Demo**: Example dashboard panels showing how to visualize metrics

## Features

- Professional, modern UI design
- All mandatory operations accessible via web interface
- Real-time health status updates
- Interactive query forms with validation
- Demo sections for Prometheus and Grafana (not integrated, just showing how they work)

## Access

Once the system is running, access the dashboard at:
- **URL**: http://localhost:5000

## Architecture

The dashboard server is a Flask (Python) application that:
- Serves a web interface for interacting with the system
- Proxies API requests to the query coordinator and partition nodes
- Provides demo information for Prometheus and Grafana

## Running

The dashboard is automatically started when you run `./setup.sh`. It's included in the docker-compose.yml file.

To rebuild the dashboard:
```bash
docker-compose build dashboard
docker-compose up -d dashboard
```

