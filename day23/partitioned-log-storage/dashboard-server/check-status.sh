#!/bin/bash

echo "🔍 Checking Dashboard Server Status..."
echo ""

# Check if server is running
if lsof -i :3001 > /dev/null 2>&1; then
    echo "✅ Server is running on port 3001"
else
    echo "❌ Server is NOT running on port 3001"
    echo "   Start it with: cd dashboard-server && npm start"
    exit 1
fi

# Test main endpoint
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3001/)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Dashboard is accessible (HTTP $HTTP_CODE)"
else
    echo "❌ Dashboard returned HTTP $HTTP_CODE"
fi

# Test API endpoint
API_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3001/api/health)
if [ "$API_CODE" = "200" ]; then
    echo "✅ API endpoints are working (HTTP $API_CODE)"
else
    echo "⚠️  API returned HTTP $API_CODE"
fi

echo ""
echo "🌐 Access the dashboard at: http://localhost:3001"
echo ""

