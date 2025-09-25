# Courier Management System - Eventing Platform

This document describes the eventing platform implementation for the Courier Management System, designed to work with both local Apache Kafka (for development/testing) and Azure Event Hub (for production).

## Architecture Overview

The eventing platform uses a dual approach:
- **Local Development**: Apache Kafka running in Docker containers
- **Production**: Azure Event Hub with Spring Cloud Stream binders
- **Compatibility**: Spring Cloud Stream abstracts the messaging implementation

## Event Flow

### Internal Events (Courier System)
1. **Parcel Registration** → `parcel-tracking-events` topic
2. **Status Updates** → `parcel-tracking-events` topic
3. **Internal Processing** → `courier-internal-events` topic

### External Events (ABC Transport Integration)
1. **Outbound**: Parcel information → `abc-transport-events` topic
2. **Inbound**: Status updates from ABC → `abc-transport-responses` topic

## Event Types

### 1. Parcel Events (`ParcelEvent`)
Base class for all parcel-related events with:
- Event ID and type
- Parcel ID and timestamp
- Source and version information
- Correlation ID for tracing
- Metadata for additional context

### 2. ABC Transport Events (`ABCTransportEvent`)
Events sent to ABC Transport system containing:
- Message type (PARCEL_REGISTRATION, etc.)
- Complete parcel information
- Customer and address details
- Service requirements

### 3. Tracking Events (`TrackingEventMessage`)
Internal tracking updates with:
- Tracking event type
- Location and timestamp
- Vehicle and driver information
- Status transitions

## Configuration Profiles

### Local Profile (`application-local.yml`)
- Uses Apache Kafka on localhost:9092
- Auto-creates topics
- Debug logging enabled
- Spring Cloud Stream configured for Kafka binder

### Production Profile (`application-production.yml`)
- Uses Azure Event Hub
- Manual checkpoint mode
- Production logging
- Metrics and monitoring enabled

## Topics Configuration

| Topic Name | Purpose | Partitions | Replication |
|------------|---------|------------|-------------|
| `incoming-parcel-orders` | EDI order intake | 3 | 1 (local) |
| `abc-transport-events` | Outbound to ABC Transport | 3 | 1 (local) |
| `abc-transport-responses` | Inbound from ABC Transport | 3 | 1 (local) |
| `parcel-tracking-events` | Internal tracking updates | 3 | 1 (local) |
| `courier-internal-events` | System internal events | 3 | 1 (local) |

## Services

### EventStreamService
- **Purpose**: Unified event publishing using Spring Cloud Stream
- **Methods**:
  - `sendToABCTransport(Parcel)`: Send parcel to ABC Transport
  - `sendTrackingEvent(TrackingEvent)`: Publish tracking updates
  - `sendEvent(String, Object, Map)`: Generic event publishing

### ABCTransportConsumerService
- **Purpose**: Process responses from ABC Transport system
- **Handles**:
  - Pickup scheduling
  - Status updates (picked up, in transit, delivered, etc.)
  - Delivery confirmations and failures
  - Return notifications

### KafkaProducerService (Legacy)
- **Purpose**: Direct Kafka integration (maintained for backward compatibility)
- **Will be phased out** in favor of EventStreamService

## Local Development Setup

### Prerequisites
- Docker Desktop
- Java 17+
- Gradle

### Quick Start
1. **Start Infrastructure**:
   ```bash
   # Windows
   start-local.bat
   
   # Or manually
   docker-compose up -d
   ```

2. **Verify Services**:
   - Kafka UI: http://localhost:8090
   - PostgreSQL: localhost:5432
   - Kafka: localhost:9092

3. **Start Application**:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

### Monitoring and Management

#### Kafka UI (http://localhost:8090)
- View topics and messages
- Monitor consumer groups
- Check partition distribution
- Debug message flow

#### Application Logs
```bash
# View all container logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f kafka
docker-compose logs -f postgres
```

## Production Deployment

### Azure Event Hub Configuration
1. **Set Environment Variables**:
   ```bash
   export AZURE_EVENTHUBS_CONNECTION_STRING="your-connection-string"
   export AZURE_EVENTHUBS_NAMESPACE="your-namespace"
   ```

2. **Deploy with Production Profile**:
   ```bash
   java -jar courier-management-system.jar --spring.profiles.active=production
   ```

### Event Hub Topics
Create the following Event Hubs in your Azure namespace:
- `incoming-parcel-orders`
- `abc-transport-events`
- `abc-transport-responses`
- `parcel-tracking-events`
- `courier-internal-events`

## Event Message Examples

### ABC Transport Event
```json
{
  "eventId": "uuid-here",
  "eventType": "ABC_TRANSPORT_EVENT",
  "parcelId": "PKG-1234567890-ABCD1234",
  "timestamp": "2024-01-15T10:30:00",
  "messageType": "PARCEL_REGISTRATION",
  "ediReference": "EDI-REF-123",
  "status": "REGISTERED",
  "priority": "STANDARD",
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
  }
}
```

### Tracking Event
```json
{
  "eventId": "uuid-here",
  "eventType": "TRACKING_EVENT",
  "parcelId": "PKG-1234567890-ABCD1234",
  "timestamp": "2024-01-15T14:30:00",
  "trackingEventType": "LOADED_IN_TRUCK",
  "description": "Parcel loaded in delivery truck",
  "eventTimestamp": "2024-01-15T14:30:00",
  "location": "Distribution Center NYC",
  "vehicleId": "TRUCK-001",
  "driverName": "Mike Johnson",
  "currentStatus": "LOADED_IN_TRUCK"
}
```

## Error Handling

### Retry Strategy
- **Producer**: 3 retries with exponential backoff
- **Consumer**: Manual acknowledgment with error handling
- **Dead Letter Queue**: Failed messages sent to DLQ (production)

### Monitoring
- **Health Checks**: Kafka/Event Hub connectivity
- **Metrics**: Message throughput, error rates
- **Alerts**: Failed deliveries, consumer lag

## Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests with Testcontainers
```bash
./gradlew integrationTest
```

### Manual Testing
1. Start local environment
2. Send EDI order via REST API
3. Monitor events in Kafka UI
4. Verify database updates

## Migration Strategy

### Phase 1: Dual Publishing (Current)
- Both KafkaProducerService and EventStreamService active
- Gradual migration of consumers

### Phase 2: Stream-Only
- Remove KafkaProducerService
- All events via Spring Cloud Stream

### Phase 3: Production Deployment
- Switch to Azure Event Hub
- Monitor and optimize performance

## Troubleshooting

### Common Issues

1. **Kafka Connection Failed**
   - Check Docker containers: `docker-compose ps`
   - Verify port 9092 is not in use
   - Restart containers: `docker-compose restart kafka`

2. **Topics Not Created**
   - Run topic initialization: `docker-compose up kafka-init`
   - Check Kafka UI for topic list

3. **Consumer Not Receiving Messages**
   - Verify consumer group configuration
   - Check topic partitions and consumer instances
   - Review application logs for errors

4. **Azure Event Hub Connection Issues**
   - Verify connection string and namespace
   - Check Azure Event Hub access policies
   - Ensure proper authentication

### Useful Commands

```bash
# View Kafka topics
docker exec -it courier-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Consume messages from topic
docker exec -it courier-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic parcel-tracking-events --from-beginning

# Produce test message
docker exec -it courier-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic abc-transport-events

# Check consumer group status
docker exec -it courier-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group courier-management-group
```

## Security Considerations

### Local Development
- No authentication required
- Network isolation via Docker

### Production
- Azure Event Hub authentication
- TLS encryption in transit
- Access policies and RBAC
- Network security groups

## Performance Tuning

### Kafka Configuration
- Adjust partition count based on throughput
- Configure appropriate replication factor
- Tune consumer concurrency

### Application Configuration
- Set appropriate batch sizes
- Configure connection pooling
- Monitor memory usage

## Future Enhancements

1. **Schema Registry**: Add Avro/JSON schema validation
2. **Event Sourcing**: Implement complete event sourcing pattern
3. **CQRS**: Separate command and query models
4. **Saga Pattern**: Implement distributed transactions
5. **Event Replay**: Add capability to replay events for recovery
