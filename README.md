# Courier Management System

A comprehensive microservices-based courier management system built with Spring Boot, featuring event-driven architecture, real-time parcel tracking, and multi-stage delivery processing.

## 🏗️ Architecture Overview

### Microservices Architecture
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Main Service   │    │ Customer        │
│   (React)       │◄──►│   (Spring Boot)  │◄──►│ Interface       │
│   Port: 3000    │    │   Port: 8080     │    │ (Spring Boot)   │
└─────────────────┘    └──────────────────┘    │ Port: 8081      │
                                │               └─────────────────┘
                                │
                       ┌────────▼────────┐
                       │  Event Stream   │
                       │     (Kafka)     │
                       │  Port: 9092     │
                       └─────────────────┘
                                │
                       ┌────────▼────────┐
                       │   PostgreSQL    │
                       │   Port: 5432    │
                       └─────────────────┘
```

### Key Components

#### **Main Service** (`port: 8080`)
- Core business logic and parcel management
- REST API for parcel operations
- Event publishing and consumption
- ABC Transport system integration
- JWT-based authentication

#### **Customer Interface Service** (`port: 8081`)
- Customer-facing operations
- Secure parcel registration and tracking
- Role-based access control (`CUSTOMER` role)
- Inter-service communication with Main Service
- Resilience patterns (Circuit Breaker, Retry, Time Limiter)

#### **Frontend** (`port: 3000`)
- React-based web application
- Modern UI for parcel management
- Real-time status updates
- Customer and admin interfaces

#### **Event Streaming Platform**
- **Local Development**: Apache Kafka
- **Production**: Azure Event Hub
- **Topics**: parcel-tracking-events, courier-internal-events, abc-transport-events
- **Real-time Processing**: Multi-stage parcel lifecycle events

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Docker & Docker Compose
- Node.js 16+ (for frontend)
- PostgreSQL 13+ (or use Docker)

### 1. Clone and Setup
```bash
git clone <repository-url>
cd springboot-kafka
```

### 2. Start Infrastructure
```bash
# Start PostgreSQL, Kafka, Zookeeper, Redis
./start-local.bat          # Windows
./start-local.sh           # Linux/macOS

# Or use Docker Compose
docker-compose -f docker-compose.microservices-updated.yml up -d
```

### 3. Seed Database (400+ Records)
```bash
# Automated seeding
./scripts/run-seed-data.bat    # Windows
./scripts/run-seed-data.sh     # Linux/macOS

# Manual seeding
./gradlew bootRun              # DataSeedingService runs automatically
```

### 4. Start Services
```bash
# Main Service
./gradlew bootRun

# Customer Interface (separate terminal)
cd customer-interface-service
./gradlew bootRun

# Frontend (separate terminal)
cd frontend
npm install
npm start
```

### 5. Access Applications
- **Main API**: http://localhost:8080/api/v1
- **Customer API**: http://localhost:8081/api/v1/customer
- **Frontend**: http://localhost:3000
- **Kafka UI**: http://localhost:8090
- **API Documentation**: http://localhost:8080/swagger-ui.html

## 📊 Sample Data

The system includes **400+ realistic records**:

| Data Type | Count | Description |
|-----------|-------|-------------|
| **Customers** | 50 | Businesses and individuals |
| **Parcels** | 200 | Various delivery statuses |
| **EDI Orders** | 100 | Processed and pending |
| **Tracking Events** | 50+ | Multi-stage lifecycle |

### Parcel Status Distribution
```
DELIVERED (50)        ████████████████████
OUT_FOR_DELIVERY (30) ████████████
LOADED_IN_TRUCK (40)  ████████████████
IN_TRANSIT (40)       ████████████████
PICKED_UP (30)        ████████████
REGISTERED (10)       ████
```

## 🔄 Event-Driven Architecture

### Event Flow
```
Parcel Registration → Pickup → Transit → Truck Loading → Delivery → Completion
       │                │        │           │            │          │
       ▼                ▼        ▼           ▼            ▼          ▼
   [REGISTERED] → [PICKED_UP] → [IN_TRANSIT] → [LOADED_IN_TRUCK] → [OUT_FOR_DELIVERY] → [DELIVERED]
```

### Kafka Topics

#### **Internal Events**
- `parcel-tracking-events` - Real-time parcel status updates
- `courier-internal-events` - System-wide notifications
- `customer-parcel-submissions` - Customer-initiated events
- `parcel-status-updates` - Status change notifications

#### **External Integration**
- `abc-transport-events` - Outbound to ABC Transport system
- `abc-transport-responses` - Inbound from ABC Transport system

### Event Streaming Service
```java
@Service
public class EventStreamService {
    // Unified event publishing using Spring Cloud Stream
    // Dual support: Local Kafka + Production Azure Event Hub
    
    public void publishParcelEvent(ParcelEvent event) {
        // Publishes to parcel-tracking-events topic
    }
    
    public void publishABCTransportEvent(ABCTransportEvent event) {
        // Publishes to abc-transport-events topic
    }
}
```

## 🛡️ Security & Authentication

### JWT Authentication
```yaml
# Main Service - Full access
jwt:
  secret: courierMainServiceSecretKey2024
  expiration: 86400000  # 24 hours

# Customer Interface - Customer role only
jwt:
  secret: courierCustomerInterfaceSecretKey2024
  expiration: 43200000  # 12 hours
```

### Role-Based Access Control
```java
@PreAuthorize("hasRole('CUSTOMER')")
@RestController
@RequestMapping("/api/v1/customer")
public class CustomerParcelController {
    // Customer-only endpoints
}

@PreAuthorize("hasRole('ADMIN')")
@RestController  
@RequestMapping("/api/v1/admin")
public class AdminController {
    // Admin-only endpoints
}
```

### Security Features
- JWT token validation
- Role-based endpoint protection
- CORS configuration
- Input validation and sanitization
- SQL injection prevention

## 📡 API Documentation

### Main Service APIs (`localhost:8080`)

#### **Customers**
```bash
GET    /api/v1/customers              # List all customers
GET    /api/v1/customers/{id}         # Get customer by ID
POST   /api/v1/customers              # Create customer
PUT    /api/v1/customers/{id}         # Update customer
DELETE /api/v1/customers/{id}         # Delete customer
GET    /api/v1/customers/search?name={name}  # Search customers
```

#### **Parcels**
```bash
GET    /api/v1/parcels                # List all parcels
GET    /api/v1/parcels/{id}           # Get parcel by ID
POST   /api/v1/parcels                # Create parcel
PUT    /api/v1/parcels/{id}           # Update parcel
GET    /api/v1/parcels/tracking/{trackingNumber}  # Track parcel
GET    /api/v1/parcels/status/{status}            # Get by status
```

#### **EDI Orders**
```bash
GET    /api/v1/edi/orders             # List EDI orders
POST   /api/v1/edi/orders             # Submit EDI order
GET    /api/v1/edi/orders/{reference} # Get by reference
GET    /api/v1/edi/orders/status/{status}  # Get by status
```

#### **Tracking Events**
```bash
GET    /api/v1/parcels/{id}/tracking  # Get parcel tracking history
POST   /api/v1/tracking/events        # Create tracking event
GET    /api/v1/tracking/events        # List all events
```

### Customer Interface APIs (`localhost:8081`)

#### **Customer Parcels** (Requires JWT with CUSTOMER role)
```bash
POST   /api/v1/customer/parcels/register     # Register new parcel
GET    /api/v1/customer/parcels/my           # Get my parcels
GET    /api/v1/customer/parcels/{id}/track   # Track my parcel
PUT    /api/v1/customer/parcels/{id}/cancel  # Cancel my parcel
GET    /api/v1/customer/parcels/history      # Get delivery history
```

### Example API Calls

#### **Create Customer**
```bash
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tech Solutions Inc",
    "email": "orders@techsolutions.com",
    "phone": "+1-555-0199",
    "addressLine1": "456 Tech Drive",
    "city": "San Francisco",
    "state": "CA",
    "postalCode": "94105",
    "country": "USA",
    "customerType": "BUSINESS"
  }'
```

#### **Submit EDI Order**
```bash
curl -X POST http://localhost:8080/api/v1/edi/orders \
  -H "Content-Type: application/json" \
  -d '{
    "ediReference": "EDI20240201",
    "sender": {
      "name": "Tech Solutions Inc",
      "email": "orders@techsolutions.com",
      "phone": "+1-555-0199"
    },
    "recipient": {
      "name": "John Customer",
      "phone": "+1-555-0200",
      "email": "john@example.com"
    },
    "package": {
      "weight": 2.5,
      "dimensions": {"length": 30, "width": 20, "height": 15},
      "value": 199.99
    },
    "service": {
      "priority": "EXPRESS",
      "insurance": true
    }
  }'
```

#### **Customer Parcel Registration**
```bash
curl -X POST http://localhost:8081/api/v1/customer/parcels/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "sender": {
      "name": "John Customer",
      "email": "john@example.com",
      "phone": "+1-555-0200"
    },
    "recipient": {
      "name": "Jane Recipient",
      "phone": "+1-555-0201",
      "email": "jane@example.com"
    },
    "package": {
      "weight": 1.5,
      "dimensions": {"length": 25, "width": 18, "height": 10},
      "value": 89.99
    },
    "addresses": {
      "pickup": {
        "line1": "123 Sender St",
        "city": "New York",
        "state": "NY",
        "postalCode": "10001"
      },
      "delivery": {
        "line1": "456 Recipient Ave",
        "city": "Brooklyn",
        "state": "NY", 
        "postalCode": "11201"
      }
    },
    "service": {
      "priority": "STANDARD",
      "insurance": false
    }
  }'
```

## 🐳 Docker Deployment

### Local Development
```bash
# Start all services
docker-compose -f docker-compose.microservices-updated.yml up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Production Deployment
```bash
# Build images
docker build -f Dockerfile.backend -t courier-main-service .
docker build -f Dockerfile.customer-interface -t courier-customer-interface ./customer-interface-service
docker build -f Dockerfile.frontend -t courier-frontend ./frontend

# Run with production profile
docker run -e SPRING_PROFILES_ACTIVE=production courier-main-service
```

## ☸️ Kubernetes Deployment

### Docker Hub Deployment (2 pods each)
```bash
# Update Docker Hub username
sed -i 's/YOUR_DOCKERHUB_USERNAME/your-username/g' k8s/dockerhub/*-deployment.yaml

# Deploy to Kubernetes
kubectl apply -f k8s/dockerhub/

# Check deployment
kubectl get pods -n courier-system
```

### Helm Deployment
```bash
# Install with Helm
helm install courier-system helm/courier-microservices \
  --set image.repository=your-dockerhub-username \
  --set image.tag=latest \
  --set replicaCount=2

# Upgrade deployment
helm upgrade courier-system helm/courier-microservices
```

### Access Services
```bash
# Port forwarding
kubectl port-forward svc/courier-main-service 8080:8080 -n courier-system
kubectl port-forward svc/courier-customer-interface 8081:8081 -n courier-system
kubectl port-forward svc/courier-frontend 3000:3000 -n courier-system

# Ingress (add to /etc/hosts)
# <INGRESS_IP> api.courier.local customer.courier.local app.courier.local
```

## 🔧 Configuration

### Application Profiles

#### **Local Development** (`application-local.yml`)
```yaml
spring:
  profiles:
    active: local
  cloud:
    stream:
      kafka:
        binder:
          brokers: localhost:9092
  datasource:
    url: jdbc:postgresql://localhost:5432/courier_db
    username: courier_user
    password: courier_pass
```

#### **Production** (`application-production.yml`)
```yaml
spring:
  profiles:
    active: production
  cloud:
    stream:
      bindings:
        parcelTrackingEvents-out-0:
          destination: parcel-tracking-events
          binder: eventhub
      binders:
        eventhub:
          type: eventhub
          environment:
            spring:
              cloud:
                azure:
                  eventhub:
                    connection-string: ${AZURE_EVENTHUB_CONNECTION_STRING}
```

### Environment Variables
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=courier_db
DB_USER=courier_user
DB_PASSWORD=courier_pass

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Azure Event Hub (Production)
AZURE_EVENTHUB_CONNECTION_STRING=Endpoint=sb://...

# JWT Security
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000

# Service URLs
MAIN_SERVICE_URL=http://localhost:8080
CUSTOMER_INTERFACE_URL=http://localhost:8081
```

## 🧪 Testing

### Unit Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests CustomerServiceTest

# Generate test report
./gradlew test jacocoTestReport
```

### Integration Tests
```bash
# Test with embedded Kafka
./gradlew integrationTest

# Test with TestContainers
./gradlew test --tests "*IntegrationTest"
```

### API Testing
```bash
# Test customer creation
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -d @test-data/customer.json

# Test parcel tracking
curl http://localhost:8080/api/v1/parcels/tracking/TRK001000001

# Test EDI submission
curl -X POST http://localhost:8080/api/v1/edi/orders \
  -H "Content-Type: application/json" \
  -d @test-data/edi-order.json
```

### Event Streaming Tests
```bash
# Start Kafka UI
http://localhost:8090

# Monitor topics
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic parcel-tracking-events --from-beginning

# Publish test event
kafka-console-producer --bootstrap-server localhost:9092 \
  --topic parcel-tracking-events
```

## 📊 Monitoring & Observability

### Health Checks
```bash
# Main Service
curl http://localhost:8080/actuator/health

# Customer Interface
curl http://localhost:8081/actuator/health

# Detailed health
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/kafka
```

### Metrics
```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Application metrics
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

### Logging
```yaml
# application.yml
logging:
  level:
    com.courier: DEBUG
    org.springframework.kafka: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
  file:
    name: logs/courier-service.log
```

## 🚨 Troubleshooting

### Common Issues

#### **Database Connection Failed**
```bash
# Check PostgreSQL status
docker ps | grep postgres
psql -h localhost -U courier_user -d courier_db -c "SELECT 1;"

# Reset database
docker-compose down -v
docker-compose up -d postgres
```

#### **Kafka Connection Issues**
```bash
# Check Kafka status
docker ps | grep kafka
kafka-topics --bootstrap-server localhost:9092 --list

# Reset Kafka
docker-compose down -v
docker-compose up -d zookeeper kafka
```

#### **Service Startup Failures**
```bash
# Check logs
./gradlew bootRun --debug
docker-compose logs courier-main-service

# Check port conflicts
netstat -an | grep 8080
lsof -i :8080
```

#### **JWT Authentication Issues**
```bash
# Generate test JWT token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass"}'

# Validate token
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8081/api/v1/customer/parcels/my
```

### Performance Tuning

#### **JVM Settings**
```bash
# For development
export JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"

# For production
export JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

#### **Database Optimization**
```sql
-- Create indexes for performance
CREATE INDEX idx_parcels_tracking_number ON parcels(tracking_number);
CREATE INDEX idx_parcels_status ON parcels(status);
CREATE INDEX idx_tracking_events_parcel_id ON parcel_tracking_events(parcel_id);
CREATE INDEX idx_edi_orders_status ON edi_parcel_orders(status);
```

## 🤝 Contributing

### Development Setup
```bash
# Fork and clone repository
git clone https://github.com/your-username/springboot-kafka.git
cd springboot-kafka

# Create feature branch
git checkout -b feature/your-feature-name

# Make changes and test
./gradlew test
./gradlew bootRun

# Commit and push
git commit -m "Add your feature"
git push origin feature/your-feature-name
```

### Code Standards
- Java 17+ features
- Spring Boot best practices
- RESTful API design
- Comprehensive testing
- Docker containerization
- Kubernetes deployment ready

### Pull Request Process
1. Ensure tests pass
2. Update documentation
3. Add integration tests
4. Update API documentation
5. Test Docker deployment

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Apache Kafka for event streaming capabilities
- PostgreSQL for reliable data persistence
- Docker for containerization
- Kubernetes for orchestration
- Azure Event Hub for production event streaming

---

## 📞 Support

For support and questions:
- Create an issue in the repository
- Check the troubleshooting section
- Review the API documentation
- Monitor application logs

**Happy Coding! 🚀**
