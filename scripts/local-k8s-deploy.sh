#!/bin/bash

# Local Kubernetes deployment script for testing the GitHub Actions workflow locally
# This script mimics the GitHub Actions workflow for local development and testing

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
DOCKER_HUB_USERNAME=${DOCKER_HUB_USERNAME:-"your-dockerhub-username"}
IMAGE_TAG=${IMAGE_TAG:-"latest"}
K8S_NAMESPACE="courier-system"
CLUSTER_NAME="courier-k8s-cluster"

echo -e "${BLUE}=== Local Kubernetes Deployment Script ===${NC}"
echo -e "${BLUE}Docker Hub Username: $DOCKER_HUB_USERNAME${NC}"
echo -e "${BLUE}Image Tag: $IMAGE_TAG${NC}"
echo -e "${BLUE}Namespace: $K8S_NAMESPACE${NC}"
echo ""

# Function to check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"
    
    # Check if Docker is running
    if ! docker info &> /dev/null; then
        echo -e "${RED}Error: Docker is not running${NC}"
        exit 1
    fi
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}Error: kubectl is not installed${NC}"
        echo "Please install kubectl: https://kubernetes.io/docs/tasks/tools/"
        exit 1
    fi
    
    # Check if Kind is installed
    if ! command -v kind &> /dev/null; then
        echo -e "${YELLOW}Installing Kind...${NC}"
        curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
        chmod +x ./kind
        sudo mv ./kind /usr/local/bin/kind
    fi
    
    echo -e "${GREEN}✓ Prerequisites check passed${NC}"
}

# Function to setup Kind cluster
setup_k8s_cluster() {
    echo -e "${YELLOW}Setting up Kubernetes cluster...${NC}"
    
    # Check if cluster already exists
    if kind get clusters | grep -q "$CLUSTER_NAME"; then
        echo -e "${YELLOW}Cluster $CLUSTER_NAME already exists. Deleting...${NC}"
        kind delete cluster --name $CLUSTER_NAME
    fi
    
    # Create Kind configuration
    cat <<EOF > kind-config.yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: $CLUSTER_NAME
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    protocol: TCP
  - containerPort: 443
    hostPort: 443
    protocol: TCP
  - containerPort: 30080
    hostPort: 30080
    protocol: TCP
  - containerPort: 30081
    hostPort: 30081
    protocol: TCP
  - containerPort: 30000
    hostPort: 30000
    protocol: TCP
- role: worker
- role: worker
EOF
    
    # Create cluster
    kind create cluster --config=kind-config.yaml --wait=300s
    
    # Install NGINX Ingress Controller
    echo -e "${YELLOW}Installing NGINX Ingress Controller...${NC}"
    kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
    kubectl wait --namespace ingress-nginx \
      --for=condition=ready pod \
      --selector=app.kubernetes.io/component=controller \
      --timeout=300s
    
    echo -e "${GREEN}✓ Kubernetes cluster setup completed${NC}"
    kubectl cluster-info
    kubectl get nodes
}

# Function to build and push images
build_and_push_images() {
    echo -e "${YELLOW}Building and pushing Docker images...${NC}"
    
    if [[ "$DOCKER_HUB_USERNAME" == "your-dockerhub-username" ]]; then
        echo -e "${YELLOW}Warning: Please set DOCKER_HUB_USERNAME environment variable${NC}"
        read -p "Enter your Docker Hub username: " DOCKER_HUB_USERNAME
    fi
    
    # Login to Docker Hub
    echo -e "${YELLOW}Logging in to Docker Hub...${NC}"
    docker login
    
    # Build Main Service
    echo -e "${YELLOW}Building Main Service...${NC}"
    ./gradlew clean build -x test
    docker build -f Dockerfile.backend -t $DOCKER_HUB_USERNAME/courier-main-service:$IMAGE_TAG .
    docker push $DOCKER_HUB_USERNAME/courier-main-service:$IMAGE_TAG
    
    # Build Customer Interface Service
    echo -e "${YELLOW}Building Customer Interface Service...${NC}"
    cd customer-interface-service
    ./gradlew clean build -x test
    cd ..
    docker build -f customer-interface-service/Dockerfile.customer-interface -t $DOCKER_HUB_USERNAME/courier-customer-interface:$IMAGE_TAG ./customer-interface-service
    docker push $DOCKER_HUB_USERNAME/courier-customer-interface:$IMAGE_TAG
    
    # Build Frontend
    echo -e "${YELLOW}Building Frontend...${NC}"
    cd frontend
    npm ci
    npm run build
    cd ..
    docker build -f frontend/Dockerfile.frontend -t $DOCKER_HUB_USERNAME/courier-frontend:$IMAGE_TAG ./frontend
    docker push $DOCKER_HUB_USERNAME/courier-frontend:$IMAGE_TAG
    
    echo -e "${GREEN}✓ All images built and pushed successfully${NC}"
}

# Function to deploy infrastructure
deploy_infrastructure() {
    echo -e "${YELLOW}Deploying infrastructure services...${NC}"
    
    # Create namespace
    kubectl apply -f k8s/dockerhub/namespace.yaml
    
    # Create Docker Hub secret
    kubectl create secret docker-registry dockerhub-secret \
      --docker-server=docker.io \
      --docker-username=$DOCKER_HUB_USERNAME \
      --docker-password=$DOCKER_HUB_TOKEN \
      --namespace=$K8S_NAMESPACE \
      --dry-run=client -o yaml | kubectl apply -f -
    
    # Deploy ConfigMaps and Secrets
    kubectl apply -f k8s/dockerhub/configmap.yaml
    kubectl apply -f k8s/dockerhub/secrets.yaml
    
    # Deploy Infrastructure Services
    kubectl apply -f k8s/dockerhub/infrastructure-deployments.yaml
    
    # Wait for infrastructure to be ready
    echo -e "${YELLOW}Waiting for infrastructure services...${NC}"
    kubectl wait --for=condition=available --timeout=300s \
      deployment/courier-postgresql -n $K8S_NAMESPACE
    kubectl wait --for=condition=available --timeout=300s \
      deployment/courier-zookeeper -n $K8S_NAMESPACE
    kubectl wait --for=condition=available --timeout=300s \
      deployment/courier-kafka -n $K8S_NAMESPACE
    kubectl wait --for=condition=available --timeout=300s \
      deployment/courier-redis -n $K8S_NAMESPACE
    
    # Initialize Kafka topics
    kubectl apply -f k8s/dockerhub/kafka-topic-init.yaml
    kubectl wait --for=condition=complete --timeout=300s \
      job/kafka-topic-init -n $K8S_NAMESPACE
    
    echo -e "${GREEN}✓ Infrastructure deployment completed${NC}"
}

# Function to deploy microservices
deploy_microservices() {
    echo -e "${YELLOW}Deploying microservices...${NC}"
    
    # Update image references
    sed -i.bak "s/YOUR_DOCKERHUB_USERNAME/$DOCKER_HUB_USERNAME/g" k8s/dockerhub/*-deployment.yaml
    sed -i.bak "s/:latest/:$IMAGE_TAG/g" k8s/dockerhub/*-deployment.yaml
    
    # Deploy Main Service
    kubectl apply -f k8s/dockerhub/main-service-deployment.yaml
    kubectl wait --for=condition=available --timeout=300s \
      deployment/courier-main-service -n $K8S_NAMESPACE
    
    # Deploy Customer Interface Service
    kubectl apply -f k8s/dockerhub/customer-interface-deployment.yaml
    kubectl wait --for=condition=available --timeout=300s \
      deployment/courier-customer-interface -n $K8S_NAMESPACE
    
    # Deploy Frontend Service
    kubectl apply -f k8s/dockerhub/frontend-deployment.yaml
    kubectl wait --for=condition=available --timeout=300s \
      deployment/courier-frontend -n $K8S_NAMESPACE
    
    # Deploy Ingress
    kubectl apply -f k8s/dockerhub/ingress.yaml
    
    echo -e "${GREEN}✓ Microservices deployment completed${NC}"
    
    # Restore backup files
    if ls k8s/dockerhub/*.bak 1> /dev/null 2>&1; then
        for file in k8s/dockerhub/*.bak; do
            mv "$file" "${file%.bak}"
        done
    fi
}

# Function to verify deployment
verify_deployment() {
    echo -e "${YELLOW}Verifying deployment...${NC}"
    
    echo -e "${BLUE}Deployment Status:${NC}"
    kubectl get pods -n $K8S_NAMESPACE
    kubectl get services -n $K8S_NAMESPACE
    kubectl get ingress -n $K8S_NAMESPACE
    
    echo -e "${BLUE}Pod Distribution:${NC}"
    echo "Main Service pods:"
    kubectl get pods -n $K8S_NAMESPACE -l app=courier-main-service -o wide
    echo "Customer Interface pods:"
    kubectl get pods -n $K8S_NAMESPACE -l app=courier-customer-interface -o wide
    echo "Frontend pods:"
    kubectl get pods -n $K8S_NAMESPACE -l app=courier-frontend -o wide
    
    # Health checks
    echo -e "${YELLOW}Running health checks...${NC}"
    sleep 60
    
    # Test main service
    kubectl port-forward svc/courier-main-service 8080:8080 -n $K8S_NAMESPACE &
    PF_PID1=$!
    sleep 10
    
    if curl -f http://localhost:8080/actuator/health --max-time 30 >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Main Service: Healthy${NC}"
    else
        echo -e "${YELLOW}⚠ Main Service: Starting up...${NC}"
    fi
    kill $PF_PID1 2>/dev/null || true
    
    # Test customer interface
    kubectl port-forward svc/courier-customer-interface 8081:8081 -n $K8S_NAMESPACE &
    PF_PID2=$!
    sleep 10
    
    if curl -f http://localhost:8081/actuator/health --max-time 30 >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Customer Interface: Healthy${NC}"
    else
        echo -e "${YELLOW}⚠ Customer Interface: Starting up...${NC}"
    fi
    kill $PF_PID2 2>/dev/null || true
    
    # Test frontend
    kubectl port-forward svc/courier-frontend 3000:3000 -n $K8S_NAMESPACE &
    PF_PID3=$!
    sleep 10
    
    if curl -f http://localhost:3000/health --max-time 30 >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Frontend: Healthy${NC}"
    else
        echo -e "${YELLOW}⚠ Frontend: Starting up...${NC}"
    fi
    kill $PF_PID3 2>/dev/null || true
}

# Function to show access information
show_access_info() {
    echo ""
    echo -e "${GREEN}=== Deployment Completed Successfully! ===${NC}"
    echo -e "${BLUE}Access Information:${NC}"
    echo ""
    echo -e "${BLUE}Port Forward Commands:${NC}"
    echo "Main Service:      kubectl port-forward svc/courier-main-service 8080:8080 -n $K8S_NAMESPACE"
    echo "Customer Interface: kubectl port-forward svc/courier-customer-interface 8081:8081 -n $K8S_NAMESPACE"
    echo "Frontend:          kubectl port-forward svc/courier-frontend 3000:3000 -n $K8S_NAMESPACE"
    echo ""
    echo -e "${BLUE}Access URLs (after port forwarding):${NC}"
    echo "Main API:          http://localhost:8080/api/v1"
    echo "Customer API:      http://localhost:8081/api/v1/customer"
    echo "Frontend:          http://localhost:3000"
    echo ""
    echo -e "${BLUE}Useful Commands:${NC}"
    echo "View pods:         kubectl get pods -n $K8S_NAMESPACE"
    echo "View services:     kubectl get svc -n $K8S_NAMESPACE"
    echo "View logs:         kubectl logs -f deployment/courier-main-service -n $K8S_NAMESPACE"
    echo "Delete cluster:    kind delete cluster --name $CLUSTER_NAME"
    echo ""
    echo -e "${GREEN}Happy testing! 🚀${NC}"
}

# Function to cleanup
cleanup() {
    echo -e "${YELLOW}Cleaning up...${NC}"
    kind delete cluster --name $CLUSTER_NAME || true
    rm -f kind-config.yaml
}

# Main execution
main() {
    case "${1:-deploy}" in
        "deploy")
            check_prerequisites
            setup_k8s_cluster
            build_and_push_images
            deploy_infrastructure
            deploy_microservices
            verify_deployment
            show_access_info
            ;;
        "cleanup")
            cleanup
            ;;
        "verify")
            verify_deployment
            ;;
        "help"|"-h"|"--help")
            echo "Usage: $0 [command]"
            echo ""
            echo "Commands:"
            echo "  deploy  - Full deployment (default)"
            echo "  cleanup - Delete Kind cluster"
            echo "  verify  - Verify existing deployment"
            echo "  help    - Show this help message"
            echo ""
            echo "Environment Variables:"
            echo "  DOCKER_HUB_USERNAME - Your Docker Hub username (required)"
            echo "  DOCKER_HUB_TOKEN    - Your Docker Hub token (optional)"
            echo "  IMAGE_TAG           - Docker image tag (default: latest)"
            ;;
        *)
            echo -e "${RED}Unknown command: $1${NC}"
            echo "Use '$0 help' for usage information"
            exit 1
            ;;
    esac
}

# Handle script interruption
trap cleanup EXIT

# Execute main function
main "$@"
