#!/bin/bash

# Comprehensive deployment script for Courier Microservices using Helm
# This script integrates with GitHub Actions and supports multiple environments

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT=${1:-development}
REGISTRY="courierregistry.azurecr.io"
NAMESPACE="courier-system"
CHART_PATH="helm/courier-microservices"
RELEASE_NAME="courier-microservices"

echo -e "${BLUE}=== Courier Microservices Helm Deployment ===${NC}"
echo -e "${BLUE}Environment: $ENVIRONMENT${NC}"
echo -e "${BLUE}Registry: $REGISTRY${NC}"
echo -e "${BLUE}Namespace: $NAMESPACE${NC}"
echo ""

# Function to check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"
    
    # Check if kubectl is available and configured
    if ! command -v kubectl &> /dev/null; then
        echo -e "${RED}Error: kubectl is not installed${NC}"
        exit 1
    fi
    
    # Check if helm is available
    if ! command -v helm &> /dev/null; then
        echo -e "${RED}Error: Helm is not installed${NC}"
        exit 1
    fi
    
    # Check if yq is available for YAML processing
    if ! command -v yq &> /dev/null; then
        echo -e "${YELLOW}Warning: yq not found, installing...${NC}"
        if [[ "$OSTYPE" == "darwin"* ]]; then
            brew install yq
        elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
            sudo apt-get update && sudo apt-get install -y yq
        else
            echo -e "${RED}Error: Please install yq manually${NC}"
            exit 1
        fi
    fi
    
    # Check kubectl connectivity
    if ! kubectl cluster-info &> /dev/null; then
        echo -e "${RED}Error: Cannot connect to Kubernetes cluster${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Prerequisites check passed${NC}"
}

# Function to generate values from application.yml files
generate_values() {
    echo -e "${YELLOW}Generating Helm values from application configurations...${NC}"
    
    # Run the values generation script
    if [[ -f "scripts/generate-helm-values.sh" ]]; then
        chmod +x scripts/generate-helm-values.sh
        ./scripts/generate-helm-values.sh "$ENVIRONMENT"
    else
        echo -e "${YELLOW}Warning: generate-helm-values.sh not found, using existing values${NC}"
    fi
}

# Function to validate Helm chart
validate_chart() {
    echo -e "${YELLOW}Validating Helm chart...${NC}"
    
    # Lint the chart
    if ! helm lint "$CHART_PATH"; then
        echo -e "${RED}Error: Helm chart validation failed${NC}"
        exit 1
    fi
    
    # Template the chart to check for syntax errors
    local values_file="$CHART_PATH/values-$ENVIRONMENT.yaml"
    if [[ -f "$values_file" ]]; then
        helm template "$RELEASE_NAME" "$CHART_PATH" \
            --values "$CHART_PATH/values.yaml" \
            --values "$values_file" \
            --namespace "$NAMESPACE" > /tmp/manifests.yaml
    else
        helm template "$RELEASE_NAME" "$CHART_PATH" \
            --values "$CHART_PATH/values.yaml" \
            --namespace "$NAMESPACE" > /tmp/manifests.yaml
    fi
    
    echo -e "${GREEN}Chart validation passed${NC}"
}

# Function to create namespace and secrets
setup_namespace() {
    echo -e "${YELLOW}Setting up namespace and secrets...${NC}"
    
    # Create namespace
    kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
    
    # Check if secrets exist, create if they don't
    if ! kubectl get secret courier-database-secret -n "$NAMESPACE" &> /dev/null; then
        echo -e "${YELLOW}Creating database secret...${NC}"
        kubectl create secret generic courier-database-secret \
            --from-literal=password="${DATABASE_PASSWORD:-defaultpassword}" \
            --namespace "$NAMESPACE"
    fi
    
    if ! kubectl get secret courier-jwt-secret -n "$NAMESPACE" &> /dev/null; then
        echo -e "${YELLOW}Creating JWT secret...${NC}"
        kubectl create secret generic courier-jwt-secret \
            --from-literal=main-service-secret="${JWT_SECRET_MAIN:-defaultmainsecret}" \
            --from-literal=customer-interface-secret="${JWT_SECRET_CUSTOMER:-defaultcustomersecret}" \
            --namespace "$NAMESPACE"
    fi
    
    if ! kubectl get secret acr-secret -n "$NAMESPACE" &> /dev/null; then
        echo -e "${YELLOW}Creating ACR secret...${NC}"
        kubectl create secret docker-registry acr-secret \
            --docker-server="$REGISTRY" \
            --docker-username="${ACR_USERNAME:-defaultuser}" \
            --docker-password="${ACR_PASSWORD:-defaultpassword}" \
            --namespace "$NAMESPACE"
    fi
    
    echo -e "${GREEN}Namespace and secrets configured${NC}"
}

# Function to deploy with Helm
deploy_chart() {
    echo -e "${YELLOW}Deploying Courier Microservices with Helm...${NC}"
    
    local values_file="$CHART_PATH/values-$ENVIRONMENT.yaml"
    local helm_args=(
        "upgrade" "--install" "$RELEASE_NAME" "$CHART_PATH"
        "--namespace" "$NAMESPACE"
        "--values" "$CHART_PATH/values.yaml"
        "--timeout" "15m"
        "--wait"
        "--atomic"
    )
    
    # Add environment-specific values if they exist
    if [[ -f "$values_file" ]]; then
        helm_args+=("--values" "$values_file")
    fi
    
    # Add image tags if provided
    if [[ -n "$IMAGE_TAG" ]]; then
        helm_args+=(
            "--set" "mainService.image.tag=$IMAGE_TAG"
            "--set" "customerInterface.image.tag=$IMAGE_TAG"
            "--set" "frontend.image.tag=$IMAGE_TAG"
        )
    fi
    
    # Execute Helm deployment
    if helm "${helm_args[@]}"; then
        echo -e "${GREEN}Deployment successful${NC}"
    else
        echo -e "${RED}Deployment failed${NC}"
        exit 1
    fi
}

# Function to verify deployment
verify_deployment() {
    echo -e "${YELLOW}Verifying deployment...${NC}"
    
    # Check deployment status
    local deployments=("courier-main-service" "courier-customer-interface" "courier-frontend")
    
    for deployment in "${deployments[@]}"; do
        echo -e "${BLUE}Checking deployment: $deployment${NC}"
        
        if kubectl get deployment "$deployment" -n "$NAMESPACE" &> /dev/null; then
            kubectl rollout status deployment/"$deployment" -n "$NAMESPACE" --timeout=300s
        else
            echo -e "${YELLOW}Warning: Deployment $deployment not found (might be disabled)${NC}"
        fi
    done
    
    # Check pod status
    echo -e "${BLUE}Pod status:${NC}"
    kubectl get pods -n "$NAMESPACE"
    
    # Check services
    echo -e "${BLUE}Service status:${NC}"
    kubectl get svc -n "$NAMESPACE"
    
    # Check ingress
    echo -e "${BLUE}Ingress status:${NC}"
    kubectl get ingress -n "$NAMESPACE"
}

# Function to run health checks
health_checks() {
    echo -e "${YELLOW}Running health checks...${NC}"
    
    # Wait for services to be ready
    sleep 30
    
    # Health check for main service
    if kubectl get deployment courier-main-service -n "$NAMESPACE" &> /dev/null; then
        echo -e "${BLUE}Health check: Main Service${NC}"
        kubectl run health-check-main --image=curlimages/curl --rm -i --restart=Never -n "$NAMESPACE" -- \
            curl -f http://courier-main-service:8080/actuator/health || echo "Main service health check failed"
    fi
    
    # Health check for customer interface
    if kubectl get deployment courier-customer-interface -n "$NAMESPACE" &> /dev/null; then
        echo -e "${BLUE}Health check: Customer Interface${NC}"
        kubectl run health-check-customer --image=curlimages/curl --rm -i --restart=Never -n "$NAMESPACE" -- \
            curl -f http://courier-customer-interface:8081/actuator/health || echo "Customer interface health check failed"
    fi
    
    # Health check for frontend
    if kubectl get deployment courier-frontend -n "$NAMESPACE" &> /dev/null; then
        echo -e "${BLUE}Health check: Frontend${NC}"
        kubectl run health-check-frontend --image=curlimages/curl --rm -i --restart=Never -n "$NAMESPACE" -- \
            curl -f http://courier-frontend:3000/health || echo "Frontend health check failed"
    fi
}

# Function to display deployment summary
deployment_summary() {
    echo ""
    echo -e "${GREEN}=== Deployment Summary ===${NC}"
    echo -e "${BLUE}Environment:${NC} $ENVIRONMENT"
    echo -e "${BLUE}Namespace:${NC} $NAMESPACE"
    echo -e "${BLUE}Release:${NC} $RELEASE_NAME"
    echo -e "${BLUE}Chart Version:${NC} $(helm list -n "$NAMESPACE" -o json | jq -r ".[] | select(.name==\"$RELEASE_NAME\") | .chart")"
    
    # Get external URLs
    echo ""
    echo -e "${GREEN}=== Access URLs ===${NC}"
    
    # Get ingress hosts
    local ingresses=$(kubectl get ingress -n "$NAMESPACE" -o json | jq -r '.items[].spec.rules[].host' 2>/dev/null || echo "")
    if [[ -n "$ingresses" ]]; then
        while IFS= read -r host; do
            if [[ "$host" == api.* ]]; then
                echo -e "${BLUE}Main API:${NC} https://$host/api/v1"
            elif [[ "$host" == customer.* ]]; then
                echo -e "${BLUE}Customer API:${NC} https://$host/api/v1/customer"
            elif [[ "$host" == app.* ]]; then
                echo -e "${BLUE}Frontend:${NC} https://$host"
            fi
        done <<< "$ingresses"
    else
        echo -e "${YELLOW}No ingress configured or external IPs not yet assigned${NC}"
    fi
    
    echo ""
    echo -e "${GREEN}=== Useful Commands ===${NC}"
    echo -e "${BLUE}View pods:${NC} kubectl get pods -n $NAMESPACE"
    echo -e "${BLUE}View services:${NC} kubectl get svc -n $NAMESPACE"
    echo -e "${BLUE}View logs (main):${NC} kubectl logs -f deployment/courier-main-service -n $NAMESPACE"
    echo -e "${BLUE}View logs (customer):${NC} kubectl logs -f deployment/courier-customer-interface -n $NAMESPACE"
    echo -e "${BLUE}View logs (frontend):${NC} kubectl logs -f deployment/courier-frontend -n $NAMESPACE"
    echo -e "${BLUE}Port forward (main):${NC} kubectl port-forward svc/courier-main-service 8080:8080 -n $NAMESPACE"
    echo -e "${BLUE}Port forward (customer):${NC} kubectl port-forward svc/courier-customer-interface 8081:8081 -n $NAMESPACE"
    echo -e "${BLUE}Port forward (frontend):${NC} kubectl port-forward svc/courier-frontend 3000:3000 -n $NAMESPACE"
    echo -e "${BLUE}Helm status:${NC} helm status $RELEASE_NAME -n $NAMESPACE"
    echo -e "${BLUE}Helm history:${NC} helm history $RELEASE_NAME -n $NAMESPACE"
}

# Function to handle rollback
rollback_deployment() {
    local revision=${1:-1}
    echo -e "${YELLOW}Rolling back to revision $revision...${NC}"
    
    if helm rollback "$RELEASE_NAME" "$revision" -n "$NAMESPACE"; then
        echo -e "${GREEN}Rollback successful${NC}"
        verify_deployment
    else
        echo -e "${RED}Rollback failed${NC}"
        exit 1
    fi
}

# Main execution flow
main() {
    case "${2:-deploy}" in
        "deploy")
            check_prerequisites
            generate_values
            validate_chart
            setup_namespace
            deploy_chart
            verify_deployment
            health_checks
            deployment_summary
            ;;
        "rollback")
            check_prerequisites
            rollback_deployment "${3:-1}"
            ;;
        "validate")
            check_prerequisites
            generate_values
            validate_chart
            echo -e "${GREEN}Validation completed successfully${NC}"
            ;;
        "health")
            check_prerequisites
            health_checks
            ;;
        *)
            echo "Usage: $0 <environment> [deploy|rollback|validate|health] [revision]"
            echo "Environments: development, staging, production"
            echo "Actions:"
            echo "  deploy   - Deploy the microservices (default)"
            echo "  rollback - Rollback to previous revision"
            echo "  validate - Validate chart and configuration"
            echo "  health   - Run health checks only"
            exit 1
            ;;
    esac
}

# Execute main function with all arguments
main "$@"
