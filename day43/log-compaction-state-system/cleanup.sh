#!/bin/bash

# Cleanup script for Docker resources
# This script stops all containers and removes unused Docker resources

set -e

echo "========================================="
echo "Docker Cleanup Script"
echo "========================================="

# Stop all running containers
echo ""
echo "Step 1: Stopping all running containers..."
docker-compose down 2>/dev/null || true
docker stop $(docker ps -aq) 2>/dev/null || true
echo "✓ All containers stopped"

# Remove all stopped containers
echo ""
echo "Step 2: Removing stopped containers..."
docker rm $(docker ps -aq) 2>/dev/null || true
echo "✓ Stopped containers removed"

# Remove unused images
echo ""
echo "Step 3: Removing unused Docker images..."
docker image prune -a -f 2>/dev/null || true
echo "✓ Unused images removed"

# Remove unused volumes
echo ""
echo "Step 4: Removing unused Docker volumes..."
docker volume prune -f 2>/dev/null || true
echo "✓ Unused volumes removed"

# Remove unused networks
echo ""
echo "Step 5: Removing unused Docker networks..."
docker network prune -f 2>/dev/null || true
echo "✓ Unused networks removed"

# System prune (optional - removes everything unused)
echo ""
echo "Step 6: Performing system-wide cleanup..."
docker system prune -a -f --volumes 2>/dev/null || true
echo "✓ System cleanup completed"

echo ""
echo "========================================="
echo "Cleanup completed successfully!"
echo "========================================="
echo ""
echo "Remaining Docker resources:"
docker ps -a
echo ""
docker images
echo ""
docker volume ls
echo ""
