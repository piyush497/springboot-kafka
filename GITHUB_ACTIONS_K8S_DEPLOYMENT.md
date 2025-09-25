# GitHub Actions Kubernetes Deployment Guide

This guide explains the `deployMicroservicesOnLocalMachineDocker` GitHub Actions workflow that sets up a local Kubernetes cluster using Kind (Kubernetes in Docker), builds microservices images, pushes them to Docker Hub, and deploys them to the K8s cluster.

## 🏗️ Workflow Overview

### Architecture
```
GitHub Actions Runner
├── Kind Cluster (Kubernetes in Docker)
│   ├── Control Plane Node
│   ├── Worker Node 1
│   └── Worker Node 2
├── Docker Hub Registry
│   ├── courier-main-service:latest
│   ├── courier-customer-interface:latest
│   └── courier-frontend:latest
└── Microservices Deployment (2 pods each)
    ├── Main Service (8080)
    ├── Customer Interface (8081)
    ├── Frontend (3000)
    └── Infrastructure (PostgreSQL, Kafka, Redis)
```

### Workflow Jobs
1. **setup-k8s-cluster**: Creates Kind cluster with NGINX Ingress
2. **build-and-push-images**: Builds and pushes Docker images to Docker Hub
3. **deploy-infrastructure**: Deploys PostgreSQL, Kafka, Zookeeper, Redis
4. **deploy-microservices**: Deploys the 3 microservices with 2 pods each
5. **verify-deployment**: Runs health checks and API tests
6. **cleanup**: Removes Kind cluster and Docker resources

## 🚀 Setup Instructions

### 1. Repository Secrets

Configure the following secrets in your GitHub repository:

```bash
# Go to: Settings > Secrets and variables > Actions > New repository secret

DOCKER_HUB_USERNAME=your-dockerhub-username
DOCKER_HUB_TOKEN=your-dockerhub-access-token
```

### 2. Docker Hub Access Token

Create a Docker Hub access token:
1. Login to Docker Hub
2. Go to Account Settings > Security
3. Click "New Access Token"
4. Name: `github-actions-courier-system`
5. Permissions: Read, Write, Delete
6. Copy the token and add it as `DOCKER_HUB_TOKEN` secret

### 3. Trigger the Workflow

#### **Automatic Triggers**
```yaml
# Triggers on push to main/develop branches
git push origin main

# Triggers on pull requests to main
# Create PR to main branch
```

#### **Manual Trigger**
```bash
# Go to: Actions > deployMicroservicesOnLocalMachineDocker > Run workflow
# Select:
# - Branch: main
# - Deployment Environment: local-k8s
# - Docker Image Tag: latest (or custom tag)
```

## 🔧 Workflow Configuration

### Kind Cluster Configuration
```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: courier-k8s-cluster
nodes:
- role: control-plane
  extraPortMappings:
  - containerPort: 80    # HTTP
    hostPort: 80
  - containerPort: 443   # HTTPS
    hostPort: 443
  - containerPort: 30080 # Main Service NodePort
    hostPort: 30080
  - containerPort: 30081 # Customer Interface NodePort
    hostPort: 30081
  - containerPort: 30000 # Frontend NodePort
    hostPort: 30000
- role: worker
- role: worker
```

### Multi-Platform Image Builds
```yaml
platforms: linux/amd64,linux/arm64
cache-from: type=gha
cache-to: type=gha,mode=max
```

### Deployment Strategy
```yaml
# Each microservice deployed with 2 pods
replicas: 2
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1
    maxUnavailable: 0
```

## 📊 Deployment Process

### Phase 1: Cluster Setup (5-10 minutes)
```bash
# Creates Kind cluster with:
✓ 1 Control Plane Node
✓ 2 Worker Nodes
✓ NGINX Ingress Controller
✓ Port mappings for services
```

### Phase 2: Image Building (10-15 minutes)
```bash
# Builds and pushes:
✓ courier-main-service (Spring Boot)
✓ courier-customer-interface (Spring Boot)
✓ courier-frontend (React)
```

### Phase 3: Infrastructure Deployment (5-10 minutes)
```bash
# Deploys:
✓ PostgreSQL (1 pod)
✓ Apache Kafka (1 pod)
✓ Zookeeper (1 pod)
✓ Redis (1 pod)
✓ Kafka topic initialization
```

### Phase 4: Microservices Deployment (5-10 minutes)
```bash
# Deploys with 2 pods each:
✓ Main Service (2 pods)
✓ Customer Interface (2 pods)
✓ Frontend (2 pods)
✓ Ingress configuration
```

### Phase 5: Verification (2-5 minutes)
```bash
# Verifies:
✓ Pod health status
✓ Service endpoints
✓ API connectivity
✓ Load balancing
```

## 🔍 Monitoring & Verification

### Deployment Status
```bash
# The workflow automatically checks:
kubectl get pods -n courier-system
kubectl get services -n courier-system
kubectl get ingress -n courier-system
```

### Health Checks
```bash
# Automated health verification:
curl http://localhost:8080/actuator/health  # Main Service
curl http://localhost:8081/actuator/health  # Customer Interface
curl http://localhost:3000/health           # Frontend
```

### API Testing
```bash
# Automated API endpoint testing:
curl http://localhost:8080/api/v1/customers
curl http://localhost:8080/api/v1/parcels
curl http://localhost:8081/api/v1/customer/parcels/health
```

### Pod Distribution Verification
```bash
# Ensures 2 pods per service:
Main Service pods: 2/2 Running
Customer Interface pods: 2/2 Running
Frontend pods: 2/2 Running
```

## 📋 Workflow Outputs

### Deployment Report
The workflow generates a comprehensive deployment report:

```
=== Deployment Report ===
Timestamp: 2024-02-16 10:30:00
Image Tag: latest
Namespace: courier-system

=== Pod Status ===
NAME                                    READY   STATUS    RESTARTS
courier-main-service-xxx-xxx            1/1     Running   0
courier-main-service-yyy-yyy            1/1     Running   0
courier-customer-interface-xxx-xxx      1/1     Running   0
courier-customer-interface-yyy-yyy      1/1     Running   0
courier-frontend-xxx-xxx                1/1     Running   0
courier-frontend-yyy-yyy                1/1     Running   0

=== Service Status ===
NAME                        TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)
courier-main-service        ClusterIP   10.96.xxx.xxx   <none>        8080/TCP
courier-customer-interface  ClusterIP   10.96.xxx.xxx   <none>        8081/TCP
courier-frontend            ClusterIP   10.96.xxx.xxx   <none>        3000/TCP

=== Resource Usage ===
NAME                                    CPU(cores)   MEMORY(bytes)
courier-main-service-xxx-xxx            250m         1Gi
courier-customer-interface-xxx-xxx      125m         512Mi
courier-frontend-xxx-xxx                50m          128Mi
```

### Artifacts
- **deployment-report-{tag}.txt**: Complete deployment status
- **Build logs**: Individual service build outputs
- **Test results**: Health check and API test results

## 🛠️ Local Testing

### Using the Local Script
```bash
# Make script executable
chmod +x scripts/local-k8s-deploy.sh

# Set environment variables
export DOCKER_HUB_USERNAME=your-username
export IMAGE_TAG=latest

# Run full deployment
./scripts/local-k8s-deploy.sh deploy

# Verify deployment
./scripts/local-k8s-deploy.sh verify

# Cleanup
./scripts/local-k8s-deploy.sh cleanup
```

### Manual Local Testing
```bash
# 1. Install Kind
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind

# 2. Create cluster
kind create cluster --name courier-k8s-cluster

# 3. Build and push images
docker build -f Dockerfile.backend -t your-username/courier-main-service:latest .
docker push your-username/courier-main-service:latest

# 4. Deploy to cluster
kubectl apply -f k8s/dockerhub/

# 5. Verify deployment
kubectl get pods -n courier-system
```

## 🚨 Troubleshooting

### Common Issues

#### **1. Docker Hub Authentication Failed**
```bash
# Error: unauthorized: authentication required
# Solution: Check DOCKER_HUB_USERNAME and DOCKER_HUB_TOKEN secrets
```

#### **2. Kind Cluster Creation Failed**
```bash
# Error: failed to create cluster
# Solution: Check Docker daemon is running and has sufficient resources
```

#### **3. Image Pull Errors**
```bash
# Error: ErrImagePull or ImagePullBackOff
# Solution: Verify image exists in Docker Hub and credentials are correct
```

#### **4. Pod Startup Failures**
```bash
# Error: CrashLoopBackOff or Pending
# Solution: Check resource limits and node capacity
kubectl describe pod <pod-name> -n courier-system
kubectl logs <pod-name> -n courier-system
```

#### **5. Service Connectivity Issues**
```bash
# Error: Connection refused or timeout
# Solution: Verify service ports and network policies
kubectl get svc -n courier-system
kubectl describe svc <service-name> -n courier-system
```

### Debugging Commands
```bash
# Check workflow logs in GitHub Actions UI
# Or use GitHub CLI:
gh run list --workflow=deploy-microservices-local-k8s.yml
gh run view <run-id> --log

# Local debugging:
kubectl get events -n courier-system --sort-by='.lastTimestamp'
kubectl top pods -n courier-system
kubectl describe nodes
```

## 🔧 Customization

### Custom Image Tags
```bash
# Trigger workflow with custom tag:
# Go to Actions > Run workflow
# Set "Docker Image Tag" to: v1.0.0, develop, feature-branch
```

### Environment-Specific Deployments
```yaml
# Modify workflow for different environments:
env:
  K8S_NAMESPACE: courier-system-staging
  REPLICAS: 3  # Scale to 3 pods per service
```

### Resource Adjustments
```yaml
# Modify k8s/dockerhub/*-deployment.yaml:
resources:
  requests:
    memory: "2Gi"    # Increase memory
    cpu: "1000m"     # Increase CPU
  limits:
    memory: "4Gi"
    cpu: "2000m"
```

### Additional Services
```yaml
# Add monitoring stack:
- name: Deploy Prometheus
  run: |
    kubectl apply -f k8s/monitoring/prometheus.yaml
    kubectl apply -f k8s/monitoring/grafana.yaml
```

## 📈 Performance Considerations

### Resource Requirements
```yaml
# Minimum GitHub Actions runner requirements:
- CPU: 4 cores
- Memory: 8GB
- Disk: 20GB
- Network: High bandwidth for image pulls/pushes
```

### Optimization Strategies
```yaml
# 1. Use build caches
cache-from: type=gha
cache-to: type=gha,mode=max

# 2. Parallel builds
strategy:
  matrix:
    service: [main-service, customer-interface, frontend]

# 3. Resource limits
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
```

## 🔒 Security Considerations

### Secrets Management
```bash
# Never commit secrets to repository
# Use GitHub Secrets for sensitive data
# Rotate Docker Hub tokens regularly
```

### Image Security
```bash
# Scan images for vulnerabilities
- name: Run Trivy vulnerability scanner
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: '${{ env.DOCKER_HUB_USERNAME }}/courier-main-service:${{ env.IMAGE_TAG }}'
```

### Network Security
```yaml
# Apply network policies
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: courier-network-policy
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/name: courier-microservices
  policyTypes:
  - Ingress
  - Egress
```

## 📞 Support

### Getting Help
1. **Check workflow logs** in GitHub Actions UI
2. **Review deployment report** artifact
3. **Use local testing script** for debugging
4. **Check Kubernetes events** for pod issues
5. **Verify Docker Hub images** are accessible

### Useful Resources
- [Kind Documentation](https://kind.sigs.k8s.io/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Docker Hub Documentation](https://docs.docker.com/docker-hub/)

The workflow provides a complete CI/CD pipeline for deploying the Courier Management System microservices to a local Kubernetes cluster, enabling comprehensive testing of the containerized application stack.
