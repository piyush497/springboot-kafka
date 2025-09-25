#!/bin/bash

# Script to generate Helm values from application.yml files
# This script reads configuration from Spring Boot application.yml files
# and generates appropriate Helm values for deployment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Helm Values Generator for Courier Microservices ===${NC}"

# Check if yq is installed
if ! command -v yq &> /dev/null; then
    echo -e "${RED}Error: yq is required but not installed. Please install yq first.${NC}"
    echo "Install with: brew install yq (macOS) or apt-get install yq (Ubuntu)"
    exit 1
fi

# Configuration
ENVIRONMENT=${1:-development}
OUTPUT_DIR="helm/courier-microservices"
VALUES_FILE="$OUTPUT_DIR/values-$ENVIRONMENT.yaml"

echo -e "${YELLOW}Environment: $ENVIRONMENT${NC}"
echo -e "${YELLOW}Output file: $VALUES_FILE${NC}"
echo ""

# Function to extract configuration from application.yml
extract_config() {
    local service_name=$1
    local config_file=$2
    local prefix=$3
    
    echo -e "${BLUE}Extracting configuration from $config_file...${NC}"
    
    if [[ ! -f "$config_file" ]]; then
        echo -e "${YELLOW}Warning: $config_file not found, using defaults${NC}"
        return
    fi
    
    # Extract server port
    local port=$(yq eval '.server.port // 8080' "$config_file")
    echo "  Port: $port"
    
    # Extract database configuration
    local db_name=$(yq eval '.spring.datasource.url | split("/") | .[-1] // "courier_db"' "$config_file")
    echo "  Database: $db_name"
    
    # Extract Kafka configuration
    local kafka_servers=$(yq eval '.spring.kafka.bootstrap-servers // "localhost:9092"' "$config_file")
    echo "  Kafka: $kafka_servers"
    
    # Extract profile-specific settings
    local profile=$(yq eval '.spring.profiles.active // "local"' "$config_file")
    echo "  Profile: $profile"
    
    # Store extracted values
    eval "${prefix}_PORT=$port"
    eval "${prefix}_DB_NAME=$db_name"
    eval "${prefix}_KAFKA_SERVERS=$kafka_servers"
    eval "${prefix}_PROFILE=$profile"
}

# Extract configurations
echo -e "${YELLOW}Reading application configurations...${NC}"

# Main service configuration
extract_config "main-service" "src/main/resources/application.yml" "MAIN"

# Customer interface configuration
extract_config "customer-interface" "customer-interface-service/src/main/resources/application.yml" "CUSTOMER"

# Frontend configuration (from package.json)
if [[ -f "frontend/package.json" ]]; then
    FRONTEND_VERSION=$(yq eval '.version // "1.0.0"' frontend/package.json)
    echo -e "${BLUE}Frontend version: $FRONTEND_VERSION${NC}"
else
    FRONTEND_VERSION="1.0.0"
fi

echo ""

# Determine environment-specific settings
case $ENVIRONMENT in
    "production")
        REPLICAS_MAIN=5
        REPLICAS_CUSTOMER=5
        REPLICAS_FRONTEND=3
        RESOURCES_MAIN_CPU_REQUEST="1000m"
        RESOURCES_MAIN_MEM_REQUEST="2Gi"
        RESOURCES_MAIN_CPU_LIMIT="2000m"
        RESOURCES_MAIN_MEM_LIMIT="4Gi"
        RESOURCES_CUSTOMER_CPU_REQUEST="500m"
        RESOURCES_CUSTOMER_MEM_REQUEST="1Gi"
        RESOURCES_CUSTOMER_CPU_LIMIT="1000m"
        RESOURCES_CUSTOMER_MEM_LIMIT="2Gi"
        INGRESS_DOMAIN="courier.company.com"
        ;;
    "staging")
        REPLICAS_MAIN=3
        REPLICAS_CUSTOMER=3
        REPLICAS_FRONTEND=2
        RESOURCES_MAIN_CPU_REQUEST="500m"
        RESOURCES_MAIN_MEM_REQUEST="1Gi"
        RESOURCES_MAIN_CPU_LIMIT="1000m"
        RESOURCES_MAIN_MEM_LIMIT="2Gi"
        RESOURCES_CUSTOMER_CPU_REQUEST="250m"
        RESOURCES_CUSTOMER_MEM_REQUEST="512Mi"
        RESOURCES_CUSTOMER_CPU_LIMIT="500m"
        RESOURCES_CUSTOMER_MEM_LIMIT="1Gi"
        INGRESS_DOMAIN="staging.courier.company.com"
        ;;
    *)
        REPLICAS_MAIN=2
        REPLICAS_CUSTOMER=2
        REPLICAS_FRONTEND=1
        RESOURCES_MAIN_CPU_REQUEST="250m"
        RESOURCES_MAIN_MEM_REQUEST="512Mi"
        RESOURCES_MAIN_CPU_LIMIT="500m"
        RESOURCES_MAIN_MEM_LIMIT="1Gi"
        RESOURCES_CUSTOMER_CPU_REQUEST="100m"
        RESOURCES_CUSTOMER_MEM_REQUEST="256Mi"
        RESOURCES_CUSTOMER_CPU_LIMIT="250m"
        RESOURCES_CUSTOMER_MEM_LIMIT="512Mi"
        INGRESS_DOMAIN="dev.courier.company.com"
        ;;
esac

echo -e "${YELLOW}Generating Helm values for $ENVIRONMENT environment...${NC}"

# Create output directory if it doesn't exist
mkdir -p "$(dirname "$VALUES_FILE")"

# Generate values file
cat > "$VALUES_FILE" << EOF
# Generated Helm values for $ENVIRONMENT environment
# Generated on: $(date)
# Source: application.yml configurations

environment: $ENVIRONMENT
namespace: courier-system

# Image configuration
global:
  imageRegistry: "courierregistry.azurecr.io"
  imagePullPolicy: Always

# Main Service Configuration (from src/main/resources/application.yml)
mainService:
  enabled: true
  name: courier-main-service
  image:
    repository: courier-main-service
    tag: latest
  replicaCount: $REPLICAS_MAIN
  port: ${MAIN_PORT:-8080}
  resources:
    requests:
      memory: "$RESOURCES_MAIN_MEM_REQUEST"
      cpu: "$RESOURCES_MAIN_CPU_REQUEST"
    limits:
      memory: "$RESOURCES_MAIN_MEM_LIMIT"
      cpu: "$RESOURCES_MAIN_CPU_LIMIT"
  env:
    SPRING_PROFILES_ACTIVE: "$ENVIRONMENT"
    LOG_LEVEL: "INFO"
  service:
    type: ClusterIP
    port: ${MAIN_PORT:-8080}
  ingress:
    enabled: true
    className: "nginx"
    annotations:
      nginx.ingress.kubernetes.io/rewrite-target: /
      nginx.ingress.kubernetes.io/ssl-redirect: "true"
    hosts:
      - host: api.$INGRESS_DOMAIN
        paths:
          - path: /api/v1
            pathType: Prefix
    tls:
      - secretName: courier-api-tls
        hosts:
          - api.$INGRESS_DOMAIN
  autoscaling:
    enabled: true
    minReplicas: $REPLICAS_MAIN
    maxReplicas: $((REPLICAS_MAIN * 2))
    targetCPUUtilizationPercentage: 70
    targetMemoryUtilizationPercentage: 80

# Customer Interface Service Configuration (from customer-interface-service/src/main/resources/application.yml)
customerInterface:
  enabled: true
  name: courier-customer-interface
  image:
    repository: courier-customer-interface
    tag: latest
  replicaCount: $REPLICAS_CUSTOMER
  port: ${CUSTOMER_PORT:-8081}
  resources:
    requests:
      memory: "$RESOURCES_CUSTOMER_MEM_REQUEST"
      cpu: "$RESOURCES_CUSTOMER_CPU_REQUEST"
    limits:
      memory: "$RESOURCES_CUSTOMER_MEM_LIMIT"
      cpu: "$RESOURCES_CUSTOMER_CPU_LIMIT"
  env:
    SPRING_PROFILES_ACTIVE: "$ENVIRONMENT"
    LOG_LEVEL: "INFO"
    MAX_PARCELS_PER_DAY: "100"
    RATE_LIMIT: "60"
  service:
    type: ClusterIP
    port: ${CUSTOMER_PORT:-8081}
  ingress:
    enabled: true
    className: "nginx"
    annotations:
      nginx.ingress.kubernetes.io/rewrite-target: /
      nginx.ingress.kubernetes.io/ssl-redirect: "true"
    hosts:
      - host: customer.$INGRESS_DOMAIN
        paths:
          - path: /api/v1/customer
            pathType: Prefix
    tls:
      - secretName: courier-customer-tls
        hosts:
          - customer.$INGRESS_DOMAIN
  autoscaling:
    enabled: true
    minReplicas: $REPLICAS_CUSTOMER
    maxReplicas: $((REPLICAS_CUSTOMER * 2))
    targetCPUUtilizationPercentage: 70
    targetMemoryUtilizationPercentage: 80

# Frontend Service Configuration
frontend:
  enabled: true
  name: courier-frontend
  image:
    repository: courier-frontend
    tag: latest
  replicaCount: $REPLICAS_FRONTEND
  port: 3000
  resources:
    requests:
      memory: "128Mi"
      cpu: "100m"
    limits:
      memory: "256Mi"
      cpu: "200m"
  env:
    REACT_APP_ENVIRONMENT: "$ENVIRONMENT"
    REACT_APP_VERSION: "$FRONTEND_VERSION"
  service:
    type: ClusterIP
    port: 3000
  ingress:
    enabled: true
    className: "nginx"
    annotations:
      nginx.ingress.kubernetes.io/rewrite-target: /
      nginx.ingress.kubernetes.io/ssl-redirect: "true"
    hosts:
      - host: app.$INGRESS_DOMAIN
        paths:
          - path: /
            pathType: Prefix
    tls:
      - secretName: courier-app-tls
        hosts:
          - app.$INGRESS_DOMAIN
  autoscaling:
    enabled: true
    minReplicas: $REPLICAS_FRONTEND
    maxReplicas: $((REPLICAS_FRONTEND + 2))
    targetCPUUtilizationPercentage: 70

# Database Configuration (extracted from application.yml)
database:
  host: "courier-postgresql"
  port: 5432
  name: "${MAIN_DB_NAME:-courier_db}"
  username: "courier_user"
  existingSecret: "courier-database-secret"
  secretKeys:
    password: "password"

# Kafka Configuration (extracted from application.yml)
messaging:
  kafka:
    enabled: true
    host: "courier-kafka"
    port: 9092
    topics:
      # Main service topics (from EventStreamService configuration)
      - name: "incoming-parcel-orders"
        partitions: 3
        replicationFactor: 1
      - name: "abc-transport-events"
        partitions: 3
        replicationFactor: 1
      - name: "abc-transport-responses"
        partitions: 3
        replicationFactor: 1
      - name: "parcel-tracking-events"
        partitions: 3
        replicationFactor: 1
      - name: "courier-internal-events"
        partitions: 3
        replicationFactor: 1
      # Customer interface topics
      - name: "customer-parcel-submissions"
        partitions: 3
        replicationFactor: 1
      - name: "parcel-status-updates"
        partitions: 3
        replicationFactor: 1
      - name: "customer-notifications"
        partitions: 3
        replicationFactor: 1

# Redis Configuration
cache:
  redis:
    enabled: true
    host: "courier-redis-master"
    port: 6379
    existingSecret: "courier-redis-secret"
    secretKeys:
      password: "password"

# Security Configuration
security:
  jwt:
    existingSecret: "courier-jwt-secret"
    secretKeys:
      mainService: "main-service-secret"
      customerInterface: "customer-interface-secret"

# Monitoring Configuration
monitoring:
  enabled: true
  prometheus:
    enabled: true
    serviceMonitor:
      enabled: true
      interval: 30s

# Network Policies
networkPolicy:
  enabled: true

# Pod Disruption Budget
podDisruptionBudget:
  enabled: true
  minAvailable: 1

# Service Account
serviceAccount:
  create: true
  name: ""

# RBAC
rbac:
  create: true

# External Dependencies
postgresql:
  enabled: $([[ "$ENVIRONMENT" == "development" ]] && echo "true" || echo "false")
  auth:
    postgresPassword: "postgres"
    username: "courier_user"
    password: "courier_pass"
    database: "${MAIN_DB_NAME:-courier_db}"

kafka:
  enabled: $([[ "$ENVIRONMENT" == "development" ]] && echo "true" || echo "false")
  replicaCount: 1
  auth:
    clientProtocol: plaintext

redis:
  enabled: $([[ "$ENVIRONMENT" == "development" ]] && echo "true" || echo "false")
  auth:
    enabled: false
EOF

echo -e "${GREEN}Helm values generated successfully: $VALUES_FILE${NC}"
echo ""

# Validate the generated values
echo -e "${YELLOW}Validating generated Helm values...${NC}"

if command -v helm &> /dev/null; then
    if helm lint "$OUTPUT_DIR" --values "$VALUES_FILE"; then
        echo -e "${GREEN}Helm chart validation passed!${NC}"
    else
        echo -e "${RED}Helm chart validation failed!${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}Helm not found, skipping validation${NC}"
fi

echo ""
echo -e "${GREEN}=== Summary ===${NC}"
echo -e "${BLUE}Environment:${NC} $ENVIRONMENT"
echo -e "${BLUE}Main Service Port:${NC} ${MAIN_PORT:-8080}"
echo -e "${BLUE}Customer Interface Port:${NC} ${CUSTOMER_PORT:-8081}"
echo -e "${BLUE}Database:${NC} ${MAIN_DB_NAME:-courier_db}"
echo -e "${BLUE}Ingress Domain:${NC} $INGRESS_DOMAIN"
echo -e "${BLUE}Replicas:${NC} Main=$REPLICAS_MAIN, Customer=$REPLICAS_CUSTOMER, Frontend=$REPLICAS_FRONTEND"
echo ""
echo -e "${GREEN}Next steps:${NC}"
echo "1. Review the generated values file: $VALUES_FILE"
echo "2. Deploy with: helm upgrade --install courier-microservices $OUTPUT_DIR --values $VALUES_FILE"
echo "3. Or use the GitHub Actions workflow for automated deployment"
