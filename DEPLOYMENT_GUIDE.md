# Courier Management System - Deployment Guide

This guide provides comprehensive instructions for deploying the Courier Management System microservices to Kubernetes using Helm charts and GitHub Actions.

## Overview

The deployment architecture consists of:

### 🏗️ **Microservices Architecture**
- **Main Service** (Port 8080): Core business logic, authentication, EDI processing
- **Customer Interface Service** (Port 8081): Customer-specific operations and API
- **Frontend Service** (Port 3000): React-based web application
- **Infrastructure**: PostgreSQL, Kafka, Redis

### 📁 **Helm Templates Structure**
```
helm/courier-microservices/
├── Chart.yaml                           # Chart metadata
├── values.yaml                          # Default values
├── values-development.yaml              # Development environment
├── values-production.yaml               # Production environment
├── README.md                           # Chart documentation
└── templates/
    ├── _helpers.tpl                    # Template helpers
    ├── configmap.yaml                  # Configuration maps
    ├── serviceaccount.yaml             # RBAC configuration
    ├── ingress.yaml                    # External access
    ├── hpa.yaml                        # Auto-scaling
    ├── main-service/
    │   ├── deployment.yaml             # Main service deployment
    │   └── service.yaml                # Main service networking
    ├── customer-interface/
    │   ├── deployment.yaml             # Customer interface deployment
    │   └── service.yaml                # Customer interface networking
    ├── frontend/
    │   ├── deployment.yaml             # Frontend deployment
    │   └── service.yaml                # Frontend networking
    └── monitoring/
        └── servicemonitor.yaml         # Prometheus monitoring
```

## 🚀 **Deployment Methods**

### **Method 1: GitHub Actions (Recommended)**

#### **Automatic Deployment**
```yaml
# Triggered on push to main/develop branches
on:
  push:
    branches: [ main, develop ]
    paths:
      - 'src/**'
      - 'customer-interface-service/**'
      - 'frontend/**'
      - 'helm/**'
```

#### **Manual Deployment**
```bash
# Go to GitHub Actions tab
# Select "Deploy Courier Microservices to Kubernetes"
# Click "Run workflow"
# Choose environment: development/staging/production
```

### **Method 2: Local Helm Deployment**

#### **Prerequisites**
```bash
# Install required tools
brew install helm kubectl yq  # macOS
# or
apt-get install helm kubectl yq  # Ubuntu

# Configure kubectl for your cluster
az aks get-credentials --resource-group courier-rg --name courier-aks
```

#### **Quick Deployment**
```bash
# Generate values from application.yml files
./scripts/generate-helm-values.sh development

# Deploy with Helm
./scripts/deploy-with-helm.sh development deploy
```

#### **Manual Helm Commands**
```bash
# Create namespace
kubectl create namespace courier-system

# Create secrets (see secrets section below)

# Deploy with Helm
helm upgrade --install courier-microservices helm/courier-microservices \
  --values helm/courier-microservices/values-development.yaml \
  --namespace courier-system \
  --wait --timeout 15m
```

## 🔐 **Secrets Management**

### **Required Secrets**

#### **Database Secret**
```bash
kubectl create secret generic courier-database-secret \
  --from-literal=password="your-secure-db-password" \
  --namespace courier-system
```

#### **JWT Secrets**
```bash
kubectl create secret generic courier-jwt-secret \
  --from-literal=main-service-secret="your-main-service-jwt-secret-key" \
  --from-literal=customer-interface-secret="your-customer-interface-jwt-secret-key" \
  --namespace courier-system
```

#### **Container Registry Secret**
```bash
kubectl create secret docker-registry acr-secret \
  --docker-server=courierregistry.azurecr.io \
  --docker-username="your-acr-username" \
  --docker-password="your-acr-password" \
  --namespace courier-system
```

#### **Azure Event Hub Secret (Production)**
```bash
kubectl create secret generic courier-eventhub-secret \
  --from-literal=connection-string="Endpoint=sb://your-eventhub.servicebus.windows.net/..." \
  --from-literal=namespace="your-eventhub-namespace" \
  --namespace courier-system
```

### **GitHub Secrets Configuration**

Set these secrets in your GitHub repository:

```bash
# Container Registry
ACR_USERNAME=your-acr-username
ACR_PASSWORD=your-acr-password

# Database
DATABASE_PASSWORD=your-secure-db-password

# JWT Secrets
JWT_SECRET_MAIN=your-main-service-jwt-secret-key
JWT_SECRET_CUSTOMER=your-customer-interface-jwt-secret-key

# Azure Credentials (for AKS deployment)
AZURE_CREDENTIALS='{
  "clientId": "your-client-id",
  "clientSecret": "your-client-secret",
  "subscriptionId": "your-subscription-id",
  "tenantId": "your-tenant-id"
}'

# Slack Notifications (optional)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
```

## 🌍 **Environment Configuration**

### **Development Environment**
- **Purpose**: Local development and testing
- **Resources**: Minimal (1 replica per service)
- **Dependencies**: Embedded PostgreSQL, Kafka, Redis
- **Domain**: `*.dev.courier.company.com`

```bash
# Deploy development
./scripts/deploy-with-helm.sh development deploy
```

### **Staging Environment**
- **Purpose**: Pre-production testing
- **Resources**: Medium (3 replicas per service)
- **Dependencies**: External managed services
- **Domain**: `*.staging.courier.company.com`

```bash
# Deploy staging
./scripts/deploy-with-helm.sh staging deploy
```

### **Production Environment**
- **Purpose**: Live production system
- **Resources**: High (5+ replicas per service)
- **Dependencies**: Azure managed services
- **Domain**: `*.courier.company.com`

```bash
# Deploy production
./scripts/deploy-with-helm.sh production deploy
```

## 📊 **Configuration from Application.yml**

The deployment system automatically reads configuration from Spring Boot application.yml files:

### **Main Service Configuration**
```yaml
# From: src/main/resources/application.yml
server:
  port: 8080  # → mainService.port in Helm values

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/courier_db  # → database.name
  kafka:
    bootstrap-servers: localhost:9092  # → messaging.kafka configuration
```

### **Customer Interface Configuration**
```yaml
# From: customer-interface-service/src/main/resources/application.yml
server:
  port: 8081  # → customerInterface.port in Helm values

customer-interface:
  limits:
    max-parcels-per-day: 100  # → customerInterface.env.MAX_PARCELS_PER_DAY
    rate-limit-requests-per-minute: 60  # → customerInterface.env.RATE_LIMIT
```

### **Event Topics Configuration**
Based on the implemented eventing platform, the following Kafka topics are configured:

```yaml
# Main service topics (from EventStreamService)
messaging:
  kafka:
    topics:
      - name: "incoming-parcel-orders"        # EDI order submissions
      - name: "abc-transport-events"          # External transport events
      - name: "abc-transport-responses"       # Transport system responses
      - name: "parcel-tracking-events"        # Internal tracking events
      - name: "courier-internal-events"       # Internal system events
      
      # Customer interface topics
      - name: "customer-parcel-submissions"   # Customer-initiated events
      - name: "parcel-status-updates"         # Status change notifications
      - name: "customer-notifications"        # Customer notifications
```

## 🔄 **CI/CD Pipeline**

### **Pipeline Stages**

1. **Change Detection**: Detects changes in specific paths
2. **Build & Test**: Builds and tests each microservice
3. **Image Build**: Creates Docker images and pushes to ACR
4. **Manifest Generation**: Creates Helm values from application.yml
5. **Deployment**: Deploys to Kubernetes using Helm
6. **Verification**: Runs health checks and validates deployment

### **Pipeline Triggers**

```yaml
# Automatic triggers
on:
  push:
    branches: [ main, develop ]
    paths:
      - 'src/**'                    # Main service changes
      - 'customer-interface-service/**'  # Customer interface changes
      - 'frontend/**'               # Frontend changes
      - 'helm/**'                   # Helm chart changes

# Manual trigger
workflow_dispatch:
  inputs:
    environment: [development, staging, production]
    force_deploy: boolean
```

### **Build Matrix**

The pipeline builds services in parallel:

```yaml
jobs:
  build-main-service:      # Spring Boot main service
  build-customer-interface: # Spring Boot customer interface
  build-frontend:          # React frontend
  generate-manifests:      # Helm chart preparation
  deploy-to-kubernetes:    # AKS deployment
```

## 🔍 **Monitoring & Observability**

### **Health Checks**

Each service provides health endpoints:

```bash
# Main service
curl http://courier-main-service:8080/actuator/health

# Customer interface
curl http://courier-customer-interface:8081/actuator/health

# Frontend
curl http://courier-frontend:3000/health
```

### **Prometheus Metrics**

Services expose metrics at:
- Main Service: `/actuator/prometheus`
- Customer Interface: `/actuator/prometheus`
- Frontend: `/health` (basic metrics)

### **Kubernetes Probes**

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 90
  periodSeconds: 30

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10

startupProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  failureThreshold: 30
```

## 🔧 **Troubleshooting**

### **Common Issues**

#### **1. Pod Startup Failures**
```bash
# Check pod status
kubectl get pods -n courier-system

# Describe pod for events
kubectl describe pod <pod-name> -n courier-system

# Check logs
kubectl logs <pod-name> -n courier-system
```

#### **2. Service Connectivity Issues**
```bash
# Check services
kubectl get svc -n courier-system

# Check endpoints
kubectl get endpoints -n courier-system

# Test connectivity
kubectl run debug --image=curlimages/curl --rm -i --restart=Never -n courier-system -- \
  curl -f http://courier-main-service:8080/actuator/health
```

#### **3. Database Connection Issues**
```bash
# Check database secret
kubectl get secret courier-database-secret -n courier-system -o yaml

# Check database connectivity
kubectl run db-test --image=postgres:15-alpine --rm -i --restart=Never -n courier-system -- \
  psql -h courier-postgresql -U courier_user -d courier_db -c "SELECT 1"
```

#### **4. Kafka Connectivity Issues**
```bash
# Check Kafka topics
kubectl exec -it deployment/courier-kafka -n courier-system -- \
  kafka-topics --bootstrap-server localhost:9092 --list

# Check consumer groups
kubectl exec -it deployment/courier-kafka -n courier-system -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

### **Debugging Commands**

```bash
# View all resources
kubectl get all -n courier-system

# Check Helm release status
helm status courier-microservices -n courier-system

# View Helm history
helm history courier-microservices -n courier-system

# Get deployment logs
kubectl logs -f deployment/courier-main-service -n courier-system

# Port forward for local access
kubectl port-forward svc/courier-main-service 8080:8080 -n courier-system
```

## 🔄 **Rollback Procedures**

### **Automatic Rollback**
The Helm deployment uses `--atomic` flag, which automatically rolls back on failure.

### **Manual Rollback**
```bash
# View release history
helm history courier-microservices -n courier-system

# Rollback to previous version
helm rollback courier-microservices 1 -n courier-system

# Or use the script
./scripts/deploy-with-helm.sh production rollback 1
```

### **Emergency Rollback**
```bash
# Quick rollback to last known good state
kubectl rollout undo deployment/courier-main-service -n courier-system
kubectl rollout undo deployment/courier-customer-interface -n courier-system
kubectl rollout undo deployment/courier-frontend -n courier-system
```

## 📈 **Scaling Operations**

### **Manual Scaling**
```bash
# Scale specific service
kubectl scale deployment courier-main-service --replicas=10 -n courier-system

# Scale via Helm
helm upgrade courier-microservices helm/courier-microservices \
  --set mainService.replicaCount=10 \
  --namespace courier-system
```

### **Auto-scaling**
HPA is configured for automatic scaling based on:
- CPU utilization (70% target)
- Memory utilization (80% target)

```bash
# Check HPA status
kubectl get hpa -n courier-system

# View HPA details
kubectl describe hpa courier-main-service-hpa -n courier-system
```

## 🔒 **Security Considerations**

### **Network Policies**
```yaml
networkPolicy:
  enabled: true  # Restricts pod-to-pod communication
```

### **Pod Security**
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  readOnlyRootFilesystem: true
  allowPrivilegeEscalation: false
```

### **RBAC**
```yaml
rbac:
  create: true  # Creates minimal required permissions
```

## 📞 **Support & Maintenance**

### **Regular Maintenance Tasks**

1. **Weekly**: Review logs and metrics
2. **Monthly**: Update dependencies and security patches
3. **Quarterly**: Performance review and optimization

### **Contact Information**

- **Development Team**: courier-dev@company.com
- **Operations Team**: courier-ops@company.com
- **Emergency**: Use Slack channel #courier-alerts

### **Documentation Links**

- **API Documentation**: https://api.courier.company.com/docs
- **Monitoring Dashboard**: https://grafana.courier.company.com
- **Internal Wiki**: https://wiki.courier.company.com/courier-system

This deployment guide ensures reliable, scalable, and maintainable deployment of the Courier Management System microservices architecture.
