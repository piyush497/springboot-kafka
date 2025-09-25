# EDI Parcel Processing Flow

This document describes the complete flow for processing EDI parcel orders, saving them to the database using Spring Data Repository, and publishing events to the ABC Transport topic.

## Architecture Overview

```
EDI JSON Payload → Kafka Topic → Consumer Service → Database → ABC Transport Event
```

## Components

### 1. EDIParcelConsumerService
**Purpose**: Consumes EDI parcel orders from Kafka and processes them

**Key Features**:
- Kafka listener for `incoming-parcel-orders` topic
- Spring Cloud Stream consumer function
- Comprehensive validation
- Database persistence via ParcelService
- Event publishing to ABC Transport topic

### 2. EDIParcelController
**Purpose**: REST API for EDI order submission and status checking

**Endpoints**:
- `POST /api/v1/edi/process` - Direct synchronous processing
- `POST /api/v1/edi/submit` - Asynchronous processing via Kafka
- `GET /api/v1/edi/status/{ediReference}` - Status lookup
- `GET /api/v1/edi/sample` - Sample EDI order for testing

### 3. ParcelService (Enhanced)
**Purpose**: Business logic for parcel management

**New Methods**:
- `findByEdiReference(String ediReference)` - Find parcel by EDI reference
- Enhanced `createParcelFromEDI()` with BigDecimal weight conversion

## Processing Flow

### 1. EDI Order Reception
```json
{
  "ediReference": "EDI-2024-001",
  "sender": {
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+1234567890",
    "customerCode": "SENDER-001"
  },
  "recipient": {
    "name": "Jane Smith",
    "email": "jane@example.com",
    "phone": "+0987654321",
    "customerCode": "RECIPIENT-001"
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

### 2. Validation Process
The system validates:
- **Required Fields**: EDI reference, sender, recipient, addresses
- **Customer Data**: Name, email format, contact information
- **Address Data**: Street address, city, postal code, country
- **Data Integrity**: Non-null values, proper formatting

### 3. Database Persistence
**Steps**:
1. **Customer Resolution**: Find or create sender and recipient customers
2. **Address Creation**: Create pickup and delivery address entities
3. **Parcel Creation**: Generate parcel ID and create parcel entity
4. **Weight Conversion**: Convert Double to BigDecimal for database storage
5. **Status Setting**: Set initial status to `REGISTERED`
6. **Tracking Event**: Create initial tracking event
7. **Database Save**: Persist all entities using `@Transactional`

**Database Schema**:
```sql
-- Parcel entity saved to 'parcels' table
INSERT INTO parcels (
    parcel_id, sender_id, recipient_id, 
    pickup_address_id, delivery_address_id,
    description, weight, dimensions, status, priority,
    edi_reference, created_at, updated_at
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

### 4. Event Publishing to ABC Transport
**Event Structure**:
```json
{
  "eventId": "uuid-generated",
  "eventType": "ABC_TRANSPORT_EVENT",
  "parcelId": "PKG-1234567890-ABCD1234",
  "timestamp": "2024-01-15T10:30:00",
  "messageType": "PARCEL_REGISTRATION",
  "ediReference": "EDI-2024-001",
  "status": "REGISTERED",
  "priority": "STANDARD",
  "sender": { "name": "John Doe", "email": "john@example.com", "phone": "+1234567890" },
  "recipient": { "name": "Jane Smith", "email": "jane@example.com", "phone": "+0987654321" },
  "pickupAddress": { "streetAddress": "123 Main St", "city": "New York", "state": "NY", "postalCode": "10001", "country": "USA" },
  "deliveryAddress": { "streetAddress": "456 Oak Ave", "city": "Los Angeles", "state": "CA", "postalCode": "90001", "country": "USA" },
  "parcelDetails": { "description": "Sample package", "weight": 2.5, "dimensions": "30x20x15 cm" },
  "correlationId": "PKG-1234567890-ABCD1234-1705312200000"
}
```

## Usage Examples

### 1. Direct Processing (Synchronous)
```bash
curl -X POST http://localhost:8080/api/v1/edi/process \
  -H "Content-Type: application/json" \
  -d @sample-edi-order.json
```

**Response**:
```json
{
  "success": true,
  "message": "EDI order processed successfully",
  "parcelId": "PKG-1705312200000-ABCD1234",
  "ediReference": "EDI-2024-001",
  "status": "REGISTERED",
  "createdAt": "2024-01-15T10:30:00"
}
```

### 2. Asynchronous Processing via Kafka
```bash
curl -X POST http://localhost:8080/api/v1/edi/submit \
  -H "Content-Type: application/json" \
  -d @sample-edi-order.json
```

**Response**:
```json
{
  "success": true,
  "message": "EDI order submitted for processing",
  "ediReference": "EDI-2024-001",
  "topic": "incoming-parcel-orders",
  "status": "SUBMITTED"
}
```

### 3. Status Lookup
```bash
curl -X GET http://localhost:8080/api/v1/edi/status/EDI-2024-001
```

**Response**:
```json
{
  "success": true,
  "message": "Parcel found",
  "ediReference": "EDI-2024-001",
  "parcelId": "PKG-1705312200000-ABCD1234",
  "status": "REGISTERED",
  "priority": "STANDARD",
  "createdAt": "2024-01-15T10:30:00",
  "sender": { "name": "John Doe", "email": "john@example.com" },
  "recipient": { "name": "Jane Smith", "email": "jane@example.com" }
}
```

## Configuration

### Kafka Topics
```yaml
kafka:
  topics:
    incoming-orders: "incoming-parcel-orders"
    abc-transport: "abc-transport-events"
    tracking-events: "parcel-tracking-events"
```

### Spring Cloud Stream Bindings
```yaml
spring:
  cloud:
    stream:
      bindings:
        ediParcelOrders-in-0:
          destination: incoming-parcel-orders
          content-type: application/json
          group: courier-edi-group
        abcTransport-out-0:
          destination: abc-transport-events
          content-type: application/json
```

## Error Handling

### Validation Errors
- **Missing Required Fields**: Returns `IllegalArgumentException`
- **Invalid Email Format**: Returns `IllegalArgumentException`
- **Invalid Data Types**: Returns JSON parsing errors

### Processing Errors
- **Database Errors**: Transaction rollback, error logging
- **Kafka Publishing Errors**: Retry mechanism, dead letter queue
- **Duplicate EDI References**: Handled by unique constraints

### Error Response Format
```json
{
  "success": false,
  "message": "Failed to process EDI order: Sender email is required",
  "error": "IllegalArgumentException"
}
```

## Monitoring and Observability

### Logging
```java
logger.info("Processing EDI order with reference: {}", ediOrder.getEdiReference());
logger.info("Successfully saved parcel with ID: {} to database", savedParcel.getParcelId());
logger.info("Successfully published parcel {} to ABC Transport topic", savedParcel.getParcelId());
```

### Metrics
- EDI orders processed per minute
- Database save success/failure rates
- ABC Transport event publishing success rates
- Processing latency metrics

### Health Checks
```bash
curl -X GET http://localhost:8080/api/v1/edi/health
```

## Testing

### Unit Tests
- `EDIParcelConsumerServiceTest`: Service layer testing
- `EDIParcelControllerTest`: REST API testing

### Integration Tests
- `EDIParcelProcessingIntegrationTest`: End-to-end flow testing
- Embedded Kafka for realistic testing
- Database integration with H2

### Manual Testing
1. **Start Local Environment**:
   ```bash
   start-local.bat
   ```

2. **Get Sample EDI Order**:
   ```bash
   curl -X GET http://localhost:8080/api/v1/edi/sample
   ```

3. **Submit for Processing**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/edi/submit \
     -H "Content-Type: application/json" \
     -d @sample-order.json
   ```

4. **Monitor in Kafka UI**: http://localhost:8090

## Performance Considerations

### Database Optimization
- Indexes on `edi_reference`, `parcel_id`, `status`
- Connection pooling configuration
- Transaction management

### Kafka Optimization
- Partition strategy by EDI reference
- Consumer group configuration
- Batch processing for high throughput

### Caching Strategy
- Customer lookup caching
- Address validation caching
- EDI reference duplicate checking

## Security Considerations

### Input Validation
- JSON schema validation
- SQL injection prevention
- XSS protection

### Authentication & Authorization
- API key authentication
- Role-based access control
- Rate limiting

### Data Privacy
- PII data encryption
- Audit logging
- GDPR compliance

## Future Enhancements

1. **Schema Registry**: Add Avro schema validation
2. **Dead Letter Queue**: Implement failed message handling
3. **Retry Mechanism**: Exponential backoff for failures
4. **Bulk Processing**: Batch EDI order processing
5. **Real-time Notifications**: WebSocket updates for status changes
6. **Analytics Dashboard**: EDI processing metrics and insights
