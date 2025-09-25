#!/bin/bash

# Script to run Courier Microservices using Docker Hub images
# This script pulls and runs the latest images from Docker Hub

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DOCKER_USERNAME=${1:-"your-dockerhub-username"}
IMAGE_TAG=${2:-"latest"}
COMPOSE_FILE="docker-compose.dockerhub.yml"

echo -e "${BLUE}=== Courier Microservices - Docker Hub Deployment ===${NC}"
echo -e "${BLUE}Docker Hub Username: $DOCKER_USERNAME${NC}"
echo -e "${BLUE}Image Tag: $IMAGE_TAG${NC}"
echo ""

# Function to check if Docker is running
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        echo -e "${RED}Error: Docker is not running. Please start Docker and try again.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Docker is running${NC}"
}

# Function to check if images exist on Docker Hub
check_images() {
    echo -e "${YELLOW}Checking if images exist on Docker Hub...${NC}"
    
    local images=(
        "$DOCKER_USERNAME/courier-main-service:$IMAGE_TAG"
        "$DOCKER_USERNAME/courier-customer-interface:$IMAGE_TAG"
        "$DOCKER_USERNAME/courier-frontend:$IMAGE_TAG"
    )
    
    for image in "${images[@]}"; do
        echo -e "${BLUE}Checking: $image${NC}"
        if docker manifest inspect "$image" >/dev/null 2>&1; then
            echo -e "${GREEN}✓ Found: $image${NC}"
        else
            echo -e "${YELLOW}⚠ Warning: $image not found on Docker Hub${NC}"
            echo -e "${YELLOW}  Make sure the GitHub Actions workflow has completed successfully${NC}"
        fi
    done
}

# Function to pull latest images
pull_images() {
    echo -e "${YELLOW}Pulling latest images from Docker Hub...${NC}"
    
    export DOCKER_USERNAME="$DOCKER_USERNAME"
    export IMAGE_TAG="$IMAGE_TAG"
    
    # Pull images explicitly to show progress
    docker pull "$DOCKER_USERNAME/courier-main-service:$IMAGE_TAG" || echo -e "${YELLOW}Warning: Could not pull main service image${NC}"
    docker pull "$DOCKER_USERNAME/courier-customer-interface:$IMAGE_TAG" || echo -e "${YELLOW}Warning: Could not pull customer interface image${NC}"
    docker pull "$DOCKER_USERNAME/courier-frontend:$IMAGE_TAG" || echo -e "${YELLOW}Warning: Could not pull frontend image${NC}"
    
    echo -e "${GREEN}✓ Image pull completed${NC}"
}

# Function to start services
start_services() {
    echo -e "${YELLOW}Starting Courier Microservices...${NC}"
    
    # Export environment variables for docker-compose
    export DOCKER_USERNAME="$DOCKER_USERNAME"
    export IMAGE_TAG="$IMAGE_TAG"
    
    # Create logs directory
    mkdir -p logs
    
    # Start services with docker-compose
    docker-compose -f "$COMPOSE_FILE" up -d
    
    echo -e "${GREEN}✓ Services started${NC}"
}

# Function to show service status
show_status() {
    echo -e "${YELLOW}Checking service status...${NC}"
    
    # Wait a moment for services to start
    sleep 10
    
    echo -e "${BLUE}Container Status:${NC}"
    docker-compose -f "$COMPOSE_FILE" ps
    
    echo ""
    echo -e "${BLUE}Service Health Checks:${NC}"
    
    # Check main service
    if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Main Service (8080): Healthy${NC}"
    else
        echo -e "${YELLOW}⚠ Main Service (8080): Starting up...${NC}"
    fi
    
    # Check customer interface
    if curl -f http://localhost:8081/actuator/health >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Customer Interface (8081): Healthy${NC}"
    else
        echo -e "${YELLOW}⚠ Customer Interface (8081): Starting up...${NC}"
    fi
    
    # Check frontend
    if curl -f http://localhost:3000/health >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Frontend (3000): Healthy${NC}"
    else
        echo -e "${YELLOW}⚠ Frontend (3000): Starting up...${NC}"
    fi
}

# Function to show access URLs
show_urls() {
    echo ""
    echo -e "${GREEN}=== Access URLs ===${NC}"
    echo -e "${BLUE}Frontend Application:${NC} http://localhost:3000"
    echo -e "${BLUE}Main Service API:${NC} http://localhost:8080/api/v1"
    echo -e "${BLUE}Customer Interface API:${NC} http://localhost:8081/api/v1/customer"
    echo -e "${BLUE}Kafka UI:${NC} http://localhost:8090"
    echo ""
    echo -e "${GREEN}=== API Documentation ===${NC}"
    echo -e "${BLUE}Main Service Swagger:${NC} http://localhost:8080/swagger-ui.html"
    echo -e "${BLUE}Customer Interface Swagger:${NC} http://localhost:8081/swagger-ui.html"
    echo ""
    echo -e "${GREEN}=== Health Endpoints ===${NC}"
    echo -e "${BLUE}Main Service Health:${NC} http://localhost:8080/actuator/health"
    echo -e "${BLUE}Customer Interface Health:${NC} http://localhost:8081/actuator/health"
    echo -e "${BLUE}Frontend Health:${NC} http://localhost:3000/health"
}

# Function to show logs
show_logs() {
    echo -e "${YELLOW}Recent logs from services:${NC}"
    echo ""
    echo -e "${BLUE}=== Main Service Logs ===${NC}"
    docker-compose -f "$COMPOSE_FILE" logs --tail=10 courier-main-service
    echo ""
    echo -e "${BLUE}=== Customer Interface Logs ===${NC}"
    docker-compose -f "$COMPOSE_FILE" logs --tail=10 courier-customer-interface
    echo ""
    echo -e "${BLUE}=== Frontend Logs ===${NC}"
    docker-compose -f "$COMPOSE_FILE" logs --tail=10 courier-frontend
}

# Function to stop services
stop_services() {
    echo -e "${YELLOW}Stopping Courier Microservices...${NC}"
    docker-compose -f "$COMPOSE_FILE" down
    echo -e "${GREEN}✓ Services stopped${NC}"
}

# Function to clean up
cleanup() {
    echo -e "${YELLOW}Cleaning up Docker resources...${NC}"
    docker-compose -f "$COMPOSE_FILE" down -v --remove-orphans
    docker system prune -f
    echo -e "${GREEN}✓ Cleanup completed${NC}"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [DOCKER_USERNAME] [IMAGE_TAG] [ACTION]"
    echo ""
    echo "Parameters:"
    echo "  DOCKER_USERNAME  - Your Docker Hub username (default: your-dockerhub-username)"
    echo "  IMAGE_TAG        - Image tag to use (default: latest)"
    echo ""
    echo "Actions:"
    echo "  start    - Start all services (default)"
    echo "  stop     - Stop all services"
    echo "  restart  - Restart all services"
    echo "  status   - Show service status"
    echo "  logs     - Show recent logs"
    echo "  cleanup  - Stop services and clean up resources"
    echo ""
    echo "Examples:"
    echo "  $0 myusername latest start"
    echo "  $0 myusername v1.0.0"
    echo "  $0 myusername latest logs"
}

# Main execution
main() {
    local action=${3:-start}
    
    case $action in
        "start")
            check_docker
            check_images
            pull_images
            start_services
            show_status
            show_urls
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            stop_services
            sleep 5
            check_docker
            pull_images
            start_services
            show_status
            show_urls
            ;;
        "status")
            show_status
            show_urls
            ;;
        "logs")
            show_logs
            ;;
        "cleanup")
            cleanup
            ;;
        "help"|"-h"|"--help")
            show_usage
            ;;
        *)
            echo -e "${RED}Error: Unknown action '$action'${NC}"
            show_usage
            exit 1
            ;;
    esac
}

# Check if help is requested
if [[ "$1" == "help" || "$1" == "-h" || "$1" == "--help" ]]; then
    show_usage
    exit 0
fi

# Validate Docker Hub username
if [[ "$DOCKER_USERNAME" == "your-dockerhub-username" ]]; then
    echo -e "${YELLOW}Warning: Using default Docker Hub username. Please provide your actual username:${NC}"
    echo -e "${BLUE}Usage: $0 <your-dockerhub-username> [image-tag] [action]${NC}"
    echo ""
    read -p "Enter your Docker Hub username: " DOCKER_USERNAME
    if [[ -z "$DOCKER_USERNAME" ]]; then
        echo -e "${RED}Error: Docker Hub username is required${NC}"
        exit 1
    fi
fi

# Execute main function
main "$@"

echo ""
echo -e "${GREEN}=== Useful Commands ===${NC}"
echo -e "${BLUE}View all logs:${NC} docker-compose -f $COMPOSE_FILE logs -f"
echo -e "${BLUE}Stop services:${NC} $0 $DOCKER_USERNAME $IMAGE_TAG stop"
echo -e "${BLUE}Restart services:${NC} $0 $DOCKER_USERNAME $IMAGE_TAG restart"
echo -e "${BLUE}Clean up:${NC} $0 $DOCKER_USERNAME $IMAGE_TAG cleanup"
