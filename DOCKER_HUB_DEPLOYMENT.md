# Docker Hub Deployment Guide

This guide explains how to use the pre-built Docker images from Docker Hub for the Courier Management System microservices.

## Overview

The GitHub Actions workflow automatically builds and pushes Docker images to Docker Hub for all microservices:

- **Main Service**: `your-username/courier-main-service`
- **Customer Interface**: `your-username/courier-customer-interface`
- **Frontend**: `your-username/courier-frontend`

## Prerequisites

- Docker and Docker Compose installed
- Access to Docker Hub images (public or with authentication)
- At least 4GB RAM and 10GB disk space

## Quick Start

### 1. Using the Automated Script

```bash
# Make the script executable
chmod +x scripts/run-dockerhub-images.sh

# Run with your Docker Hub username
./scripts/run-dockerhub-images.sh your-dockerhub-username latest start

# Or run interactively (script will prompt for username)
./scripts/run-dockerhub-images.sh
```

### 2. Using Docker Compose Directly

```bash
# Set your Docker Hub username and image tag
export DOCKER_USERNAME=your-dockerhub-username
export IMAGE_TAG=latest

# Start all services
docker-compose -f docker-compose.dockerhub.yml up -d

# Check status
docker-compose -f docker-compose.dockerhub.yml ps
```

## Available Image Tags

The GitHub Actions workflow creates multiple tags for each image:

### Automatic Tags
- `latest` - Latest build from main branch
- `main-YYYYMMDD-HHmmss` - Timestamped builds from main
- `develop-YYYYMMDD-HHmmss` - Timestamped builds from develop
- `main-<commit-sha>` - Specific commit builds
- `pr-<number>` - Pull request builds

### Manual Tags
- Custom tags can be added via workflow dispatch with `tag_suffix` parameter

## Service Configuration

### Main Service (Port 8080)
```yaml
courier-main-service:
  image: your-username/courier-main-service:latest
  ports:
    - "8080:8080"
  environment:
    - SPRING_PROFILES_ACTIVE=local
    - DATABASE_URL=jdbc:postgresql://postgres:5432/courier_db
    - KAFKA_BOOTSTRAP_SERVERS=kafka:29092
    - JWT_SECRET=courierMainServiceSecretKey2024!@#$%^&*()
```

### Customer Interface Service (Port 8081)
```yaml
courier-customer-interface:
  image: your-username/courier-customer-interface:latest
  ports:
    - "8081:8081"
  environment:
    - SPRING_PROFILES_ACTIVE=local
    - MAIN_SERVICE_URL=http://courier-main-service:8080
    - MAX_PARCELS_PER_DAY=100
    - RATE_LIMIT=60
```

### Frontend Service (Port 3000)
```yaml
courier-frontend:
  image: your-username/courier-frontend:latest
  ports:
    - "3000:3000"
  environment:
    - REACT_APP_MAIN_API_URL=http://localhost:8080/api/v1
    - REACT_APP_CUSTOMER_API_URL=http://localhost:8081/api/v1/customer
```

## Infrastructure Services

The Docker Compose setup includes all necessary infrastructure:

- **PostgreSQL** (Port 5432) - Database
- **Apache Kafka** (Port 9092) - Event streaming
- **Zookeeper** (Port 2181) - Kafka coordination
- **Redis** (Port 6379) - Caching
- **Kafka UI** (Port 8090) - Kafka management interface

## Access URLs

Once all services are running:

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | Main web application |
| Main API | http://localhost:8080/api/v1 | Core business API |
| Customer API | http://localhost:8081/api/v1/customer | Customer interface API |
| Kafka UI | http://localhost:8090 | Kafka topic management |
| Main Service Health | http://localhost:8080/actuator/health | Health check |
| Customer Service Health | http://localhost:8081/actuator/health | Health check |
| Frontend Health | http://localhost:3000/health | Health check |

## Event Topics

The system automatically creates the following Kafka topics:

### Main Service Topics
- `incoming-parcel-orders` - EDI order submissions
- `abc-transport-events` - External ABC Transport events
- `abc-transport-responses` - ABC Transport system responses
- `parcel-tracking-events` - Internal tracking events
- `courier-internal-events` - Internal system events

### Customer Interface Topics
- `customer-parcel-submissions` - Customer-initiated events
- `parcel-status-updates` - Status change notifications
- `customer-notifications` - Customer notifications

## Environment Variables

### Required Environment Variables

```bash
# Docker Hub Configuration
DOCKER_USERNAME=your-dockerhub-username
IMAGE_TAG=latest

# Database Configuration (automatically set in docker-compose)
DATABASE_URL=jdbc:postgresql://postgres:5432/courier_db
DATABASE_USERNAME=courier_user
DATABASE_PASSWORD=courier_pass

# Kafka Configuration (automatically set)
KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# Service URLs (automatically set)
MAIN_SERVICE_URL=http://courier-main-service:8080
```

### Optional Environment Variables

```bash
# Logging
LOG_LEVEL=INFO

# Customer Interface Limits
MAX_PARCELS_PER_DAY=100
RATE_LIMIT=60

# Frontend Configuration
REACT_APP_ENVIRONMENT=development
REACT_APP_VERSION=latest

# Security (use strong secrets in production)
JWT_SECRET=your-jwt-secret-key
```

## Script Commands

The `run-dockerhub-images.sh` script supports multiple actions:

```bash
# Start services
./scripts/run-dockerhub-images.sh username latest start

# Stop services
./scripts/run-dockerhub-images.sh username latest stop

# Restart services
./scripts/run-dockerhub-images.sh username latest restart

# Check status
./scripts/run-dockerhub-images.sh username latest status

# View logs
./scripts/run-dockerhub-images.sh username latest logs

# Clean up everything
./scripts/run-dockerhub-images.sh username latest cleanup

# Show help
./scripts/run-dockerhub-images.sh help
```

## Troubleshooting

### Common Issues

#### 1. Images Not Found
```bash
# Check if images exist on Docker Hub
docker manifest inspect your-username/courier-main-service:latest

# Pull images manually
docker pull your-username/courier-main-service:latest
docker pull your-username/courier-customer-interface:latest
docker pull your-username/courier-frontend:latest
```

#### 2. Services Not Starting
```bash
# Check container logs
docker-compose -f docker-compose.dockerhub.yml logs courier-main-service
docker-compose -f docker-compose.dockerhub.yml logs courier-customer-interface
docker-compose -f docker-compose.dockerhub.yml logs courier-frontend

# Check container status
docker-compose -f docker-compose.dockerhub.yml ps
```

#### 3. Database Connection Issues
```bash
# Check PostgreSQL container
docker-compose -f docker-compose.dockerhub.yml logs postgres

# Test database connection
docker exec -it courier-postgres psql -U courier_user -d courier_db -c "SELECT 1"
```

#### 4. Kafka Issues
```bash
# Check Kafka logs
docker-compose -f docker-compose.dockerhub.yml logs kafka

# List topics
docker exec -it courier-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check topic details
docker exec -it courier-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic parcel-tracking-events
```

### Health Checks

```bash
# Check all service health
curl http://localhost:8080/actuator/health  # Main service
curl http://localhost:8081/actuator/health  # Customer interface
curl http://localhost:3000/health           # Frontend

# Check infrastructure health
docker exec -it courier-postgres pg_isready -U courier_user
docker exec -it courier-redis redis-cli ping
docker exec -it courier-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

### Performance Monitoring

```bash
# Check resource usage
docker stats

# Check specific service resources
docker stats courier-main-service courier-customer-interface courier-frontend

# View detailed container info
docker inspect courier-main-service
```

## Production Considerations

### Security
- Change default passwords and JWT secrets
- Use environment-specific configuration
- Enable TLS/SSL for external access
- Implement proper network segmentation

### Scaling
- Use Docker Swarm or Kubernetes for orchestration
- Configure horizontal pod autoscaling
- Set up load balancers
- Monitor resource usage

### Monitoring
- Set up Prometheus and Grafana
- Configure log aggregation
- Implement health check monitoring
- Set up alerting

### Backup
- Regular database backups
- Kafka topic backup strategies
- Configuration backup
- Disaster recovery planning

## Integration with CI/CD

The Docker Hub images are automatically updated by the GitHub Actions workflow:

1. **Code Changes** → GitHub push/PR
2. **Build & Test** → GitHub Actions workflow
3. **Image Build** → Multi-platform Docker images
4. **Push to Hub** → Docker Hub repository
5. **Deploy** → Pull and run updated images

### Webhook Integration
Set up Docker Hub webhooks to trigger deployments:

```bash
# Example webhook URL for automatic deployment
POST https://your-deployment-server.com/webhook/docker-hub
{
  "repository": {
    "name": "courier-main-service",
    "namespace": "your-username"
  },
  "push_data": {
    "tag": "latest"
  }
}
```

## Support

### Documentation
- [Main README](README.md) - Project overview
- [Deployment Guide](DEPLOYMENT_GUIDE.md) - Kubernetes deployment
- [API Documentation](API_DOCUMENTATION.md) - REST API reference

### Monitoring
- **Logs**: `./logs/` directory
- **Metrics**: http://localhost:8080/actuator/prometheus
- **Health**: http://localhost:8080/actuator/health

### Contact
- **Development Team**: courier-dev@company.com
- **Operations Team**: courier-ops@company.com
- **Docker Hub**: https://hub.docker.com/u/your-username

This Docker Hub deployment provides a production-ready way to run the Courier Management System microservices with minimal setup and maximum reliability.
