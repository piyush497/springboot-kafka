# Courier Management System - Docker Hub Integration

## 🚀 **Complete CI/CD Pipeline with Docker Hub**

This project now includes a comprehensive GitHub Actions workflow that automatically builds, tests, and pushes Docker images to Docker Hub for all microservices.

## 📦 **Docker Hub Images**

The following images are automatically built and published:

| Service | Docker Hub Repository | Description |
|---------|----------------------|-------------|
| **Main Service** | `your-username/courier-main-service` | Core business logic, authentication, EDI processing |
| **Customer Interface** | `your-username/courier-customer-interface` | Customer-specific operations and API |
| **Frontend** | `your-username/courier-frontend` | React-based web application |

## 🔄 **Automated Build Process**

### **GitHub Actions Workflow**
```yaml
# Triggers
- Push to main/develop branches
- Pull requests
- Manual workflow dispatch

# Build Matrix
- Main Service (Spring Boot + Gradle)
- Customer Interface (Spring Boot + Gradle)
- Frontend (React + Node.js)

# Multi-Platform Support
- linux/amd64
- linux/arm64
```

### **Build Features**
- ✅ **Smart Change Detection** - Only builds changed services
- ✅ **Parallel Builds** - All services build simultaneously
- ✅ **Multi-Platform Images** - AMD64 and ARM64 support
- ✅ **Security Scanning** - Trivy vulnerability scanning
- ✅ **SBOM Generation** - Software Bill of Materials
- ✅ **Test Integration** - Unit and integration tests
- ✅ **Cache Optimization** - GitHub Actions cache for faster builds

## 🏗️ **Image Tags Strategy**

### **Automatic Tags**
```bash
# Latest from main branch
your-username/courier-main-service:latest

# Branch-specific builds
your-username/courier-main-service:main-20241226-143022

# Commit-specific builds
your-username/courier-main-service:main-abc123def

# Pull request builds
your-username/courier-main-service:pr-42
```

### **Manual Tags**
```bash
# Custom suffix via workflow dispatch
your-username/courier-main-service:latest-beta
your-username/courier-main-service:latest-rc1
```

## 🚀 **Quick Start with Docker Hub Images**

### **Option 1: Automated Script**
```bash
# Clone repository
git clone <repository-url>
cd springboot-kafka

# Run with your Docker Hub username
./scripts/run-dockerhub-images.sh your-dockerhub-username latest start
```

### **Option 2: Docker Compose**
```bash
# Set environment variables
export DOCKER_USERNAME=your-dockerhub-username
export IMAGE_TAG=latest

# Start all services
docker-compose -f docker-compose.dockerhub.yml up -d
```

### **Option 3: Individual Services**
```bash
# Run main service only
docker run -d \
  --name courier-main-service \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  your-dockerhub-username/courier-main-service:latest
```

## 🔧 **GitHub Secrets Configuration**

Set these secrets in your GitHub repository:

```bash
# Docker Hub Authentication
DOCKER_USERNAME=your-dockerhub-username
DOCKER_PASSWORD=your-dockerhub-token

# Optional: Slack Notifications
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
```

## 📊 **Monitoring & Observability**

### **Build Monitoring**
- **GitHub Actions**: Real-time build status and logs
- **Docker Hub**: Image push notifications and webhooks
- **Slack Integration**: Build success/failure notifications

### **Runtime Monitoring**
```bash
# Health checks
curl http://localhost:8080/actuator/health  # Main service
curl http://localhost:8081/actuator/health  # Customer interface
curl http://localhost:3000/health           # Frontend

# Metrics
curl http://localhost:8080/actuator/prometheus  # Prometheus metrics
```

## 🔒 **Security Features**

### **Image Security**
- ✅ **Vulnerability Scanning** - Trivy security scans
- ✅ **SBOM Generation** - Software Bill of Materials
- ✅ **Multi-stage Builds** - Minimal attack surface
- ✅ **Non-root Users** - Containers run as non-root
- ✅ **Read-only Filesystems** - Enhanced security

### **Secrets Management**
- ✅ **GitHub Secrets** - Secure credential storage
- ✅ **Environment Variables** - Runtime configuration
- ✅ **JWT Tokens** - Secure service communication

## 🌍 **Multi-Environment Support**

### **Development**
```bash
# Use latest development images
export IMAGE_TAG=develop-latest
./scripts/run-dockerhub-images.sh username develop-latest start
```

### **Staging**
```bash
# Use specific commit builds
export IMAGE_TAG=main-abc123def
./scripts/run-dockerhub-images.sh username main-abc123def start
```

### **Production**
```bash
# Use stable latest builds
export IMAGE_TAG=latest
./scripts/run-dockerhub-images.sh username latest start
```

## 📈 **Performance Optimizations**

### **Build Performance**
- **GitHub Actions Cache** - Gradle and npm dependencies
- **Docker Layer Caching** - Faster image builds
- **Parallel Builds** - Multiple services simultaneously
- **Smart Triggers** - Only build changed services

### **Runtime Performance**
- **Multi-stage Builds** - Smaller image sizes
- **JVM Optimization** - Tuned for containers
- **Connection Pooling** - Database and Redis
- **Async Processing** - Non-blocking operations

## 🔄 **Event-Driven Architecture**

Based on the implemented eventing platform, the Docker Hub deployment supports:

### **Kafka Topics**
```yaml
# Main service topics (from EventStreamService)
- incoming-parcel-orders      # EDI order submissions
- abc-transport-events        # External ABC Transport events
- abc-transport-responses     # ABC Transport responses
- parcel-tracking-events      # Internal tracking events
- courier-internal-events     # Internal system events

# Customer interface topics
- customer-parcel-submissions # Customer-initiated events
- parcel-status-updates       # Status notifications
- customer-notifications     # Customer alerts
```

### **Multi-Stage Tracking**
```
registered → picked up → in transit → loaded in truck → out for delivery → delivered
```

## 🛠️ **Development Workflow**

### **Local Development**
```bash
# 1. Make code changes
git checkout -b feature/new-feature

# 2. Test locally
./gradlew test
npm test

# 3. Commit and push
git commit -m "Add new feature"
git push origin feature/new-feature

# 4. Create pull request
# GitHub Actions will build and test PR

# 5. Merge to main
# Automatic build and push to Docker Hub
```

### **Testing with Docker Hub Images**
```bash
# Test latest images
./scripts/run-dockerhub-images.sh username latest start

# Test specific commit
./scripts/run-dockerhub-images.sh username main-abc123def start

# Run integration tests
curl -X POST http://localhost:8080/api/v1/edi/submit \
  -H "Content-Type: application/json" \
  -d @sample-edi-order.json
```

## 📋 **Troubleshooting**

### **Build Issues**
```bash
# Check GitHub Actions logs
# Go to Actions tab in GitHub repository

# Check Docker Hub build status
# Visit https://hub.docker.com/u/your-username

# Local build testing
docker build -t test-image -f Dockerfile.backend .
```

### **Runtime Issues**
```bash
# Check container logs
docker-compose -f docker-compose.dockerhub.yml logs courier-main-service

# Check service health
./scripts/run-dockerhub-images.sh username latest status

# Debug container
docker exec -it courier-main-service bash
```

## 📚 **Documentation Links**

- **[Docker Hub Deployment Guide](DOCKER_HUB_DEPLOYMENT.md)** - Detailed deployment instructions
- **[Kubernetes Deployment Guide](DEPLOYMENT_GUIDE.md)** - Production Kubernetes deployment
- **[API Documentation](API_DOCUMENTATION.md)** - REST API reference
- **[EDI Processing Flow](EDI_PROCESSING_FLOW.md)** - EDI order processing

## 🎯 **Next Steps**

1. **Configure GitHub Secrets** - Set up Docker Hub credentials
2. **Customize Docker Hub Username** - Update scripts and documentation
3. **Test Build Pipeline** - Push code changes to trigger builds
4. **Set Up Monitoring** - Configure Slack notifications
5. **Deploy to Production** - Use images in production environment

## 📞 **Support**

- **GitHub Issues**: Report bugs and feature requests
- **Docker Hub**: https://hub.docker.com/u/your-username
- **Documentation**: See linked guides above
- **Community**: Join our development discussions

---

**The Courier Management System now provides a complete CI/CD pipeline with Docker Hub integration, enabling seamless development, testing, and deployment workflows for all microservices.**
