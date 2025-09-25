# Database Seeding Guide

This guide explains how to seed the Courier Management System database with approximately **400 rows** of realistic sample data for testing and development.

## Overview

The seeding process creates:
- **50 Customers** (mix of businesses and individuals)
- **200 Parcels** (various statuses and priorities)
- **100 EDI Orders** (processed and pending)
- **50 Tracking Events** (multi-stage parcel lifecycle)

**Total: ~400 records** representing a realistic courier management scenario.

## Data Structure

### Customers (50 records)
- **Business customers**: Logistics companies, corporations
- **Individual customers**: Personal senders and recipients
- **Realistic data**: Names, addresses, phone numbers, emails
- **Geographic distribution**: Major US cities and states

### Parcels (200 records)
- **Status distribution**:
  - 50 DELIVERED parcels (completed deliveries)
  - 30 OUT_FOR_DELIVERY parcels (final stage)
  - 40 LOADED_IN_TRUCK parcels (ready for delivery)
  - 40 IN_TRANSIT parcels (moving between facilities)
  - 30 PICKED_UP parcels (collected from senders)
  - 10 REGISTERED parcels (newly created)

- **Priority levels**: STANDARD, EXPRESS, PRIORITY
- **Package details**: Weight, dimensions, declared value, insurance
- **Addresses**: Realistic pickup and delivery locations
- **Special instructions**: Fragile handling, signature requirements, etc.

### EDI Orders (100 records)
- **80 PROCESSED orders**: Successfully processed EDI submissions
- **20 PENDING orders**: Awaiting processing or validation
- **JSON payloads**: Complete EDI order data in JSON format
- **Error handling**: Some orders with validation warnings

### Tracking Events (50 records)
Supporting the multi-stage tracking system from the eventing platform:
```
registered → picked up → in transit → loaded in truck → out for delivery → delivered
```

- **Event types**: PARCEL_REGISTERED, PICKED_UP, IN_TRANSIT, LOADED_IN_TRUCK, OUT_FOR_DELIVERY, DELIVERED
- **Locations**: Geographic tracking through delivery network
- **Timestamps**: Realistic progression through delivery stages
- **Descriptions**: Human-readable event descriptions

## Seeding Methods

### Method 1: Automated Scripts (Recommended)

#### **Windows**
```bash
# Run the seeding script
scripts\run-seed-data.bat

# With custom database settings
set DB_HOST=your-host
set DB_PASSWORD=your-password
scripts\run-seed-data.bat
```

#### **Linux/macOS**
```bash
# Make script executable
chmod +x scripts/run-seed-data.sh

# Run the seeding script
./scripts/run-seed-data.sh

# With custom database settings
DB_HOST=your-host DB_PASSWORD=your-password ./scripts/run-seed-data.sh
```

### Method 2: Spring Boot Service (Automatic)

The `DataSeedingService` automatically runs on application startup:

```java
@Service
public class DataSeedingService implements CommandLineRunner {
    // Automatically seeds data if database is empty
}
```

**To enable automatic seeding:**
```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # Recreates schema on startup
```

### Method 3: Manual SQL Execution

Execute SQL files individually:

```bash
# 1. Seed customers
psql -h localhost -U courier_user -d courier_db -f scripts/seed-data.sql

# 2. Seed parcels
psql -h localhost -U courier_user -d courier_db -f scripts/seed-parcels.sql

# 3. Seed EDI orders
psql -h localhost -U courier_user -d courier_db -f scripts/seed-edi-orders.sql

# 4. Seed tracking events
psql -h localhost -U courier_user -d courier_db -f scripts/seed-tracking-events.sql
```

## Configuration

### Database Settings

Default configuration:
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=courier_db
DB_USER=courier_user
DB_PASSWORD=courier_pass
```

### Environment Variables

Override defaults using environment variables:
```bash
export DB_HOST=your-database-host
export DB_PORT=5432
export DB_NAME=your-database-name
export DB_USER=your-username
export DB_PASSWORD=your-password
```

## Verification

### Check Record Counts
```bash
# Using the script
./scripts/run-seed-data.sh check

# Manual SQL
psql -h localhost -U courier_user -d courier_db -c "
SELECT 
  (SELECT COUNT(*) FROM customers) as customers,
  (SELECT COUNT(*) FROM parcels) as parcels,
  (SELECT COUNT(*) FROM edi_parcel_orders) as edi_orders,
  (SELECT COUNT(*) FROM parcel_tracking_events) as tracking_events;
"
```

### Sample Data Preview
```bash
# Using the script
./scripts/run-seed-data.sh sample

# Recent customers
SELECT id, name, customer_type, city, state 
FROM customers 
ORDER BY created_at DESC 
LIMIT 5;

# Parcel status distribution
SELECT status, COUNT(*) as count 
FROM parcels 
GROUP BY status 
ORDER BY count DESC;
```

## Event Streaming Integration

The seeded data integrates with the implemented eventing platform:

### Kafka Topics
Based on the eventing platform, the following topics will receive events:

- **parcel-tracking-events**: Internal tracking events
- **courier-internal-events**: System events
- **abc-transport-events**: External transport events
- **abc-transport-responses**: Transport system responses

### Event Flow Testing
```bash
# Start Kafka and services
./start-local.bat

# Monitor events in Kafka UI
http://localhost:8090

# Create test events
./scripts/run-seed-data.sh events
```

## API Testing

Once data is seeded, test the APIs:

### Customer APIs
```bash
# Get all customers
curl http://localhost:8080/api/v1/customers

# Get customer by ID
curl http://localhost:8080/api/v1/customers/1

# Search customers
curl "http://localhost:8080/api/v1/customers/search?name=Acme"
```

### Parcel APIs
```bash
# Get all parcels
curl http://localhost:8080/api/v1/parcels

# Get parcel by tracking number
curl http://localhost:8080/api/v1/parcels/TRK001000001

# Get parcels by status
curl "http://localhost:8080/api/v1/parcels/status/DELIVERED"
```

### EDI APIs
```bash
# Get all EDI orders
curl http://localhost:8080/api/v1/edi/orders

# Get EDI order by reference
curl http://localhost:8080/api/v1/edi/orders/EDI20240001

# Get pending EDI orders
curl "http://localhost:8080/api/v1/edi/orders/status/PENDING"
```

### Tracking APIs
```bash
# Get tracking events for a parcel
curl http://localhost:8080/api/v1/parcels/1/tracking

# Get all tracking events
curl http://localhost:8080/api/v1/tracking/events
```

## Customer Interface Testing

Test the separate customer interface microservice:

```bash
# Customer parcel registration
curl -X POST http://localhost:8081/api/v1/customer/parcels/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "sender": {"name": "Test Customer", "email": "test@example.com"},
    "recipient": {"name": "Test Recipient", "phone": "+1-555-0123"},
    "package": {"weight": 2.5, "value": 100.00}
  }'

# Get customer parcels
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8081/api/v1/customer/parcels/my

# Track a parcel
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8081/api/v1/customer/parcels/TRK001000001/track
```

## Troubleshooting

### Common Issues

#### 1. Database Connection Failed
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Test connection manually
psql -h localhost -U courier_user -d courier_db -c "SELECT 1;"
```

#### 2. Tables Don't Exist
```bash
# Run schema creation first
./gradlew flywayMigrate

# Or start the application to auto-create tables
./gradlew bootRun
```

#### 3. Data Already Exists
```bash
# Clear existing data
psql -h localhost -U courier_user -d courier_db -c "
TRUNCATE TABLE parcel_tracking_events CASCADE;
TRUNCATE TABLE edi_parcel_orders CASCADE;
TRUNCATE TABLE parcels CASCADE;
TRUNCATE TABLE customers CASCADE;
"

# Then re-run seeding
./scripts/run-seed-data.sh
```

#### 4. Permission Denied
```bash
# Make script executable (Linux/macOS)
chmod +x scripts/run-seed-data.sh

# Check database permissions
psql -h localhost -U courier_user -d courier_db -c "
SELECT current_user, session_user;
"
```

### Script Options

```bash
# Available commands
./scripts/run-seed-data.sh help

# Commands:
# seed    - Seed the database with sample data (default)
# check   - Check database connectivity and record counts
# sample  - Show sample data from database
# events  - Create test events for Kafka streaming
```

## Performance Considerations

### Large Dataset Seeding
For larger datasets:

```sql
-- Disable constraints temporarily
ALTER TABLE parcels DISABLE TRIGGER ALL;
ALTER TABLE parcel_tracking_events DISABLE TRIGGER ALL;

-- Insert data in batches
INSERT INTO parcels (...) VALUES (...);

-- Re-enable constraints
ALTER TABLE parcels ENABLE TRIGGER ALL;
ALTER TABLE parcel_tracking_events ENABLE TRIGGER ALL;

-- Update statistics
ANALYZE customers;
ANALYZE parcels;
ANALYZE edi_parcel_orders;
ANALYZE parcel_tracking_events;
```

### Memory Optimization
```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
```

## Production Considerations

### Security
- **Never use seeded data in production**
- **Change default passwords and secrets**
- **Use environment-specific configurations**

### Data Privacy
- **Anonymize customer data**
- **Use fake email addresses and phone numbers**
- **Comply with data protection regulations**

### Backup
```bash
# Backup seeded data
pg_dump -h localhost -U courier_user courier_db > courier_seed_backup.sql

# Restore from backup
psql -h localhost -U courier_user -d courier_db < courier_seed_backup.sql
```

The database seeding provides a comprehensive foundation for testing the Courier Management System, including the event-driven architecture, multi-stage parcel tracking, and customer interface functionality.
