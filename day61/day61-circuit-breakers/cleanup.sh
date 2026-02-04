#!/bin/bash

# =============================================================================
# Cleanup Script - Stop containers and remove unused Docker resources
# =============================================================================
# This script:
#   1. Stops all running containers for this project
#   2. Removes containers, networks, and volumes
#   3. Prunes unused Docker resources (optional)
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "Docker Cleanup Script"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Step 1: Stop and remove containers, networks, volumes
print_info "Stopping and removing Docker Compose services..."
if docker compose ps -q > /dev/null 2>&1; then
    docker compose down -v --remove-orphans
    print_info "Docker Compose services stopped and removed."
else
    print_warn "No running Docker Compose services found."
fi

# Step 2: Remove any orphaned containers with project name
print_info "Checking for orphaned containers..."
ORPHANED=$(docker ps -a --filter "name=api-gateway\|log-producer\|log-consumer\|kafka\|zookeeper\|postgres\|redis\|prometheus\|grafana" --format "{{.Names}}" 2>/dev/null || true)

if [ -n "$ORPHANED" ]; then
    print_warn "Found orphaned containers:"
    echo "$ORPHANED"
    read -p "Remove these containers? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "$ORPHANED" | xargs -r docker rm -f
        print_info "Orphaned containers removed."
    else
        print_info "Skipping orphaned container removal."
    fi
else
    print_info "No orphaned containers found."
fi

# Step 3: Remove unused Docker resources (optional)
echo ""
read -p "Remove unused Docker resources (images, networks, volumes)? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_info "Removing unused Docker resources..."
    
    # Remove unused images
    print_info "Pruning unused images..."
    docker image prune -f
    
    # Remove unused networks
    print_info "Pruning unused networks..."
    docker network prune -f
    
    # Remove unused volumes (be careful with this)
    print_warn "Pruning unused volumes (this may remove data)..."
    read -p "Continue with volume pruning? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker volume prune -f
        print_info "Unused volumes removed."
    else
        print_info "Skipping volume pruning."
    fi
    
    print_info "Docker resource cleanup completed."
else
    print_info "Skipping Docker resource pruning."
fi

# Step 4: Show summary
echo ""
print_info "Cleanup summary:"
echo "  - Containers: $(docker ps -a -q --filter "name=api-gateway\|log-producer\|log-consumer\|kafka\|zookeeper\|postgres\|redis\|prometheus\|grafana" 2>/dev/null | wc -l) remaining"
echo "  - Networks: $(docker network ls -q --filter "name=day61\|lognet" 2>/dev/null | wc -l) remaining"
echo "  - Volumes: $(docker volume ls -q --filter "name=day61" 2>/dev/null | wc -l) remaining"

echo ""
print_info "Cleanup completed!"
echo ""
