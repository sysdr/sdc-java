#!/usr/bin/env python3
"""
Simple metrics proxy to fetch data from Spring Boot services
and serve it to the monitoring dashboard
"""

import json
import requests
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
import time

class MetricsProxyHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed_path = urlparse(self.path)
        
        if parsed_path.path == '/api/metrics':
            # Get service parameter
            query_params = parse_qs(parsed_path.query)
            service = query_params.get('service', ['gateway'])[0]
            
            try:
                # Fetch data from the specified service
                port = {'gateway': 8080, 'producer': 8081, 'consumer': 8082}.get(service, 8080)
                
                # Get health data
                health_response = requests.get(f'http://localhost:{port}/actuator/health', timeout=5)
                health_data = health_response.json()
                
                # Get metrics data
                metrics_response = requests.get(f'http://localhost:{port}/actuator/prometheus', timeout=5)
                metrics_text = metrics_response.text
                
                # Parse some basic metrics
                metrics = self.parse_prometheus_metrics(metrics_text)
                
                # Combine data
                result = {
                    'service': service,
                    'health': health_data,
                    'metrics': metrics,
                    'timestamp': time.time()
                }
                
                self.send_response(200)
                self.send_header('Content-type', 'application/json')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(json.dumps(result).encode())
                
            except Exception as e:
                error_response = {
                    'service': service,
                    'error': str(e),
                    'timestamp': time.time()
                }
                
                self.send_response(500)
                self.send_header('Content-type', 'application/json')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(json.dumps(error_response).encode())
        
        elif parsed_path.path == '/api/status':
            # Return status of all services
            services = {}
            for service, port in [('gateway', 8080), ('producer', 8081), ('consumer', 8082)]:
                try:
                    response = requests.get(f'http://localhost:{port}/actuator/health', timeout=2)
                    services[service] = {
                        'status': 'UP' if response.status_code == 200 else 'DOWN',
                        'port': port,
                        'response_time': response.elapsed.total_seconds()
                    }
                except:
                    services[service] = {
                        'status': 'DOWN',
                        'port': port,
                        'response_time': None
                    }
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(json.dumps(services).encode())
        
        else:
            self.send_response(404)
            self.end_headers()
    
    def parse_prometheus_metrics(self, metrics_text):
        """Parse Prometheus metrics text into a dictionary"""
        metrics = {}
        for line in metrics_text.split('\n'):
            if line.startswith('#') or line.strip() == '':
                continue
            
            parts = line.split(' ')
            if len(parts) >= 2:
                metric_name = parts[0].split('{')[0]
                try:
                    value = float(parts[1])
                    metrics[metric_name] = value
                except ValueError:
                    continue
        
        return metrics

if __name__ == '__main__':
    server = HTTPServer(('localhost', 8084), MetricsProxyHandler)
    print("🚀 Metrics Proxy Server running on http://localhost:8084")
    print("📊 Available endpoints:")
    print("   - /api/status - Get status of all services")
    print("   - /api/metrics?service=gateway - Get metrics for specific service")
    print("   - /api/metrics?service=producer")
    print("   - /api/metrics?service=consumer")
    server.serve_forever()
