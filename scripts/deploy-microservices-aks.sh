#!/bin/bash

# Deploy Courier Management Microservices to AKS
# This script builds, pushes, and deploys all microservices

set -e

# Configuration
REGISTRY_NAME="courierregistry"
RESOURCE_GROUP="courier-rg"
AKS_CLUSTER="courier-aks"
NAMESPACE="courier-system"
IMAGE_TAG=${1:-latest}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Courier Microservices Deployment Script ===${NC}"
echo -e "${BLUE}Registry: ${REGISTRY_NAME}${NC}"
echo -e "${BLUE}Resource Group: ${RESOURCE_GROUP}${NC}"
echo -e "${BLUE}AKS Cluster: ${AKS_CLUSTER}${NC}"
echo -e "${BLUE}Namespace: ${NAMESPACE}${NC}"
echo -e "${BLUE}Image Tag: ${IMAGE_TAG}${NC}"
echo ""

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command_exists az; then
    echo -e "${RED}Error: Azure CLI is not installed${NC}"
    exit 1
fi

if ! command_exists kubectl; then
    echo -e "${RED}Error: kubectl is not installed${NC}"
    exit 1
fi

if ! command_exists docker; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}Prerequisites check passed${NC}"

# Login to Azure (if not already logged in)
echo -e "${YELLOW}Checking Azure login...${NC}"
if ! az account show >/dev/null 2>&1; then
    echo -e "${YELLOW}Please login to Azure...${NC}"
    az login
fi

# Get AKS credentials
echo -e "${YELLOW}Getting AKS credentials...${NC}"
az aks get-credentials --resource-group $RESOURCE_GROUP --name $AKS_CLUSTER --overwrite-existing

# Login to ACR
echo -e "${YELLOW}Logging into Azure Container Registry...${NC}"
az acr login --name $REGISTRY_NAME

# Build and push backend services
echo -e "${YELLOW}Building backend services Docker image...${NC}"
docker build -f Dockerfile.backend -t $REGISTRY_NAME.azurecr.io/courier-main-service:$IMAGE_TAG .
docker build -f Dockerfile.backend -t $REGISTRY_NAME.azurecr.io/courier-customer-interface:$IMAGE_TAG .

echo -e "${YELLOW}Pushing backend images to ACR...${NC}"
docker push $REGISTRY_NAME.azurecr.io/courier-main-service:$IMAGE_TAG
docker push $REGISTRY_NAME.azurecr.io/courier-customer-interface:$IMAGE_TAG

# Build and push frontend service
echo -e "${YELLOW}Building frontend Docker image...${NC}"
cd frontend
docker build -t $REGISTRY_NAME.azurecr.io/courier-frontend:$IMAGE_TAG .
cd ..

echo -e "${YELLOW}Pushing frontend image to ACR...${NC}"
docker push $REGISTRY_NAME.azurecr.io/courier-frontend:$IMAGE_TAG

# Create namespace if it doesn't exist
echo -e "${YELLOW}Creating namespace if it doesn't exist...${NC}"
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Apply ConfigMaps and Secrets
echo -e "${YELLOW}Applying ConfigMaps and Secrets...${NC}"
kubectl apply -f k8s/customer-interface-config.yaml

# Deploy infrastructure services (if not already deployed)
echo -e "${YELLOW}Deploying infrastructure services...${NC}"
if ! kubectl get deployment courier-postgres -n $NAMESPACE >/dev/null 2>&1; then
    echo "Deploying PostgreSQL..."
    kubectl apply -f k8s/postgres-deployment.yaml
fi

if ! kubectl get deployment courier-kafka -n $NAMESPACE >/dev/null 2>&1; then
    echo "Deploying Kafka..."
    kubectl apply -f k8s/kafka-deployment.yaml
fi

if ! kubectl get deployment courier-redis -n $NAMESPACE >/dev/null 2>&1; then
    echo "Deploying Redis..."
    kubectl apply -f k8s/redis-deployment.yaml
fi

# Update deployments with new images
echo -e "${YELLOW}Updating deployment images...${NC}"
kubectl set image deployment/courier-main-service main-service=$REGISTRY_NAME.azurecr.io/courier-main-service:$IMAGE_TAG -n $NAMESPACE
kubectl set image deployment/courier-customer-interface customer-interface=$REGISTRY_NAME.azurecr.io/courier-customer-interface:$IMAGE_TAG -n $NAMESPACE
kubectl set image deployment/courier-frontend frontend=$REGISTRY_NAME.azurecr.io/courier-frontend:$IMAGE_TAG -n $NAMESPACE

# Apply all Kubernetes manifests
echo -e "${YELLOW}Applying Kubernetes manifests...${NC}"
kubectl apply -f k8s/main-service-deployment.yaml
kubectl apply -f k8s/customer-interface-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml

# Apply ingress and HPA
kubectl apply -f k8s/customer-interface-ingress.yaml
kubectl apply -f k8s/customer-interface-hpa.yaml

# Wait for deployments to be ready
echo -e "${YELLOW}Waiting for deployments to be ready...${NC}"
kubectl rollout status deployment/courier-main-service -n $NAMESPACE --timeout=300s
kubectl rollout status deployment/courier-customer-interface -n $NAMESPACE --timeout=300s
kubectl rollout status deployment/courier-frontend -n $NAMESPACE --timeout=300s

# Check pod status
echo -e "${YELLOW}Checking pod status...${NC}"
kubectl get pods -n $NAMESPACE

# Get service information
echo -e "${YELLOW}Getting service information...${NC}"
kubectl get svc -n $NAMESPACE

# Get ingress information
echo -e "${YELLOW}Getting ingress information...${NC}"
kubectl get ingress -n $NAMESPACE

# Health check
echo -e "${YELLOW}Performing health checks...${NC}"
MAIN_SERVICE_IP=$(kubectl get svc courier-main-service -n $NAMESPACE -o jsonpath='{.spec.clusterIP}')
CUSTOMER_SERVICE_IP=$(kubectl get svc courier-customer-interface-service -n $NAMESPACE -o jsonpath='{.spec.clusterIP}')
FRONTEND_SERVICE_IP=$(kubectl get svc courier-frontend-service -n $NAMESPACE -o jsonpath='{.spec.clusterIP}')

echo -e "${YELLOW}Waiting for services to be ready...${NC}"
sleep 60

# Test internal connectivity
kubectl run test-pod --image=curlimages/curl --rm -i --restart=Never -n $NAMESPACE -- \
  curl -f http://courier-main-service:8080/actuator/health && echo "Main service health check passed" || echo "Main service health check failed"

kubectl run test-pod --image=curlimages/curl --rm -i --restart=Never -n $NAMESPACE -- \
  curl -f http://courier-customer-interface-service:8081/actuator/health && echo "Customer interface health check passed" || echo "Customer interface health check failed"

kubectl run test-pod --image=curlimages/curl --rm -i --restart=Never -n $NAMESPACE -- \
  curl -f http://courier-frontend-service:80/health && echo "Frontend health check passed" || echo "Frontend health check failed"

# Display useful information
echo ""
echo -e "${GREEN}=== Deployment Summary ===${NC}"
echo -e "${GREEN}Main Service: courier-main-service${NC}"
echo -e "${GREEN}Customer Interface: courier-customer-interface${NC}"
echo -e "${GREEN}Frontend: courier-frontend${NC}"
echo -e "${GREEN}Images: $REGISTRY_NAME.azurecr.io/*:$IMAGE_TAG${NC}"
echo -e "${GREEN}Namespace: $NAMESPACE${NC}"

echo ""
echo -e "${GREEN}=== Service URLs ===${NC}"
INGRESS_IP=$(kubectl get ingress courier-customer-interface-ingress -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "Pending")
if [ "$INGRESS_IP" != "Pending" ] && [ -n "$INGRESS_IP" ]; then
    echo -e "${GREEN}Frontend URL: https://customer.courier.company.com${NC}"
    echo -e "${GREEN}Customer API: https://customer.courier.company.com/api/v1/customer${NC}"
else
    echo -e "${YELLOW}Ingress IP not yet assigned. Check status manually.${NC}"
fi

echo ""
echo -e "${GREEN}=== Useful Commands ===${NC}"
echo -e "${BLUE}View all pods:${NC} kubectl get pods -n $NAMESPACE"
echo -e "${BLUE}View services:${NC} kubectl get svc -n $NAMESPACE"
echo -e "${BLUE}View ingress:${NC} kubectl get ingress -n $NAMESPACE"
echo -e "${BLUE}View logs (main):${NC} kubectl logs -f deployment/courier-main-service -n $NAMESPACE"
echo -e "${BLUE}View logs (customer):${NC} kubectl logs -f deployment/courier-customer-interface -n $NAMESPACE"
echo -e "${BLUE}View logs (frontend):${NC} kubectl logs -f deployment/courier-frontend -n $NAMESPACE"
echo -e "${BLUE}Scale deployment:${NC} kubectl scale deployment courier-main-service --replicas=5 -n $NAMESPACE"
echo -e "${BLUE}Port forward (main):${NC} kubectl port-forward svc/courier-main-service 8080:8080 -n $NAMESPACE"
echo -e "${BLUE}Port forward (customer):${NC} kubectl port-forward svc/courier-customer-interface-service 8081:8081 -n $NAMESPACE"
echo -e "${BLUE}Port forward (frontend):${NC} kubectl port-forward svc/courier-frontend-service 3000:80 -n $NAMESPACE"

echo ""
echo -e "${GREEN}Microservices deployment completed successfully!${NC}"
