#!/bin/bash

# Deploy Customer Interface Microservice to AKS
# This script builds, pushes, and deploys the customer interface service

set -e

# Configuration
REGISTRY_NAME="courierregistry"
RESOURCE_GROUP="courier-rg"
AKS_CLUSTER="courier-aks"
NAMESPACE="courier-system"
SERVICE_NAME="courier-customer-interface"
IMAGE_TAG=${1:-latest}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Courier Customer Interface Deployment Script ===${NC}"
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

# Build the Docker image
echo -e "${YELLOW}Building Docker image...${NC}"
docker build -f Dockerfile.customer-interface -t $REGISTRY_NAME.azurecr.io/$SERVICE_NAME:$IMAGE_TAG .

# Push the image to ACR
echo -e "${YELLOW}Pushing image to ACR...${NC}"
docker push $REGISTRY_NAME.azurecr.io/$SERVICE_NAME:$IMAGE_TAG

# Create namespace if it doesn't exist
echo -e "${YELLOW}Creating namespace if it doesn't exist...${NC}"
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Apply ConfigMaps and Secrets
echo -e "${YELLOW}Applying ConfigMaps and Secrets...${NC}"
kubectl apply -f k8s/customer-interface-config.yaml

# Update deployment with new image
echo -e "${YELLOW}Updating deployment image...${NC}"
kubectl set image deployment/$SERVICE_NAME customer-interface=$REGISTRY_NAME.azurecr.io/$SERVICE_NAME:$IMAGE_TAG -n $NAMESPACE

# Apply all Kubernetes manifests
echo -e "${YELLOW}Applying Kubernetes manifests...${NC}"
kubectl apply -f k8s/customer-interface-deployment.yaml
kubectl apply -f k8s/customer-interface-ingress.yaml
kubectl apply -f k8s/customer-interface-hpa.yaml

# Wait for deployment to be ready
echo -e "${YELLOW}Waiting for deployment to be ready...${NC}"
kubectl rollout status deployment/$SERVICE_NAME -n $NAMESPACE --timeout=300s

# Check pod status
echo -e "${YELLOW}Checking pod status...${NC}"
kubectl get pods -n $NAMESPACE -l app=$SERVICE_NAME

# Get service information
echo -e "${YELLOW}Getting service information...${NC}"
kubectl get svc -n $NAMESPACE -l app=$SERVICE_NAME

# Get ingress information
echo -e "${YELLOW}Getting ingress information...${NC}"
kubectl get ingress -n $NAMESPACE -l app=$SERVICE_NAME

# Health check
echo -e "${YELLOW}Performing health check...${NC}"
SERVICE_IP=$(kubectl get svc courier-customer-interface-service -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

if [ -n "$SERVICE_IP" ]; then
    echo -e "${YELLOW}Waiting for service to be ready...${NC}"
    sleep 30
    
    if curl -f http://$SERVICE_IP/actuator/health >/dev/null 2>&1; then
        echo -e "${GREEN}Health check passed!${NC}"
    else
        echo -e "${YELLOW}Health check failed, but deployment completed. Service might still be starting up.${NC}"
    fi
else
    echo -e "${YELLOW}Service IP not yet assigned. Check status manually.${NC}"
fi

# Display useful information
echo ""
echo -e "${GREEN}=== Deployment Summary ===${NC}"
echo -e "${GREEN}Service: $SERVICE_NAME${NC}"
echo -e "${GREEN}Image: $REGISTRY_NAME.azurecr.io/$SERVICE_NAME:$IMAGE_TAG${NC}"
echo -e "${GREEN}Namespace: $NAMESPACE${NC}"
if [ -n "$SERVICE_IP" ]; then
    echo -e "${GREEN}Service IP: $SERVICE_IP${NC}"
    echo -e "${GREEN}Health Check: http://$SERVICE_IP/actuator/health${NC}"
    echo -e "${GREEN}API Base URL: http://$SERVICE_IP/api/v1/customer${NC}"
fi

echo ""
echo -e "${GREEN}=== Useful Commands ===${NC}"
echo -e "${BLUE}View logs:${NC} kubectl logs -f deployment/$SERVICE_NAME -n $NAMESPACE"
echo -e "${BLUE}View pods:${NC} kubectl get pods -n $NAMESPACE -l app=$SERVICE_NAME"
echo -e "${BLUE}Describe deployment:${NC} kubectl describe deployment $SERVICE_NAME -n $NAMESPACE"
echo -e "${BLUE}Port forward:${NC} kubectl port-forward svc/courier-customer-interface-service 8081:80 -n $NAMESPACE"
echo -e "${BLUE}Scale deployment:${NC} kubectl scale deployment $SERVICE_NAME --replicas=5 -n $NAMESPACE"

echo ""
echo -e "${GREEN}Deployment completed successfully!${NC}"
