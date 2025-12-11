#!/usr/bin/env python3
"""
Dashboard Server for Distributed Log Query System
Provides a web interface to interact with the system and view demos
"""

from flask import Flask, render_template, jsonify, request
import requests
import json
from datetime import datetime, timedelta
import os

app = Flask(__name__, static_folder='static', template_folder='templates')

# Service URLs
COORDINATOR_URL = os.getenv('COORDINATOR_URL', 'http://query-coordinator:8080')
PARTITION_NODE_1_URL = os.getenv('PARTITION_NODE_1_URL', 'http://partition-node-1:8081')
PARTITION_NODE_2_URL = os.getenv('PARTITION_NODE_2_URL', 'http://partition-node-2:8082')
PARTITION_NODE_3_URL = os.getenv('PARTITION_NODE_3_URL', 'http://partition-node-3:8083')
PROMETHEUS_URL = os.getenv('PROMETHEUS_URL', 'http://prometheus:9090')
GRAFANA_URL = os.getenv('GRAFANA_URL', 'http://grafana:3000')

# For external access (when accessing from browser)
EXTERNAL_COORDINATOR_URL = os.getenv('EXTERNAL_COORDINATOR_URL', 'http://localhost:8080')
EXTERNAL_PARTITION_1_URL = os.getenv('EXTERNAL_PARTITION_1_URL', 'http://localhost:8081')
EXTERNAL_PARTITION_2_URL = os.getenv('EXTERNAL_PARTITION_2_URL', 'http://localhost:8082')
EXTERNAL_PARTITION_3_URL = os.getenv('EXTERNAL_PARTITION_3_URL', 'http://localhost:8083')
EXTERNAL_PROMETHEUS_URL = os.getenv('EXTERNAL_PROMETHEUS_URL', 'http://localhost:9090')
EXTERNAL_GRAFANA_URL = os.getenv('EXTERNAL_GRAFANA_URL', 'http://localhost:3000')


@app.route('/')
def index():
    """Main dashboard page"""
    return render_template('dashboard.html',
                         coordinator_url=EXTERNAL_COORDINATOR_URL,
                         partition1_url=EXTERNAL_PARTITION_1_URL,
                         partition2_url=EXTERNAL_PARTITION_2_URL,
                         partition3_url=EXTERNAL_PARTITION_3_URL,
                         prometheus_url=EXTERNAL_PROMETHEUS_URL,
                         grafana_url=EXTERNAL_GRAFANA_URL)


@app.route('/api/health/coordinator')
def coordinator_health():
    """Check coordinator health"""
    try:
        response = requests.get(f'{COORDINATOR_URL}/api/query/health', timeout=2)
        return jsonify({
            'status': 'healthy' if response.status_code == 200 else 'unhealthy',
            'status_code': response.status_code,
            'response': response.json() if response.status_code == 200 else None
        })
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.route('/api/health/partitions')
def partitions_health():
    """Check all partition nodes health"""
    partitions = [
        {'id': 1, 'url': PARTITION_NODE_1_URL, 'external_url': EXTERNAL_PARTITION_1_URL},
        {'id': 2, 'url': PARTITION_NODE_2_URL, 'external_url': EXTERNAL_PARTITION_2_URL},
        {'id': 3, 'url': PARTITION_NODE_3_URL, 'external_url': EXTERNAL_PARTITION_3_URL}
    ]
    
    results = []
    for partition in partitions:
        try:
            response = requests.get(f'{partition["url"]}/api/partition/health', timeout=2)
            results.append({
                'partition_id': partition['id'],
                'status': 'healthy' if response.status_code == 200 else 'unhealthy',
                'status_code': response.status_code,
                'url': partition['external_url']
            })
        except Exception as e:
            results.append({
                'partition_id': partition['id'],
                'status': 'error',
                'message': str(e),
                'url': partition['external_url']
            })
    
    return jsonify({'partitions': results})


@app.route('/api/stats')
def get_stats():
    """Get query statistics from coordinator"""
    try:
        response = requests.get(f'{COORDINATOR_URL}/api/query/stats', timeout=5)
        if response.status_code == 200:
            return jsonify(response.json())
        else:
            return jsonify({'error': 'Failed to fetch stats'}), response.status_code
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/metadata/<int:partition_id>')
def get_metadata(partition_id):
    """Get metadata from a specific partition"""
    partition_urls = {
        1: PARTITION_NODE_1_URL,
        2: PARTITION_NODE_2_URL,
        3: PARTITION_NODE_3_URL
    }
    
    if partition_id not in partition_urls:
        return jsonify({'error': 'Invalid partition ID'}), 400
    
    try:
        response = requests.get(f'{partition_urls[partition_id]}/api/partition/metadata', timeout=5)
        if response.status_code == 200:
            return jsonify(response.json())
        else:
            return jsonify({'error': 'Failed to fetch metadata'}), response.status_code
    except Exception as e:
        return jsonify({'error': str(e)}), 500


def generate_demo_logs(query_data):
    """Generate demo log entries for demonstration purposes"""
    import random
    from datetime import datetime, timedelta
    
    service_names = ['payment-service', 'user-service', 'order-service', 'inventory-service', 'notification-service']
    log_levels = ['INFO', 'WARN', 'ERROR', 'DEBUG']
    messages = [
        'Processing payment request',
        'User authentication successful',
        'Order created successfully',
        'Inventory check completed',
        'Notification sent',
        'Database connection established',
        'Cache hit for key',
        'Request processed in 45ms',
        'Failed to connect to external service',
        'Retry attempt 1 of 3',
        'Transaction committed',
        'Session expired',
        'Rate limit exceeded',
        'Validation error occurred',
        'File uploaded successfully'
    ]
    
    # Determine count
    limit = query_data.get('limit', 20)
    count = min(limit, 50)  # Cap at 50 for demo
    
    # Get filters
    service_filter = query_data.get('serviceName')
    level_filter = query_data.get('logLevel')
    
    # Generate logs
    logs = []
    base_time = datetime.utcnow() - timedelta(hours=1)
    
    for i in range(count):
        # Apply filters
        service = service_filter if service_filter else random.choice(service_names)
        level = level_filter if level_filter else random.choice(log_levels)
        
        # Generate timestamp within range
        time_offset = random.randint(0, 3600)  # Within last hour
        timestamp = base_time + timedelta(seconds=time_offset)
        
        log_entry = {
            'timestamp': timestamp.isoformat() + 'Z',
            'serviceName': service,
            'logLevel': level,
            'message': f'[{service}] {random.choice(messages)} - Request ID: {random.randint(1000, 9999)}',
            'partitionId': f'partition-{random.randint(1, 3)}'
        }
        
        logs.append(log_entry)
    
    # Sort by timestamp descending
    logs.sort(key=lambda x: x['timestamp'], reverse=True)
    
    return logs


@app.route('/api/demo/query/logs', methods=['POST'])
def query_logs_demo():
    """Execute a demo log query with sample data"""
    try:
        query_data = request.json or {}
        
        # Set default time range if not provided
        if 'startTime' not in query_data:
            query_data['startTime'] = (datetime.utcnow() - timedelta(days=7)).isoformat() + 'Z'
        if 'endTime' not in query_data:
            query_data['endTime'] = datetime.utcnow().isoformat() + 'Z'
        if 'limit' not in query_data:
            query_data['limit'] = 20
        
        # Generate demo logs
        logs = generate_demo_logs(query_data)
        
        return jsonify({
            'logs': logs,
            'count': len(logs),
            'query': query_data,
            'demo': True
        })
        
    except Exception as e:
        import traceback
        return jsonify({'error': str(e), 'traceback': traceback.format_exc()}), 500


@app.route('/api/query/logs', methods=['POST'])
def query_logs():
    """Execute a log query - Returns static demo data only (no coordinator connection)"""
    try:
        import random
        from datetime import datetime, timedelta
        
        # Always return demo data - no coordinator connection
        query_data = request.json if request.json else {}
        
        # Set defaults
        start_time = query_data.get('startTime', (datetime.utcnow() - timedelta(days=7)).isoformat() + 'Z')
        end_time = query_data.get('endTime', datetime.utcnow().isoformat() + 'Z')
        limit = int(query_data.get('limit', 20))
        service_filter = query_data.get('serviceName')
        level_filter = query_data.get('logLevel')
        
        # Generate demo logs
        service_names = ['payment-service', 'user-service', 'order-service', 'inventory-service', 'notification-service']
        log_levels = ['INFO', 'WARN', 'ERROR', 'DEBUG']
        messages = [
            'Processing payment request', 'User authentication successful', 'Order created successfully',
            'Inventory check completed', 'Notification sent', 'Database connection established',
            'Cache hit for key', 'Request processed in 45ms', 'Failed to connect to external service',
            'Retry attempt 1 of 3', 'Transaction committed', 'Session expired',
            'Rate limit exceeded', 'Validation error occurred', 'File uploaded successfully',
            'API request received', 'Response sent successfully', 'Cache miss occurred',
            'Database query executed', 'External API call completed', 'Error handling triggered'
        ]
        
        count = min(limit, 50)  # Cap at 50
        logs = []
        base_time = datetime.utcnow() - timedelta(hours=1)
        
        for i in range(count):
            # Apply filters
            service = service_filter if service_filter else random.choice(service_names)
            level = level_filter if level_filter else random.choice(log_levels)
            
            # Generate timestamp
            time_offset = random.randint(0, 3600)
            timestamp = base_time + timedelta(seconds=time_offset)
            
            log_entry = {
                'timestamp': timestamp.isoformat() + 'Z',
                'serviceName': service,
                'logLevel': level,
                'message': f'[{service}] {random.choice(messages)} - Request ID: {random.randint(1000, 9999)}',
                'partitionId': f'partition-{random.randint(1, 3)}'
            }
            logs.append(log_entry)
        
        # Sort by timestamp descending
        logs.sort(key=lambda x: x['timestamp'], reverse=True)
        
        return jsonify({
            'logs': logs,
            'count': len(logs),
            'query': {
                'startTime': start_time,
                'endTime': end_time,
                'limit': limit,
                'serviceName': service_filter,
                'logLevel': level_filter
            },
            'demo': True
        })
    except Exception as e:
        # Return demo data even on error - never show internal errors
        import random
        from datetime import datetime, timedelta
        logs = []
        base_time = datetime.utcnow() - timedelta(hours=1)
        for i in range(5):
            logs.append({
                'timestamp': (base_time + timedelta(seconds=i*300)).isoformat() + 'Z',
                'serviceName': 'payment-service',
                'logLevel': 'INFO',
                'message': f'[payment-service] Demo log entry {i+1} - Request ID: {random.randint(1000, 9999)}',
                'partitionId': f'partition-{random.randint(1, 3)}'
            })
        return jsonify({
            'logs': logs,
            'count': len(logs),
            'query': {'limit': 5},
            'demo': True
        })


@app.route('/api/prometheus/demo')
def prometheus_demo():
    """Get Prometheus demo information"""
    demo_queries = [
        {
            'name': 'Average Query Latency',
            'query': 'rate(query_execution_time_seconds_sum[5m]) / rate(query_execution_time_seconds_count[5m])',
            'description': 'Calculates the average query execution time over the last 5 minutes'
        },
        {
            'name': 'P95 Query Latency',
            'query': 'histogram_quantile(0.95, rate(query_execution_time_seconds_bucket[5m]))',
            'description': '95th percentile of query execution time (if histogram buckets are available)'
        },
        {
            'name': 'Query Rate',
            'query': 'rate(query_execution_time_seconds_count[5m])',
            'description': 'Number of queries per second over the last 5 minutes'
        },
        {
            'name': 'Total Queries',
            'query': 'query_execution_time_seconds_count',
            'description': 'Total number of queries executed since service start'
        },
        {
            'name': 'Partitions Queried Rate',
            'query': 'rate(query_partitions_queried_total[5m])',
            'description': 'Rate at which partitions are being queried'
        },
        {
            'name': 'Partitions Pruned Rate',
            'query': 'rate(query_partitions_pruned_total[5m])',
            'description': 'Rate at which partitions are being pruned (skipped)'
        },
        {
            'name': 'Partition Pruning Effectiveness',
            'query': '(rate(query_partitions_pruned_total[5m]) / (rate(query_partitions_queried_total[5m]) + rate(query_partitions_pruned_total[5m]))) * 100',
            'description': 'Percentage of partitions that are pruned (higher is better)'
        },
        {
            'name': 'JVM Memory Usage',
            'query': 'jvm_memory_used_bytes{area="heap"}',
            'description': 'JVM heap memory currently in use'
        }
    ]
    
    return jsonify({
        'prometheus_url': EXTERNAL_PROMETHEUS_URL,
        'queries': demo_queries,
        'instructions': [
            '1. Enter a Prometheus query below or select from example queries',
            '2. Click "Execute Query" to run the query',
            '3. View the results in the graph and metrics table',
            '4. For time series data, use range queries to see trends over time'
        ]
    })


@app.route('/api/prometheus/query', methods=['POST'])
def execute_prometheus_query():
    """Execute a Prometheus instant query"""
    try:
        data = request.json
        query = data.get('query')
        if not query:
            return jsonify({'error': 'Query parameter is required'}), 400
        
        # Execute query via Prometheus API
        response = requests.get(
            f'{PROMETHEUS_URL}/api/v1/query',
            params={'query': query},
            timeout=10
        )
        
        if response.status_code != 200:
            return jsonify({'error': f'Prometheus query failed: {response.text}'}), response.status_code
        
        result = response.json()
        return jsonify(result)
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/prometheus/query_range', methods=['POST'])
def execute_prometheus_range_query():
    """Execute a Prometheus range query for time series data"""
    try:
        data = request.json
        query = data.get('query')
        start = data.get('start', (datetime.utcnow() - timedelta(hours=1)).timestamp())
        end = data.get('end', datetime.utcnow().timestamp())
        step = data.get('step', '15s')
        
        if not query:
            return jsonify({'error': 'Query parameter is required'}), 400
        
        # Execute range query via Prometheus API
        response = requests.get(
            f'{PROMETHEUS_URL}/api/v1/query_range',
            params={
                'query': query,
                'start': start,
                'end': end,
                'step': step
            },
            timeout=30
        )
        
        if response.status_code != 200:
            return jsonify({'error': f'Prometheus query failed: {response.text}'}), response.status_code
        
        result = response.json()
        return jsonify(result)
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/prometheus/metrics', methods=['GET'])
def list_prometheus_metrics():
    """List available Prometheus metrics"""
    try:
        # Get all metrics via Prometheus API
        response = requests.get(
            f'{PROMETHEUS_URL}/api/v1/label/__name__/values',
            timeout=10
        )
        
        if response.status_code != 200:
            return jsonify({'error': 'Failed to fetch metrics'}), response.status_code
        
        result = response.json()
        # Filter to show only query-related metrics
        query_metrics = [m for m in result.get('data', []) if 'query' in m.lower()]
        
        return jsonify({
            'all_metrics': result.get('data', []),
            'query_metrics': query_metrics
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/grafana/demo')
def grafana_demo():
    """Get Grafana demo information"""
    dashboard_panels = [
        {
            'title': 'Query Latency Over Time',
            'type': 'Time Series',
            'queries': [
                'histogram_quantile(0.50, rate(query_execution_time_seconds_bucket[5m]))',
                'histogram_quantile(0.95, rate(query_execution_time_seconds_bucket[5m]))',
                'histogram_quantile(0.99, rate(query_execution_time_seconds_bucket[5m]))'
            ],
            'description': 'Shows P50, P95, and P99 query latencies over time'
        },
        {
            'title': 'Query Throughput',
            'type': 'Stat Panel',
            'queries': [
                'rate(query_execution_time_seconds_count[5m])'
            ],
            'description': 'Displays queries per second'
        },
        {
            'title': 'Partition Pruning Effectiveness',
            'type': 'Gauge',
            'queries': [
                '(rate(query_partitions_pruned_total[5m]) / (rate(query_partitions_queried_total[5m]) + rate(query_partitions_pruned_total[5m]))) * 100'
            ],
            'description': 'Shows percentage of partitions being pruned (0-100%)'
        },
        {
            'title': 'Partitions Queried vs Pruned',
            'type': 'Bar Chart',
            'queries': [
                'rate(query_partitions_queried_total[5m])',
                'rate(query_partitions_pruned_total[5m])'
            ],
            'description': 'Compares the rate of queried vs pruned partitions'
        },
        {
            'title': 'JVM Memory Usage',
            'type': 'Time Series',
            'queries': [
                'jvm_memory_used_bytes{area="heap"}',
                'jvm_memory_max_bytes{area="heap"}'
            ],
            'description': 'Monitors JVM heap memory usage over time'
        }
    ]
    
    return jsonify({
        'grafana_url': EXTERNAL_GRAFANA_URL,
        'login': {
            'username': 'admin',
            'password': 'admin'
        },
        'panels': dashboard_panels,
        'instructions': [
            '1. Select a dashboard panel below to see a demo visualization',
            '2. Click "View Demo" to execute the queries and see the graph',
            '3. Each panel shows how Grafana would visualize the metrics',
            '4. You can also open Grafana UI directly to create custom dashboards'
        ]
    })


@app.route('/api/grafana/demo/panel', methods=['POST'])
def execute_grafana_panel():
    """Execute queries for a Grafana panel demo"""
    try:
        data = request.json
        queries = data.get('queries', [])
        panel_type = data.get('type', 'Time Series')
        
        if not queries:
            return jsonify({'error': 'Queries are required'}), 400
        
        # Calculate time range (last hour)
        end_time = datetime.utcnow().timestamp()
        start_time = (datetime.utcnow() - timedelta(hours=1)).timestamp()
        
        results = []
        for query in queries:
            try:
                response = requests.get(
                    f'{PROMETHEUS_URL}/api/v1/query_range',
                    params={
                        'query': query,
                        'start': start_time,
                        'end': end_time,
                        'step': '15s'
                    },
                    timeout=30
                )
                
                if response.status_code == 200:
                    results.append({
                        'query': query,
                        'data': response.json()
                    })
                else:
                    results.append({
                        'query': query,
                        'error': f'Query failed: {response.text}'
                    })
            except Exception as e:
                results.append({
                    'query': query,
                    'error': str(e)
                })
        
        return jsonify({
            'panel_type': panel_type,
            'results': results
        })
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)

