# Courier Customer Interface Microservice

This document describes the customer interface microservice, a dedicated service for customer-facing operations deployed as a separate pod in Azure Kubernetes Service (AKS).

## Overview

The Customer Interface Microservice is a specialized Spring Boot application that provides a customer-focused API for parcel registration, tracking, and management. It's designed to be deployed independently in AKS, allowing for separate scaling, monitoring, and maintenance.

## Architecture

### Microservice Design
```
┌─────────────────────────────────────────────────────────────┐
│                    AKS Cluster                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Customer      │  │   Main Courier  │  │  Notification│ │
│  │   Interface     │  │   Service       │  │   Service    │ │
│  │   (Port 8081)   │  │   (Port 8080)   │  │  (Port 8082) │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
│           │                     │                    │      │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                Shared Services                          │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │ PostgreSQL  │  │    Kafka    │  │      Redis      │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Key Features
- **Independent Deployment**: Separate Docker container and Kubernetes deployment
- **Customer-Focused API**: Simplified endpoints for customer operations
- **Security**: JWT-based authentication with customer role validation
- **Scalability**: Horizontal Pod Autoscaler (HPA) for automatic scaling
- **Monitoring**: Comprehensive health checks and metrics
- **Resilience**: Circuit breakers and retry mechanisms

## Configuration

### Profile: `customer-interface`
The service uses the `customer-interface` Spring profile with specific configurations:

```yaml
spring:
  profiles:
    active: customer-interface
  application:
    name: courier-customer-interface
server:
  port: 8081
  servlet:
    context-path: /api/v1/customer
```

### Environment Variables
| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `customer-interface` |
| `SERVER_PORT` | Application port | `8081` |
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://courier-postgres-service:5432/courier_db` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses | `courier-kafka-service:9092` |
| `JWT_SECRET` | JWT signing secret | (from secret) |
| `MAX_PARCELS_PER_DAY` | Daily parcel limit per customer | `100` |
| `RATE_LIMIT` | API rate limit per minute | `60` |

## API Endpoints

### Authentication
All endpoints require JWT authentication with `CUSTOMER` role.

### Parcel Operations

#### Register Parcel
```http
POST /api/v1/customer/parcels/register
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "ediReference": "CUST-123-1234567890",
  "sender": {
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+1234567890"
  },
  "recipient": {
    "name": "Jane Smith",
    "email": "jane@example.com",
    "phone": "+0987654321"
  },
  "pickupAddress": {
    "streetAddress": "123 Main St",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "USA"
  },
  "deliveryAddress": {
    "streetAddress": "456 Oak Ave",
    "city": "Los Angeles",
    "state": "CA",
    "postalCode": "90001",
    "country": "USA"
  },
  "parcelDetails": {
    "description": "Sample package",
    "weight": 2.5,
    "dimensions": "30x20x15 cm"
  },
  "serviceOptions": {
    "priority": "STANDARD",
    "estimatedDeliveryDate": "2024-01-18T10:00:00"
  }
}
```

#### Get My Parcels
```http
GET /api/v1/customer/parcels/my?page=0&size=10&sortBy=createdAt&sortDir=desc&status=REGISTERED
Authorization: Bearer <jwt-token>
```

#### Track Parcel
```http
GET /api/v1/customer/parcels/{parcelId}/track
Authorization: Bearer <jwt-token>
```

#### Get Parcel Details
```http
GET /api/v1/customer/parcels/{parcelId}
Authorization: Bearer <jwt-token>
```

#### Cancel Parcel
```http
PUT /api/v1/customer/parcels/{parcelId}/cancel
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "reason": "Customer requested cancellation"
}
```

#### Customer Dashboard
```http
GET /api/v1/customer/parcels/dashboard
Authorization: Bearer <jwt-token>
```

### Health Endpoints

#### Basic Health Check
```http
GET /health
```

#### Detailed Health Check
```http
GET /health/detailed
```

#### Kubernetes Probes
```http
GET /health/ready    # Readiness probe
GET /health/live     # Liveness probe
```

## Deployment

### Prerequisites
- Azure CLI installed and configured
- kubectl configured for AKS cluster
- Docker installed
- Access to Azure Container Registry

### Build and Deploy
```bash
# Make script executable
chmod +x scripts/deploy-customer-interface.sh

# Deploy with latest tag
./scripts/deploy-customer-interface.sh latest

# Deploy with specific version
./scripts/deploy-customer-interface.sh v1.2.3
```

### Manual Deployment Steps

1. **Build Docker Image**
   ```bash
   docker build -f Dockerfile.customer-interface -t courierregistry.azurecr.io/courier-customer-interface:latest .
   ```

2. **Push to Registry**
   ```bash
   az acr login --name courierregistry
   docker push courierregistry.azurecr.io/courier-customer-interface:latest
   ```

3. **Apply Kubernetes Manifests**
   ```bash
   kubectl apply -f k8s/customer-interface-config.yaml
   kubectl apply -f k8s/customer-interface-deployment.yaml
   kubectl apply -f k8s/customer-interface-ingress.yaml
   kubectl apply -f k8s/customer-interface-hpa.yaml
   ```

## Kubernetes Resources

### Deployment
- **Replicas**: 3 (minimum), 10 (maximum with HPA)
- **Resources**: 512Mi-1Gi memory, 250m-500m CPU
- **Strategy**: RollingUpdate with maxSurge=1, maxUnavailable=0

### Service
- **Type**: LoadBalancer (internal)
- **Ports**: 80 (HTTP), 8080 (Management)

### Ingress
- **External**: `customer.courier.company.com`
- **Internal**: `customer-internal.courier.local` (management endpoints)
- **TLS**: Enabled with Let's Encrypt

### Autoscaling
- **CPU Target**: 70%
- **Memory Target**: 80%
- **Custom Metrics**: HTTP requests per second (100 avg)

## Security

### Authentication
- JWT-based authentication
- Role-based access control (RBAC)
- Customer role required for all endpoints

### Network Security
- NetworkPolicy restricting ingress/egress
- Internal service communication only
- TLS encryption for external traffic

### Container Security
- Non-root user execution
- Read-only root filesystem
- Security context with dropped capabilities

## Monitoring and Observability

### Health Checks
- **Liveness Probe**: `/health/live`
- **Readiness Probe**: `/health/ready`
- **Startup Probe**: `/actuator/health`

### Metrics
- Prometheus metrics at `/actuator/prometheus`
- Custom business metrics
- JVM and application metrics

### Logging
- Structured JSON logging
- Correlation IDs for tracing
- Log aggregation via Azure Monitor

### Alerts
- High error rates
- High response times
- Pod restart frequency
- Resource utilization

## Business Logic

### Customer Limits
- Maximum 100 parcels per day per customer
- Rate limiting: 60 requests per minute
- File upload limit: 10MB

### Parcel Lifecycle
1. **Registration**: Customer submits parcel details
2. **Validation**: System validates customer limits and data
3. **Processing**: Parcel created and sent to main system
4. **Tracking**: Customer can track parcel status
5. **Cancellation**: Limited cancellation window

### Event Flow
```
Customer Registration → Validation → Database Save → Kafka Event → Main Service
                                                                      ↓
Customer Notifications ← Status Updates ← ABC Transport ← Processing
```

## Troubleshooting

### Common Issues

1. **Pod Not Starting**
   ```bash
   kubectl describe pod <pod-name> -n courier-system
   kubectl logs <pod-name> -n courier-system
   ```

2. **Database Connection Issues**
   ```bash
   kubectl exec -it <pod-name> -n courier-system -- curl localhost:8081/health/detailed
   ```

3. **Kafka Connection Issues**
   ```bash
   kubectl logs <pod-name> -n courier-system | grep -i kafka
   ```

### Useful Commands

```bash
# View service status
kubectl get all -n courier-system -l app=courier-customer-interface

# Port forward for local testing
kubectl port-forward svc/courier-customer-interface-service 8081:80 -n courier-system

# Scale deployment
kubectl scale deployment courier-customer-interface --replicas=5 -n courier-system

# View HPA status
kubectl get hpa -n courier-system

# Check ingress
kubectl get ingress -n courier-system
```

## Performance Tuning

### JVM Options
```bash
JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"
```

### Database Connection Pool
- Maximum pool size: 10
- Minimum idle: 2
- Connection timeout: 20s

### Kafka Configuration
- Batch size: 25
- Retries: 3
- Acknowledgment: all

## Future Enhancements

1. **Real-time Notifications**: WebSocket support for live updates
2. **File Upload**: Support for parcel images and documents
3. **Mobile API**: Optimized endpoints for mobile applications
4. **Analytics**: Customer behavior tracking and insights
5. **Multi-tenant**: Support for multiple courier companies
6. **Caching**: Redis integration for improved performance

## Support

For issues and support:
- **Development Team**: courier-dev@company.com
- **Operations Team**: courier-ops@company.com
- **Documentation**: Internal wiki and Confluence
- **Monitoring**: Azure Monitor and Grafana dashboards
