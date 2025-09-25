# Courier Microservices Helm Chart

This Helm chart deploys the complete Courier Management System microservices architecture to Kubernetes.

## Overview

The chart deploys the following microservices:
- **Main Service**: Core business logic, authentication, and admin operations
- **Customer Interface Service**: Customer-specific operations and API
- **Frontend Service**: React-based web application
- **Infrastructure**: PostgreSQL, Kafka, Redis (optional)

## Prerequisites

- Kubernetes 1.20+
- Helm 3.8+
- Azure Container Registry access (for images)
- Ingress controller (nginx recommended)
- Cert-manager (for TLS certificates)

## Installation

### Quick Start

```bash
# Add the chart repository (if published)
helm repo add courier-microservices https://charts.courier.company.com
helm repo update

# Install with default values
helm install courier-microservices courier-microservices/courier-microservices \
  --namespace courier-system \
  --create-namespace
```

### Local Development

```bash
# Generate values from application.yml files
./scripts/generate-helm-values.sh development

# Install from local chart
helm install courier-microservices ./helm/courier-microservices \
  --values ./helm/courier-microservices/values-development.yaml \
  --namespace courier-system \
  --create-namespace
```

### Production Deployment

```bash
# Generate production values
./scripts/generate-helm-values.sh production

# Install with production configuration
helm install courier-microservices ./helm/courier-microservices \
  --values ./helm/courier-microservices/values-production.yaml \
  --namespace courier-system \
  --create-namespace
```

## Configuration

### Environment-Specific Values

The chart supports multiple environments through values files:

- `values.yaml` - Default values
- `values-development.yaml` - Development environment
- `values-staging.yaml` - Staging environment  
- `values-production.yaml` - Production environment

### Key Configuration Sections

#### Global Settings

```yaml
global:
  imageRegistry: "courierregistry.azurecr.io"
  imagePullPolicy: Always

environment: development
namespace: courier-system
```

#### Main Service Configuration

```yaml
mainService:
  enabled: true
  image:
    repository: courier-main-service
    tag: latest
  replicaCount: 3
  port: 8080
  resources:
    requests:
      memory: "1Gi"
      cpu: "500m"
    limits:
      memory: "2Gi"
      cpu: "1000m"
```

#### Customer Interface Configuration

```yaml
customerInterface:
  enabled: true
  image:
    repository: courier-customer-interface
    tag: latest
  replicaCount: 3
  port: 8081
  env:
    MAX_PARCELS_PER_DAY: "100"
    RATE_LIMIT: "60"
```

#### Frontend Configuration

```yaml
frontend:
  enabled: true
  image:
    repository: courier-frontend
    tag: latest
  replicaCount: 2
  port: 3000
```

### Database Configuration

```yaml
database:
  host: "courier-postgresql"
  port: 5432
  name: "courier_db"
  username: "courier_user"
  existingSecret: "courier-database-secret"
```

### Messaging Configuration

Based on the implemented eventing platform, the chart configures Kafka topics for:

```yaml
messaging:
  kafka:
    enabled: true
    host: "courier-kafka"
    port: 9092
    topics:
      # Main service topics (from EventStreamService)
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
```

## Secrets Management

Create required secrets before deployment:

```bash
# Database secret
kubectl create secret generic courier-database-secret \
  --from-literal=password="your-db-password" \
  --namespace courier-system

# JWT secrets
kubectl create secret generic courier-jwt-secret \
  --from-literal=main-service-secret="your-main-jwt-secret" \
  --from-literal=customer-interface-secret="your-customer-jwt-secret" \
  --namespace courier-system

# Container registry secret
kubectl create secret docker-registry acr-secret \
  --docker-server=courierregistry.azurecr.io \
  --docker-username="your-acr-username" \
  --docker-password="your-acr-password" \
  --namespace courier-system
```

## Ingress Configuration

The chart creates ingress resources for external access:

- **Main API**: `api.courier.company.com/api/v1`
- **Customer API**: `customer.courier.company.com/api/v1/customer`
- **Frontend**: `app.courier.company.com`

### TLS Configuration

```yaml
ingress:
  enabled: true
  className: "nginx"
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
  hosts:
    - host: api.courier.company.com
      paths:
        - path: /api/v1
          pathType: Prefix
  tls:
    - secretName: courier-api-tls
      hosts:
        - api.courier.company.com
```

## Monitoring

The chart includes monitoring configuration for:

- **Prometheus**: Metrics collection from all services
- **Service Monitors**: Automatic service discovery
- **Health Checks**: Kubernetes probes for all services

```yaml
monitoring:
  enabled: true
  prometheus:
    enabled: true
    serviceMonitor:
      enabled: true
      interval: 30s
```

## Autoscaling

Horizontal Pod Autoscaler (HPA) is configured for all services:

```yaml
autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80
```

## Network Policies

Network policies provide security isolation:

```yaml
networkPolicy:
  enabled: true
  ingress:
    enabled: true
  egress:
    enabled: true
```

## Upgrading

```bash
# Upgrade with new values
helm upgrade courier-microservices ./helm/courier-microservices \
  --values ./helm/courier-microservices/values-production.yaml \
  --namespace courier-system

# Rollback if needed
helm rollback courier-microservices 1 --namespace courier-system
```

## Uninstalling

```bash
helm uninstall courier-microservices --namespace courier-system
```

## Troubleshooting

### Common Issues

1. **Pod startup failures**
   ```bash
   kubectl describe pod <pod-name> -n courier-system
   kubectl logs <pod-name> -n courier-system
   ```

2. **Service connectivity issues**
   ```bash
   kubectl get svc -n courier-system
   kubectl get endpoints -n courier-system
   ```

3. **Ingress issues**
   ```bash
   kubectl get ingress -n courier-system
   kubectl describe ingress <ingress-name> -n courier-system
   ```

### Health Checks

```bash
# Check service health
kubectl run health-check --image=curlimages/curl --rm -i --restart=Never -n courier-system -- \
  curl -f http://courier-main-service:8080/actuator/health

kubectl run health-check --image=curlimages/curl --rm -i --restart=Never -n courier-system -- \
  curl -f http://courier-customer-interface:8081/actuator/health

kubectl run health-check --image=curlimages/curl --rm -i --restart=Never -n courier-system -- \
  curl -f http://courier-frontend:3000/health
```

### Scaling

```bash
# Manual scaling
kubectl scale deployment courier-main-service --replicas=5 -n courier-system

# Check HPA status
kubectl get hpa -n courier-system
```

## Values Reference

| Parameter | Description | Default |
|-----------|-------------|---------|
| `global.imageRegistry` | Container registry URL | `courierregistry.azurecr.io` |
| `environment` | Deployment environment | `development` |
| `mainService.enabled` | Enable main service | `true` |
| `mainService.replicaCount` | Number of replicas | `3` |
| `customerInterface.enabled` | Enable customer interface | `true` |
| `customerInterface.replicaCount` | Number of replicas | `3` |
| `frontend.enabled` | Enable frontend | `true` |
| `frontend.replicaCount` | Number of replicas | `2` |
| `postgresql.enabled` | Enable embedded PostgreSQL | `true` |
| `kafka.enabled` | Enable embedded Kafka | `true` |
| `redis.enabled` | Enable embedded Redis | `true` |
| `monitoring.enabled` | Enable monitoring | `true` |
| `networkPolicy.enabled` | Enable network policies | `true` |

## Contributing

1. Make changes to the chart
2. Update version in `Chart.yaml`
3. Test with `helm lint` and `helm template`
4. Submit pull request

## Support

For issues and questions:
- **Documentation**: [Internal Wiki](https://wiki.courier.company.com)
- **Support**: courier-ops@company.com
- **Development**: courier-dev@company.com
