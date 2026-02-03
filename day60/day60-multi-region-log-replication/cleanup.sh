#!/bin/bash
# =============================================================================
# Cleanup Script - Stop containers and remove unused Docker resources
# =============================================================================

set -e

echo "=========================================="
echo "Docker Cleanup Script"
echo "=========================================="

# Stop all running containers
echo "[1/6] Stopping all running containers..."
docker stop $(docker ps -aq) 2>/dev/null || echo "No containers to stop"

# Remove all containers
echo "[2/6] Removing all containers..."
docker rm $(docker ps -aq) 2>/dev/null || echo "No containers to remove"

# Remove all unused images
echo "[3/6] Removing unused Docker images..."
docker image prune -a -f || echo "No unused images to remove"

# Remove all unused volumes
echo "[4/6] Removing unused Docker volumes..."
docker volume prune -f || echo "No unused volumes to remove"

# Remove all unused networks
echo "[5/6] Removing unused Docker networks..."
docker network prune -f || echo "No unused networks to remove"

# Remove all unused build cache
echo "[6/6] Removing unused Docker build cache..."
docker builder prune -a -f || echo "No build cache to remove"

echo ""
echo "=========================================="
echo "Cleanup completed successfully!"
echo "=========================================="
echo ""
echo "Summary:"
docker system df
