#!/usr/bin/env python3
"""
Simple CORS proxy server for Prometheus and Grafana
This adds CORS headers to responses from services that don't support CORS
"""

from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.request import urlopen, Request
from urllib.error import URLError
import json
import gzip
import io

class CORSProxyHandler(BaseHTTPRequestHandler):
    def do_OPTIONS(self):
        """Handle preflight CORS requests"""
        self.send_response(200)
        self.send_cors_headers()
        self.end_headers()
    
    def do_GET(self):
        """Proxy GET requests with CORS headers"""
        self.proxy_request()
    
    def do_POST(self):
        """Proxy POST requests with CORS headers"""
        self.proxy_request()
    
    def do_PUT(self):
        """Proxy PUT requests with CORS headers"""
        self.proxy_request()
    
    def do_DELETE(self):
        """Proxy DELETE requests with CORS headers"""
        self.proxy_request()
    
    def send_cors_headers(self):
        """Add CORS headers to response"""
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS, PATCH')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization, Accept, X-Requested-With')
        self.send_header('Access-Control-Max-Age', '3600')
    
    def proxy_request(self):
        """Proxy the request to the target service"""
        path = self.path.lstrip('/')
        
        # Route to appropriate service
        if path.startswith('prometheus/'):
            target_url = f"http://localhost:9090/{path[11:]}"
        elif path.startswith('grafana/'):
            target_url = f"http://localhost:3000/{path[8:]}"
        elif path.startswith('registry/'):
            target_url = f"http://localhost:8081/{path[9:]}"
        elif path.startswith('gateway/'):
            target_url = f"http://localhost:8082/{path[8:]}"
        else:
            self.send_error(404, "Unknown proxy path. Use /prometheus/, /grafana/, /registry/, or /gateway/")
            return
        
        try:
            # Read request body if present
            content_length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(content_length) if content_length > 0 else None
            
            # Create request to target service
            req = Request(target_url)
            req.add_header('User-Agent', 'CORS-Proxy/1.0')
            
            # Don't request compression - we'll handle it if the server sends it
            req.add_header('Accept-Encoding', 'identity')
            
            # Copy relevant headers (but skip CORS, host, and compression headers)
            skip_headers = ['Host', 'Connection', 'Content-Length', 'Access-Control-Request-Method', 
                          'Access-Control-Request-Headers', 'Origin', 'Referer', 'Accept-Encoding']
            for header_name, header_value in self.headers.items():
                if header_name not in skip_headers:
                    req.add_header(header_name, header_value)
            
            # Set method for POST/PUT/PATCH/DELETE
            method = self.command
            if method in ['POST', 'PUT', 'PATCH', 'DELETE']:
                req.get_method = lambda: method
            
            # Make request
            try:
                if body:
                    response = urlopen(req, data=body, timeout=10)
                else:
                    response = urlopen(req, timeout=10)
                
                # Read response
                response_data = response.read()
                content_type = response.headers.get('Content-Type', 'application/json')
                content_encoding = response.headers.get('Content-Encoding', '').lower()
                
                # Handle compressed responses
                if content_encoding == 'gzip':
                    try:
                        response_data = gzip.decompress(response_data)
                        content_encoding = ''  # Remove encoding since we decompressed
                    except Exception as e:
                        print(f"Warning: Failed to decompress gzip: {e}")
                
                # Ensure proper encoding and content type
                try:
                    # Try to decode as UTF-8 to ensure proper encoding
                    if isinstance(response_data, bytes):
                        decoded_data = response_data.decode('utf-8')
                    else:
                        decoded_data = str(response_data)
                    
                    # Re-encode as UTF-8 bytes for sending
                    response_data = decoded_data.encode('utf-8')
                    
                    # Ensure proper content type for JSON responses
                    if 'json' in content_type.lower() or content_type == '':
                        # Try to parse as JSON to validate
                        try:
                            json.loads(decoded_data)
                            content_type = 'application/json; charset=utf-8'
                        except (ValueError, TypeError):
                            # Not valid JSON, keep original content type
                            if not content_type:
                                content_type = 'text/plain; charset=utf-8'
                except UnicodeDecodeError as e:
                    # If UTF-8 decoding fails, try other encodings
                    try:
                        decoded_data = response_data.decode('latin-1')
                        response_data = decoded_data.encode('utf-8')
                        if 'json' in content_type.lower():
                            content_type = 'application/json; charset=utf-8'
                    except Exception:
                        # If all else fails, send as-is but log the error
                        print(f"Warning: Could not decode response properly: {e}")
                        if not content_type:
                            content_type = 'application/octet-stream'
                
                # Send response with CORS headers (even for error status codes)
                self.send_response(response.getcode())
                self.send_header('Content-Type', content_type)
                # Don't forward content-encoding since we've decompressed if needed
                if content_encoding:
                    self.send_header('Content-Encoding', '')  # Remove encoding header
                self.send_cors_headers()
                self.end_headers()
                self.wfile.write(response_data)
                
            except URLError as e:
                # Handle URL errors (connection refused, etc.)
                error_code = 502
                error_msg = str(e)
                if hasattr(e, 'code') and e.code:
                    error_code = e.code
                elif hasattr(e, 'reason') and 'refused' in str(e.reason).lower():
                    error_code = 503
                
                # Send error with CORS headers
                self.send_response(error_code)
                self.send_header('Content-Type', 'application/json')
                self.send_cors_headers()
                self.end_headers()
                error_response = json.dumps({
                    "error": "Proxy Error",
                    "message": error_msg,
                    "status": error_code
                }).encode('utf-8')
                self.wfile.write(error_response)
                
        except Exception as e:
            # Send error with CORS headers
            self.send_response(500)
            self.send_header('Content-Type', 'application/json')
            self.send_cors_headers()
            self.end_headers()
            error_response = json.dumps({
                "error": "Internal Proxy Error",
                "message": str(e),
                "status": 500
            }).encode('utf-8')
            self.wfile.write(error_response)
    
    def log_message(self, format, *args):
        """Suppress default logging"""
        pass

def run(port=8083):
    server_address = ('', port)
    httpd = HTTPServer(server_address, CORSProxyHandler)
    print(f"CORS Proxy server running on http://localhost:{port}")
    print("Routes:")
    print("  /prometheus/* -> http://localhost:9090/*")
    print("  /grafana/* -> http://localhost:3000/*")
    print("  /registry/* -> http://localhost:8081/*")
    print("  /gateway/* -> http://localhost:8082/*")
    print("\nPress Ctrl+C to stop")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down proxy server...")
        httpd.shutdown()

if __name__ == '__main__':
    import sys
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8083
    run(port)

