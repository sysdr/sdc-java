#!/usr/bin/env python3
"""
Simple HTTP server to serve the dashboard and proxy requests to avoid CORS issues
"""

import http.server
import socketserver
import urllib.request
import urllib.parse
import json
from urllib.error import URLError

PORT = 8083

class DashboardHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, format, *args):
        # Suppress default logging, we'll handle errors ourselves
        pass
    
    def do_GET(self):
        # Handle root and dashboard
        if self.path == '/' or self.path == '/dashboard.html':
            self.path = '/dashboard.html'
            return super().do_GET()
        
        # Proxy Prometheus health check (check this first before /api/prometheus)
        if self.path == '/api/prometheus/health' or self.path.startswith('/api/prometheus/health?'):
            try:
                prom_url = 'http://localhost:9090/-/healthy'
                req = urllib.request.Request(prom_url)
                with urllib.request.urlopen(req, timeout=2) as response:
                    status = 'UP' if response.status == 200 else 'DOWN'
                    self.send_response(200)
                    self.send_header('Content-Type', 'application/json')
                    self.send_header('Access-Control-Allow-Origin', '*')
                    self.end_headers()
                    self.wfile.write(json.dumps({'status': status}).encode())
            except Exception as e:
                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(json.dumps({'status': 'DOWN', 'error': str(e)}).encode())
            return
        
        # Proxy Prometheus queries
        if self.path.startswith('/api/prometheus'):
            try:
                # Parse query parameter (parse_qs automatically decodes it)
                parsed = urllib.parse.urlparse(self.path)
                params = urllib.parse.parse_qs(parsed.query)
                query = params.get('query', [''])[0]
                
                if not query:
                    self.send_response(400)
                    self.send_header('Content-Type', 'application/json')
                    self.send_header('Access-Control-Allow-Origin', '*')
                    self.end_headers()
                    self.wfile.write(json.dumps({
                        'status': 'error',
                        'error': 'Missing query parameter'
                    }).encode())
                    return
                
                # Re-encode the query for Prometheus (parse_qs decoded it, so we need to encode again)
                # Keep parentheses, brackets, commas, etc. safe for Prometheus query syntax
                query_encoded = urllib.parse.quote(query, safe='()[],=')
                prom_url = f'http://localhost:9090/api/v1/query?query={query_encoded}'
                
                req = urllib.request.Request(prom_url)
                with urllib.request.urlopen(req, timeout=10) as response:
                    data = response.read()
                    self.send_response(200)
                    self.send_header('Content-Type', 'application/json')
                    self.send_header('Access-Control-Allow-Origin', '*')
                    self.end_headers()
                    self.wfile.write(data)
            except urllib.error.HTTPError as e:
                # Prometheus returned an error
                error_data = e.read().decode() if hasattr(e, 'read') else str(e)
                self.send_response(200)  # Return 200 but with error in JSON
                self.send_header('Content-Type', 'application/json')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(json.dumps({
                    'status': 'error',
                    'error': 'Prometheus query failed',
                    'message': error_data[:200]  # Limit error message length
                }).encode())
            except urllib.error.URLError as e:
                # Prometheus not available
                self.send_response(200)  # Return 200 but with error in JSON
                self.send_header('Content-Type', 'application/json')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(json.dumps({
                    'status': 'error',
                    'error': 'Prometheus unavailable',
                    'message': str(e)
                }).encode())
            except Exception as e:
                import traceback
                error_trace = traceback.format_exc()
                self.send_response(200)  # Return 200 but with error in JSON
                self.send_header('Content-Type', 'application/json')
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
                self.wfile.write(json.dumps({
                    'status': 'error',
                    'error': str(e),
                    'trace': error_trace[:500]  # Limit trace length
                }).encode())
            return
        
        # Proxy health checks
        if self.path.startswith('/api/health/'):
            service = self.path.split('/api/health/')[1].split('?')[0]  # Remove query params if any
            service_urls = {
                'api-gateway': 'http://localhost:8080',
                'normalizer': 'http://localhost:8081',
                'producer': 'http://localhost:8082'
            }
            
            if service in service_urls:
                try:
                    url = f"{service_urls[service]}/actuator/health"
                    req = urllib.request.Request(url)
                    with urllib.request.urlopen(req, timeout=3) as response:
                        data = response.read()
                        self.send_response(200)
                        self.send_header('Content-Type', 'application/json')
                        self.send_header('Access-Control-Allow-Origin', '*')
                        self.end_headers()
                        self.wfile.write(data)
                except urllib.error.URLError as e:
                    # Service not available
                    self.send_response(200)
                    self.send_header('Content-Type', 'application/json')
                    self.send_header('Access-Control-Allow-Origin', '*')
                    self.end_headers()
                    self.wfile.write(json.dumps({
                        'status': 'DOWN',
                        'error': 'Service unavailable'
                    }).encode())
                except Exception as e:
                    self.send_response(200)
                    self.send_header('Content-Type', 'application/json')
                    self.send_header('Access-Control-Allow-Origin', '*')
                    self.end_headers()
                    self.wfile.write(json.dumps({
                        'status': 'DOWN',
                        'error': str(e)
                    }).encode())
            else:
                self.send_response(404)
                self.send_header('Access-Control-Allow-Origin', '*')
                self.end_headers()
            return
        
        return super().do_GET()
    
    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        super().end_headers()

if __name__ == "__main__":
    import os
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    
    with socketserver.TCPServer(("", PORT), DashboardHandler) as httpd:
        print(f"Dashboard server running on http://localhost:{PORT}")
        print(f"Open http://localhost:{PORT}/dashboard.html in your browser")
        print("Press Ctrl+C to stop")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down server...")

