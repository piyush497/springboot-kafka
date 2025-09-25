# Courier Management System - Microservices Architecture

This document describes the microservices architecture for the Courier Management System, with separate frontend and backend services designed for scalability, maintainability, and independent deployment.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           AKS Cluster / Docker Environment                  │
│                                                                             │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────────────┐  │
│  │   API Gateway   │    │    Frontend     │    │      Monitoring        │  │
│  │   (Nginx)       │    │   (React SPA)   │    │   (Prometheus/Grafana) │  │
│  │   Port: 80      │    │   Port: 3000    │    │                        │  │
│  └─────────────────┘    └─────────────────┘    └─────────────────────────┘  │
│           │                       │                                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                        Backend Services                                 │  │
│  │  ┌─────────────────┐              ┌─────────────────────────────────┐   │  │
│  │  │   Main Service  │              │   Customer Interface Service   │   │  │
│  │  │   Port: 8080    │              │         Port: 8081             │   │  │
│  │  │                 │              │                                 │   │  │
│  │  │ • Authentication│              │ • Customer Registration        │   │  │
│  │  │ • EDI Processing│              │ • Parcel Management            │   │  │
│  │  │ • Admin APIs    │              │ • Customer Dashboard           │   │  │
│  │  │ • Operator APIs │              │ • Parcel Tracking              │   │  │
│  │  │ • System Mgmt   │              │ • Customer-specific Features   │   │  │
│  │  └─────────────────┘              └─────────────────────────────────┘   │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
│                                      │                                       │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │                        Infrastructure Services                          │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │  │
│  │  │ PostgreSQL  │  │    Kafka    │  │    Redis    │  │    Kafka UI     │  │  │
│  │  │ Port: 5432  │  │ Port: 9092  │  │ Port: 6379  │  │   Port: 8090    │  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘  │  │
│  └─────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Microservices Components

### 1. Frontend Service (React SPA)
**Technology**: React 18, Material-UI, Nginx  
**Port**: 3000  
**Purpose**: User interface for customers, operators, and administrators

**Features**:
- Customer dashboard and parcel management
- Operator interface for parcel operations
- Admin interface for system management
- Real-time tracking and notifications
- Responsive design for mobile and desktop
- JWT-based authentication
- Role-based access control

**Key Components**:
- Authentication (Login/Register)
- Customer Dashboard
- Parcel Registration Form
- Parcel Tracking Interface
- Parcel List Management
- Public Tracking Page

### 2. Main Service (Spring Boot)
**Technology**: Spring Boot, Spring Security, Spring Data JPA  
**Port**: 8080  
**Purpose**: Core business logic and system administration

**Features**:
- User authentication and authorization
- EDI order processing
- System administration
- Operator functionalities
- ABC Transport integration
- Event publishing and consumption

**Key APIs**:
- `/api/v1/auth/*` - Authentication endpoints
- `/api/v1/edi/*` - EDI processing
- `/api/v1/admin/*` - Admin operations
- `/api/v1/operator/*` - Operator functions
- `/api/v1/parcels/*` - General parcel operations

### 3. Customer Interface Service (Spring Boot)
**Technology**: Spring Boot, Spring Security, Spring Data JPA  
**Port**: 8081  
**Purpose**: Customer-specific operations and business logic

**Features**:
- Customer parcel registration
- Customer-specific parcel tracking
- Customer dashboard data
- Parcel cancellation
- Customer limits and validation
- Customer event publishing

**Key APIs**:
- `/api/v1/customer/parcels/register` - Register new parcel
- `/api/v1/customer/parcels/my` - Get customer parcels
- `/api/v1/customer/parcels/{id}/track` - Track specific parcel
- `/api/v1/customer/parcels/dashboard` - Dashboard data

## Infrastructure Services

### PostgreSQL Database
- **Purpose**: Primary data storage
- **Port**: 5432
- **Features**: ACID compliance, relational data integrity
- **Tables**: Users, Parcels, Customers, Addresses, Tracking Events

### Apache Kafka
- **Purpose**: Event streaming and messaging
- **Port**: 9092
- **Topics**:
  - `incoming-parcel-orders` - EDI order submissions
  - `abc-transport-events` - External transport events
  - `abc-transport-responses` - Transport system responses
  - `parcel-tracking-events` - Internal tracking events
  - `customer-parcel-submissions` - Customer-specific events
  - `parcel-status-updates` - Status change notifications

### Redis
- **Purpose**: Caching and session storage
- **Port**: 6379
- **Use Cases**: JWT token blacklisting, rate limiting, caching

### Kafka UI
- **Purpose**: Kafka monitoring and management
- **Port**: 8090
- **Features**: Topic visualization, message inspection, consumer monitoring

## Deployment Configurations

### Docker Compose (Local Development)
```bash
# Start all microservices locally
./scripts/start-microservices.bat

# Stop all microservices
./scripts/stop-microservices.bat
```

**Services**:
- All infrastructure services (Kafka, PostgreSQL, Redis)
- Backend services with different profiles
- Frontend service with nginx
- API Gateway for routing

### Kubernetes (Production)
```bash
# Deploy to AKS
./scripts/deploy-microservices-aks.sh latest
```

**Features**:
- Horizontal Pod Autoscaling (HPA)
- Network policies for security
- Health checks and probes
- Service discovery
- Load balancing
- Rolling updates

## Communication Patterns

### Synchronous Communication
- **Frontend ↔ Backend**: HTTP/REST APIs
- **Service-to-Service**: HTTP with circuit breakers
- **API Gateway**: Request routing and load balancing

### Asynchronous Communication
- **Event Publishing**: Kafka topics for decoupled communication
- **Event Consumption**: Spring Cloud Stream consumers
- **Message Patterns**: Publish-Subscribe, Event Sourcing

## Security Architecture

### Authentication & Authorization
- **JWT Tokens**: Stateless authentication
- **Role-Based Access Control**: CUSTOMER, OPERATOR, ADMIN roles
- **Service-to-Service**: Internal API authentication
- **Rate Limiting**: API gateway and service-level limits

### Network Security
- **Network Policies**: Kubernetes network isolation
- **TLS/SSL**: Encrypted communication
- **Secret Management**: Kubernetes secrets for sensitive data
- **Container Security**: Non-root users, read-only filesystems

## Monitoring & Observability

### Health Checks
- **Kubernetes Probes**: Liveness, readiness, startup probes
- **Custom Health Endpoints**: Service-specific health indicators
- **Database Connectivity**: Connection pool monitoring
- **External Dependencies**: Kafka, Redis health checks

### Logging
- **Structured Logging**: JSON format with correlation IDs
- **Centralized Logs**: ELK stack or Azure Monitor
- **Log Levels**: Environment-specific configuration
- **Audit Logging**: Security and business event tracking

### Metrics
- **Prometheus Integration**: Application and JVM metrics
- **Custom Metrics**: Business-specific indicators
- **Grafana Dashboards**: Visualization and alerting
- **Performance Monitoring**: Response times, throughput

## Data Management

### Database Strategy
- **Shared Database**: PostgreSQL for all services
- **Connection Pooling**: HikariCP configuration
- **Transaction Management**: Service-level transactions
- **Migration Strategy**: Flyway or Liquibase

### Event Sourcing
- **Event Store**: Kafka topics as event log
- **Event Replay**: Ability to rebuild state from events
- **Saga Pattern**: Distributed transaction management
- **CQRS**: Command Query Responsibility Segregation

## Development Workflow

### Local Development
1. **Start Infrastructure**: `docker-compose up -d postgres kafka redis`
2. **Start Backend Services**: Run with different profiles
3. **Start Frontend**: `npm start` in frontend directory
4. **API Testing**: Use Postman or curl for API testing

### CI/CD Pipeline
1. **Code Commit**: Git repository triggers
2. **Build Phase**: Docker image building
3. **Test Phase**: Unit, integration, and contract tests
4. **Security Scan**: Container and dependency scanning
5. **Deploy Phase**: AKS deployment with blue-green strategy

## Scaling Strategies

### Horizontal Scaling
- **Frontend**: Multiple nginx instances behind load balancer
- **Backend Services**: Kubernetes HPA based on CPU/memory
- **Database**: Read replicas for query scaling
- **Kafka**: Multiple partitions for parallel processing

### Vertical Scaling
- **Resource Limits**: CPU and memory optimization
- **JVM Tuning**: Garbage collection and heap sizing
- **Connection Pools**: Database connection optimization
- **Caching**: Redis for frequently accessed data

## Disaster Recovery

### Backup Strategy
- **Database Backups**: Automated PostgreSQL backups
- **Configuration Backups**: Kubernetes manifests and configs
- **Event Store**: Kafka topic retention and backup
- **Code Repository**: Git-based version control

### High Availability
- **Multi-Zone Deployment**: AKS node pools across zones
- **Service Redundancy**: Multiple replicas per service
- **Database Clustering**: PostgreSQL high availability
- **Load Balancing**: Traffic distribution across instances

## Migration Strategy

### From Monolith to Microservices
1. **Strangler Fig Pattern**: Gradually replace monolith components
2. **Database Decomposition**: Extract service-specific schemas
3. **API Versioning**: Maintain backward compatibility
4. **Feature Toggles**: Control feature rollout

### Zero-Downtime Deployment
1. **Blue-Green Deployment**: Parallel environment switching
2. **Rolling Updates**: Gradual pod replacement
3. **Circuit Breakers**: Fault tolerance during updates
4. **Health Checks**: Automated readiness validation

## Performance Optimization

### Frontend Optimization
- **Code Splitting**: Lazy loading of components
- **Bundle Optimization**: Webpack configuration
- **CDN Integration**: Static asset delivery
- **Caching Strategy**: Browser and proxy caching

### Backend Optimization
- **Database Indexing**: Query performance optimization
- **Connection Pooling**: Resource management
- **Async Processing**: Non-blocking operations
- **Caching Layers**: Redis for hot data

## Troubleshooting Guide

### Common Issues
1. **Service Discovery**: DNS resolution problems
2. **Database Connections**: Pool exhaustion
3. **Kafka Connectivity**: Topic access issues
4. **Authentication**: JWT token problems

### Debugging Commands
```bash
# Check pod status
kubectl get pods -n courier-system

# View service logs
kubectl logs -f deployment/courier-main-service -n courier-system

# Port forward for local access
kubectl port-forward svc/courier-main-service 8080:8080 -n courier-system

# Execute into pod
kubectl exec -it deployment/courier-main-service -n courier-system -- /bin/bash

# Check service connectivity
kubectl run test-pod --image=curlimages/curl --rm -i --restart=Never -n courier-system -- curl -f http://courier-main-service:8080/actuator/health
```

## Future Enhancements

### Technical Improvements
- **Service Mesh**: Istio for advanced traffic management
- **API Gateway**: Kong or Ambassador for advanced routing
- **Event Streaming**: Apache Pulsar for enhanced messaging
- **Database**: Microservice-specific databases

### Business Features
- **Real-time Notifications**: WebSocket integration
- **Mobile Applications**: React Native or native apps
- **Analytics Platform**: Business intelligence and reporting
- **Third-party Integrations**: Additional transport providers

This microservices architecture provides a robust, scalable, and maintainable foundation for the Courier Management System, enabling independent development, deployment, and scaling of each service component.
