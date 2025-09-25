#!/bin/bash

# Deployment script for Courier Microservices on Kubernetes using Docker Hub images
# This script deploys all services with 2 pods each

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="courier-system"
DOCKERHUB_USERNAME=${1:-"YOUR_DOCKERHUB_USERNAME"}
IMAGE_TAG=${2:-"latest"}

echo -e "${BLUE}=== Courier Microservices Kubernetes Deployment ===${NC}"
echo -e "${BLUE}Namespace: $NAMESPACE${NC}"
echo -e "${BLUE}Docker Hub Username: $DOCKERHUB_USERNAME${NC}"
echo -e "${BLUE}Image Tag: $IMAGE_TAG${NC}"
echo ""

# Function to check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"
    
    # Check if kubectl is available
    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}Error: kubectl is not installed${NC}"
        exit 1
    fi
    
    # Check kubectl connectivity
    if ! kubectl cluster-info &> /dev/null; then
        echo -e "${RED}Error: Cannot connect to Kubernetes cluster${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Prerequisites check passed${NC}"
}

# Function to validate Docker Hub username
validate_dockerhub_username() {
    if [[ "$DOCKERHUB_USERNAME" == "YOUR_DOCKERHUB_USERNAME" ]]; then
        echo -e "${YELLOW}Warning: Please provide your actual Docker Hub username${NC}"
        echo -e "${BLUE}Usage: $0 <dockerhub-username> [image-tag]${NC}"
        echo ""
        read -p "Enter your Docker Hub username: " DOCKERHUB_USERNAME
        if [[ -z "$DOCKERHUB_USERNAME" ]]; then
            echo -e "${RED}Error: Docker Hub username is required${NC}"
            exit 1
        fi
    fi
}

# Function to update image references
update_image_references() {
    echo -e "${YELLOW}Updating Docker Hub image references...${NC}"
    
    # Update image references in deployment files
    sed -i.bak "s/YOUR_DOCKERHUB_USERNAME/$DOCKERHUB_USERNAME/g" k8s/dockerhub/*-deployment.yaml
    sed -i.bak "s/:latest/:$IMAGE_TAG/g" k8s/dockerhub/*-deployment.yaml
    
    echo -e "${GREEN}✓ Image references updated${NC}"
}

# Function to create namespace
create_namespace() {
    echo -e "${YELLOW}Creating namespace...${NC}"
    kubectl apply -f k8s/dockerhub/namespace.yaml
    echo -e "${GREEN}✓ Namespace created/updated${NC}"
}

# Function to create secrets
create_secrets() {
    echo -e "${YELLOW}Creating secrets...${NC}"
    
    # Check if Docker Hub secret needs to be created manually
    if ! kubectl get secret dockerhub-secret -n $NAMESPACE &> /dev/null; then
        echo -e "${YELLOW}Creating Docker Hub secret...${NC}"
        echo -e "${BLUE}Please enter your Docker Hub credentials:${NC}"
        read -p "Docker Hub Username: " DOCKER_USER
        read -s -p "Docker Hub Password/Token: " DOCKER_PASS
        echo ""
        
        kubectl create secret docker-registry dockerhub-secret \
            --docker-server=docker.io \
            --docker-username="$DOCKER_USER" \
            --docker-password="$DOCKER_PASS" \
            --namespace=$NAMESPACE
    fi
    
    # Apply other secrets
    kubectl apply -f k8s/dockerhub/secrets.yaml
    echo -e "${GREEN}✓ Secrets created/updated${NC}"
}

# Function to create config maps
create_configmaps() {
    echo -e "${YELLOW}Creating config maps...${NC}"
    kubectl apply -f k8s/dockerhub/configmap.yaml
    echo -e "${GREEN}✓ Config maps created/updated${NC}"
}

# Function to deploy infrastructure
deploy_infrastructure() {
    echo -e "${YELLOW}Deploying infrastructure services...${NC}"
    kubectl apply -f k8s/dockerhub/infrastructure-deployments.yaml
    
    echo -e "${YELLOW}Waiting for infrastructure services to be ready...${NC}"
    kubectl wait --for=condition=available --timeout=300s deployment/courier-postgresql -n $NAMESPACE
    kubectl wait --for=condition=available --timeout=300s deployment/courier-zookeeper -n $NAMESPACE
    kubectl wait --for=condition=available --timeout=300s deployment/courier-kafka -n $NAMESPACE
    kubectl wait --for=condition=available --timeout=300s deployment/courier-redis -n $NAMESPACE
    
    echo -e "${GREEN}✓ Infrastructure services deployed${NC}"
}

# Function to initialize Kafka topics
initialize_kafka_topics() {
    echo -e "${YELLOW}Initializing Kafka topics...${NC}"
    kubectl apply -f k8s/dockerhub/kafka-topic-init.yaml
    
    # Wait for job completion
    kubectl wait --for=condition=complete --timeout=300s job/kafka-topic-init -n $NAMESPACE
    
    echo -e "${GREEN}✓ Kafka topics initialized${NC}"
}

# Function to deploy microservices
deploy_microservices() {
    echo -e "${YELLOW}Deploying microservices (2 pods each)...${NC}"
    
    # Deploy main service
    kubectl apply -f k8s/dockerhub/main-service-deployment.yaml
    
    # Deploy customer interface
    kubectl apply -f k8s/dockerhub/customer-interface-deployment.yaml
    
    # Deploy frontend
    kubectl apply -f k8s/dockerhub/frontend-deployment.yaml
    
    echo -e "${YELLOW}Waiting for microservices to be ready...${NC}"
    kubectl wait --for=condition=available --timeout=300s deployment/courier-main-service -n $NAMESPACE
    kubectl wait --for=condition=available --timeout=300s deployment/courier-customer-interface -n $NAMESPACE
    kubectl wait --for=condition=available --timeout=300s deployment/courier-frontend -n $NAMESPACE
    
    echo -e "${GREEN}✓ Microservices deployed with 2 pods each${NC}"
}

# Function to create ingress
create_ingress() {
    echo -e "${YELLOW}Creating ingress resources...${NC}"
    kubectl apply -f k8s/dockerhub/ingress.yaml
    echo -e "${GREEN}✓ Ingress resources created${NC}"
}

# Function to verify deployment
verify_deployment() {
    echo -e "${YELLOW}Verifying deployment...${NC}"
    
    echo -e "${BLUE}Pods status:${NC}"
    kubectl get pods -n $NAMESPACE
    
    echo ""
    echo -e "${BLUE}Services status:${NC}"
    kubectl get svc -n $NAMESPACE
    
    echo ""
    echo -e "${BLUE}Ingress status:${NC}"
    kubectl get ingress -n $NAMESPACE
    
    echo ""
    echo -e "${BLUE}Pod distribution:${NC}"
    echo "Main Service pods:"
    kubectl get pods -n $NAMESPACE -l app=courier-main-service -o wide
    echo ""
    echo "Customer Interface pods:"
    kubectl get pods -n $NAMESPACE -l app=courier-customer-interface -o wide
    echo ""
    echo "Frontend pods:"
    kubectl get pods -n $NAMESPACE -l app=courier-frontend -o wide
}

# Function to run health checks
health_checks() {
    echo -e "${YELLOW}Running health checks...${NC}"
    
    # Wait for services to be ready
    sleep 30
    
    # Port forward and test services
    echo -e "${BLUE}Testing service health...${NC}"
    
    # Test main service
    kubectl port-forward svc/courier-main-service 8080:8080 -n $NAMESPACE &
    PF_PID1=$!
    sleep 5
    if curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Main Service: Healthy${NC}"
    else
        echo -e "${YELLOW}⚠ Main Service: Starting up...${NC}"
    fi
    kill $PF_PID1 2>/dev/null || true
    
    # Test customer interface
    kubectl port-forward svc/courier-customer-interface 8081:8081 -n $NAMESPACE &
    PF_PID2=$!
    sleep 5
    if curl -f http://localhost:8081/actuator/health >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Customer Interface: Healthy${NC}"
    else
        echo -e "${YELLOW}⚠ Customer Interface: Starting up...${NC}"
    fi
    kill $PF_PID2 2>/dev/null || true
    
    # Test frontend
    kubectl port-forward svc/courier-frontend 3000:3000 -n $NAMESPACE &
    PF_PID3=$!
    sleep 5
    if curl -f http://localhost:3000/health >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Frontend: Healthy${NC}"
    else
        echo -e "${YELLOW}⚠ Frontend: Starting up...${NC}"
    fi
    kill $PF_PID3 2>/dev/null || true
}

# Function to show access information
show_access_info() {
    echo ""
    echo -e "${GREEN}=== Deployment Summary ===${NC}"
    echo -e "${BLUE}Namespace:${NC} $NAMESPACE"
    echo -e "${BLUE}Docker Hub Images:${NC}"
    echo "  - $DOCKERHUB_USERNAME/courier-main-service:$IMAGE_TAG"
    echo "  - $DOCKERHUB_USERNAME/courier-customer-interface:$IMAGE_TAG"
    echo "  - $DOCKERHUB_USERNAME/courier-frontend:$IMAGE_TAG"
    echo ""
    echo -e "${GREEN}=== Pod Distribution ===${NC}"
    echo -e "${BLUE}Each microservice is running with 2 pods for high availability${NC}"
    echo ""
    echo -e "${GREEN}=== Access URLs (via port-forward) ===${NC}"
    echo -e "${BLUE}Main Service:${NC} kubectl port-forward svc/courier-main-service 8080:8080 -n $NAMESPACE"
    echo -e "${BLUE}Customer Interface:${NC} kubectl port-forward svc/courier-customer-interface 8081:8081 -n $NAMESPACE"
    echo -e "${BLUE}Frontend:${NC} kubectl port-forward svc/courier-frontend 3000:3000 -n $NAMESPACE"
    echo ""
    echo -e "${GREEN}=== Access URLs (via ingress - add to /etc/hosts) ===${NC}"
    echo -e "${BLUE}Main API:${NC} http://api.courier.local/api/v1"
    echo -e "${BLUE}Customer API:${NC} http://customer.courier.local/api/v1/customer"
    echo -e "${BLUE}Frontend:${NC} http://app.courier.local"
    echo ""
    echo -e "${GREEN}=== Useful Commands ===${NC}"
    echo -e "${BLUE}View pods:${NC} kubectl get pods -n $NAMESPACE"
    echo -e "${BLUE}View services:${NC} kubectl get svc -n $NAMESPACE"
    echo -e "${BLUE}View logs:${NC} kubectl logs -f deployment/courier-main-service -n $NAMESPACE"
    echo -e "${BLUE}Scale service:${NC} kubectl scale deployment courier-main-service --replicas=3 -n $NAMESPACE"
    echo -e "${BLUE}Delete deployment:${NC} kubectl delete namespace $NAMESPACE"
}

# Function to cleanup on error
cleanup_on_error() {
    echo -e "${RED}Deployment failed. Cleaning up...${NC}"
    # Restore backup files
    if ls k8s/dockerhub/*.bak 1> /dev/null 2>&1; then
        for file in k8s/dockerhub/*.bak; do
            mv "$file" "${file%.bak}"
        done
    fi
}

# Main execution
main() {
    trap cleanup_on_error ERR
    
    check_prerequisites
    validate_dockerhub_username
    update_image_references
    create_namespace
    create_secrets
    create_configmaps
    deploy_infrastructure
    initialize_kafka_topics
    deploy_microservices
    create_ingress
    verify_deployment
    health_checks
    show_access_info
    
    # Clean up backup files
    rm -f k8s/dockerhub/*.bak
    
    echo ""
    echo -e "${GREEN}🎉 Deployment completed successfully!${NC}"
    echo -e "${GREEN}All microservices are running with 2 pods each.${NC}"
}

# Show usage if help is requested
if [[ "$1" == "help" || "$1" == "-h" || "$1" == "--help" ]]; then
    echo "Usage: $0 <dockerhub-username> [image-tag]"
    echo ""
    echo "Parameters:"
    echo "  dockerhub-username  - Your Docker Hub username (required)"
    echo "  image-tag          - Image tag to deploy (default: latest)"
    echo ""
    echo "Examples:"
    echo "  $0 myusername latest"
    echo "  $0 myusername v1.0.0"
    echo ""
    echo "Prerequisites:"
    echo "  - kubectl configured for your cluster"
    echo "  - Docker Hub images built and pushed"
    echo "  - Nginx ingress controller (optional)"
    exit 0
fi

# Execute main function
main "$@"
