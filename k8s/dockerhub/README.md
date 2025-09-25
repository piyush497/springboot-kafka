# Kubernetes Deployment with Docker Hub Images

This directory contains Kubernetes manifests to deploy the Courier Management System microservices using Docker Hub images with **2 pods each** for high availability.

## Overview

The deployment includes:
- **3 Microservices** (2 pods each):
  - Main Service (Spring Boot) - Core business logic
  - Customer Interface (Spring Boot) - Customer operations  
  - Frontend (React) - Web application
- **Infrastructure Services**:
  - PostgreSQL - Database
  - Apache Kafka - Event streaming
  - Zookeeper - Kafka coordination
  - Redis - Caching

## Quick Start

### 1. Automated Deployment

```bash
# Make the script executable
chmod +x k8s/dockerhub/deploy.sh

# Deploy with your Docker Hub username
./k8s/dockerhub/deploy.sh your-dockerhub-username latest
```

### 2. Manual Deployment

```bash
# 1. Update Docker Hub username in deployment files
sed -i 's/YOUR_DOCKERHUB_USERNAME/your-dockerhub-username/g' k8s/dockerhub/*-deployment.yaml

# 2. Create namespace
kubectl apply -f k8s/dockerhub/namespace.yaml

# 3. Create secrets (you'll be prompted for Docker Hub credentials)
kubectl create secret docker-registry dockerhub-secret \
  --docker-server=docker.io \
  --docker-username=your-username \
  --docker-password=your-password \
  --namespace=courier-system

kubectl apply -f k8s/dockerhub/secrets.yaml

# 4. Create config maps
kubectl apply -f k8s/dockerhub/configmap.yaml

# 5. Deploy infrastructure
kubectl apply -f k8s/dockerhub/infrastructure-deployments.yaml

# 6. Wait for infrastructure to be ready
kubectl wait --for=condition=available --timeout=300s deployment/courier-postgresql -n courier-system
kubectl wait --for=condition=available --timeout=300s deployment/courier-kafka -n courier-system

# 7. Initialize Kafka topics
kubectl apply -f k8s/dockerhub/kafka-topic-init.yaml

# 8. Deploy microservices (2 pods each)
kubectl apply -f k8s/dockerhub/main-service-deployment.yaml
kubectl apply -f k8s/dockerhub/customer-interface-deployment.yaml
kubectl apply -f k8s/dockerhub/frontend-deployment.yaml

# 9. Create ingress
kubectl apply -f k8s/dockerhub/ingress.yaml
```

## File Structure

```
k8s/dockerhub/
├── namespace.yaml                    # Namespace definition
├── configmap.yaml                    # Configuration data
├── secrets.yaml                      # Secrets (passwords, JWT tokens)
├── main-service-deployment.yaml      # Main service (2 pods)
├── customer-interface-deployment.yaml # Customer interface (2 pods)
├── frontend-deployment.yaml          # Frontend (2 pods)
├── infrastructure-deployments.yaml   # PostgreSQL, Kafka, Redis
├── kafka-topic-init.yaml            # Kafka topic initialization
├── ingress.yaml                      # External access
├── deploy.sh                         # Automated deployment script
└── README.md                         # This file
```

## Configuration

### Docker Hub Images

Update the image references in deployment files:

```yaml
# Replace YOUR_DOCKERHUB_USERNAME with your actual username
image: your-dockerhub-username/courier-main-service:latest
image: your-dockerhub-username/courier-customer-interface:latest
image: your-dockerhub-username/courier-frontend:latest
```

### Pod Replicas

Each microservice is configured with **2 pods** for high availability:

```yaml
spec:
  replicas: 2  # 2 pods per service
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
```

### Resource Allocation

| Service | CPU Request | Memory Request | CPU Limit | Memory Limit |
|---------|-------------|----------------|-----------|--------------|
| Main Service | 500m | 1Gi | 1000m | 2Gi |
| Customer Interface | 250m | 512Mi | 500m | 1Gi |
| Frontend | 100m | 128Mi | 200m | 256Mi |

## Event Topics

The deployment automatically creates Kafka topics based on the eventing platform:

### Main Service Topics
- `incoming-parcel-orders` - EDI order submissions
- `abc-transport-events` - External ABC Transport events
- `abc-transport-responses` - ABC Transport responses
- `parcel-tracking-events` - Internal tracking events
- `courier-internal-events` - Internal system events

### Customer Interface Topics
- `customer-parcel-submissions` - Customer-initiated events
- `parcel-status-updates` - Status notifications
- `customer-notifications` - Customer alerts

## Access Methods

### 1. Port Forwarding (Recommended for testing)

```bash
# Main Service
kubectl port-forward svc/courier-main-service 8080:8080 -n courier-system

# Customer Interface
kubectl port-forward svc/courier-customer-interface 8081:8081 -n courier-system

# Frontend
kubectl port-forward svc/courier-frontend 3000:3000 -n courier-system
```

### 2. Ingress (Requires Nginx Ingress Controller)

Add to your `/etc/hosts` file:
```
<INGRESS_IP> api.courier.local
<INGRESS_IP> customer.courier.local
<INGRESS_IP> app.courier.local
```

Access URLs:
- **Main API**: http://api.courier.local/api/v1
- **Customer API**: http://customer.courier.local/api/v1/customer
- **Frontend**: http://app.courier.local

## Monitoring

### Health Checks

```bash
# Check pod status
kubectl get pods -n courier-system

# Check service health
kubectl port-forward svc/courier-main-service 8080:8080 -n courier-system &
curl http://localhost:8080/actuator/health

kubectl port-forward svc/courier-customer-interface 8081:8081 -n courier-system &
curl http://localhost:8081/actuator/health

kubectl port-forward svc/courier-frontend 3000:3000 -n courier-system &
curl http://localhost:3000/health
```

### Logs

```bash
# View logs from all pods of a service
kubectl logs -f deployment/courier-main-service -n courier-system

# View logs from specific pod
kubectl logs -f <pod-name> -n courier-system

# View logs from all containers
kubectl logs -f -l app=courier-main-service -n courier-system
```

### Metrics

```bash
# Prometheus metrics
kubectl port-forward svc/courier-main-service 8080:8080 -n courier-system &
curl http://localhost:8080/actuator/prometheus
```

## Scaling

### Manual Scaling

```bash
# Scale main service to 3 pods
kubectl scale deployment courier-main-service --replicas=3 -n courier-system

# Scale customer interface to 4 pods
kubectl scale deployment courier-customer-interface --replicas=4 -n courier-system

# Scale frontend to 3 pods
kubectl scale deployment courier-frontend --replicas=3 -n courier-system
```

### Auto-scaling (Optional)

Add HPA (Horizontal Pod Autoscaler):

```bash
# Auto-scale based on CPU usage
kubectl autoscale deployment courier-main-service --cpu-percent=70 --min=2 --max=10 -n courier-system
kubectl autoscale deployment courier-customer-interface --cpu-percent=70 --min=2 --max=8 -n courier-system
kubectl autoscale deployment courier-frontend --cpu-percent=70 --min=2 --max=5 -n courier-system
```

## Troubleshooting

### Common Issues

#### 1. Image Pull Errors
```bash
# Check if Docker Hub secret is configured
kubectl get secret dockerhub-secret -n courier-system

# Recreate Docker Hub secret
kubectl delete secret dockerhub-secret -n courier-system
kubectl create secret docker-registry dockerhub-secret \
  --docker-server=docker.io \
  --docker-username=your-username \
  --docker-password=your-password \
  --namespace=courier-system
```

#### 2. Pod Startup Issues
```bash
# Check pod events
kubectl describe pod <pod-name> -n courier-system

# Check logs
kubectl logs <pod-name> -n courier-system

# Check resource constraints
kubectl top pods -n courier-system
```

#### 3. Service Connectivity
```bash
# Test service DNS resolution
kubectl run debug --image=busybox --rm -i --restart=Never -n courier-system -- nslookup courier-main-service

# Test service connectivity
kubectl run debug --image=curlimages/curl --rm -i --restart=Never -n courier-system -- curl http://courier-main-service:8080/actuator/health
```

#### 4. Database Issues
```bash
# Check PostgreSQL logs
kubectl logs deployment/courier-postgresql -n courier-system

# Test database connection
kubectl run postgres-client --image=postgres:15-alpine --rm -i --restart=Never -n courier-system -- psql -h courier-postgresql -U courier_user -d courier_db -c "SELECT 1"
```

### Useful Commands

```bash
# View all resources
kubectl get all -n courier-system

# Check resource usage
kubectl top pods -n courier-system
kubectl top nodes

# Restart a deployment
kubectl rollout restart deployment/courier-main-service -n courier-system

# View deployment history
kubectl rollout history deployment/courier-main-service -n courier-system

# Rollback deployment
kubectl rollout undo deployment/courier-main-service -n courier-system
```

## Cleanup

### Remove Deployment

```bash
# Delete entire namespace (removes everything)
kubectl delete namespace courier-system

# Or delete individual components
kubectl delete -f k8s/dockerhub/
```

### Persistent Data

Note: Deleting the namespace will also delete persistent volumes and data. To preserve data:

```bash
# Backup database before deletion
kubectl exec deployment/courier-postgresql -n courier-system -- pg_dump -U courier_user courier_db > backup.sql

# Restore after redeployment
kubectl exec -i deployment/courier-postgresql -n courier-system -- psql -U courier_user courier_db < backup.sql
```

## Production Considerations

### Security
- Use proper secrets management (e.g., Kubernetes Secrets, HashiCorp Vault)
- Enable network policies for pod-to-pod communication
- Use non-root containers (already configured)
- Enable RBAC and pod security policies

### High Availability
- Deploy across multiple availability zones
- Use pod disruption budgets
- Configure proper resource requests and limits
- Set up monitoring and alerting

### Performance
- Tune JVM settings for containerized environments
- Configure proper database connection pooling
- Set up horizontal pod autoscaling
- Monitor and optimize resource usage

This deployment provides a production-ready setup with high availability (2 pods per service) and comprehensive monitoring capabilities.
