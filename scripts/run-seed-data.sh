#!/bin/bash

# Script to seed the Courier Management System database with sample data
# This script creates approximately 400 rows of realistic data

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Database configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-courier_db}
DB_USER=${DB_USER:-courier_user}
DB_PASSWORD=${DB_PASSWORD:-courier_pass}

echo -e "${BLUE}=== Courier Management System - Database Seeding ===${NC}"
echo -e "${BLUE}Database: $DB_HOST:$DB_PORT/$DB_NAME${NC}"
echo -e "${BLUE}User: $DB_USER${NC}"
echo ""

# Function to check if PostgreSQL is available
check_database() {
    echo -e "${YELLOW}Checking database connectivity...${NC}"
    
    if ! command -v psql &> /dev/null; then
        echo -e "${RED}Error: psql is not installed${NC}"
        exit 1
    fi
    
    # Test connection
    if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1;" &> /dev/null; then
        echo -e "${GREEN}✓ Database connection successful${NC}"
    else
        echo -e "${RED}Error: Cannot connect to database${NC}"
        echo "Please ensure PostgreSQL is running and credentials are correct"
        exit 1
    fi
}

# Function to check if data already exists
check_existing_data() {
    echo -e "${YELLOW}Checking for existing data...${NC}"
    
    local customer_count=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM customers;" 2>/dev/null | xargs)
    local parcel_count=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM parcels;" 2>/dev/null | xargs)
    
    echo "Current data: $customer_count customers, $parcel_count parcels"
    
    if [[ $customer_count -gt 0 || $parcel_count -gt 0 ]]; then
        echo -e "${YELLOW}Warning: Database already contains data${NC}"
        read -p "Do you want to continue and add more data? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Seeding cancelled"
            exit 0
        fi
    fi
}

# Function to execute SQL file
execute_sql_file() {
    local file=$1
    local description=$2
    
    echo -e "${YELLOW}$description...${NC}"
    
    if [[ ! -f "$file" ]]; then
        echo -e "${RED}Error: SQL file $file not found${NC}"
        return 1
    fi
    
    if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$file" &> /dev/null; then
        echo -e "${GREEN}✓ $description completed${NC}"
        return 0
    else
        echo -e "${RED}✗ $description failed${NC}"
        return 1
    fi
}

# Function to get record counts
get_record_counts() {
    echo -e "${YELLOW}Getting record counts...${NC}"
    
    local customers=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM customers;" | xargs)
    local parcels=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM parcels;" | xargs)
    local edi_orders=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM edi_parcel_orders;" | xargs)
    local tracking_events=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM parcel_tracking_events;" | xargs)
    
    echo -e "${BLUE}Record counts:${NC}"
    echo "  Customers: $customers"
    echo "  Parcels: $parcels"
    echo "  EDI Orders: $edi_orders"
    echo "  Tracking Events: $tracking_events"
    echo "  Total Records: $((customers + parcels + edi_orders + tracking_events))"
}

# Function to show sample data
show_sample_data() {
    echo -e "${YELLOW}Sample data preview:${NC}"
    
    echo -e "${BLUE}Recent customers:${NC}"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "
        SELECT id, name, customer_type, city, state 
        FROM customers 
        ORDER BY created_at DESC 
        LIMIT 5;
    " 2>/dev/null
    
    echo -e "${BLUE}Recent parcels:${NC}"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "
        SELECT tracking_number, status, priority, delivery_city, delivery_state 
        FROM parcels 
        ORDER BY created_at DESC 
        LIMIT 5;
    " 2>/dev/null
    
    echo -e "${BLUE}Parcel status distribution:${NC}"
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "
        SELECT status, COUNT(*) as count 
        FROM parcels 
        GROUP BY status 
        ORDER BY count DESC;
    " 2>/dev/null
}

# Function to create test data for event streaming
create_test_events() {
    echo -e "${YELLOW}Creating test events for Kafka streaming...${NC}"
    
    # Create a simple SQL script to generate some recent events
    cat > /tmp/test_events.sql << EOF
-- Insert some recent tracking events for testing event streaming
INSERT INTO parcel_tracking_events (parcel_id, event_type, status, location, description, event_timestamp, created_at) 
SELECT 
    p.id,
    'STATUS_UPDATE',
    CASE 
        WHEN p.status = 'REGISTERED' THEN 'PICKED_UP'
        WHEN p.status = 'PICKED_UP' THEN 'IN_TRANSIT'
        WHEN p.status = 'IN_TRANSIT' THEN 'LOADED_IN_TRUCK'
        WHEN p.status = 'LOADED_IN_TRUCK' THEN 'OUT_FOR_DELIVERY'
        ELSE p.status
    END,
    p.delivery_city || ', ' || p.delivery_state,
    'Automated status update for testing',
    NOW(),
    NOW()
FROM parcels p 
WHERE p.status IN ('REGISTERED', 'PICKED_UP', 'IN_TRANSIT', 'LOADED_IN_TRUCK')
AND p.id <= 200
LIMIT 10;
EOF
    
    if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f /tmp/test_events.sql &> /dev/null; then
        echo -e "${GREEN}✓ Test events created for Kafka streaming${NC}"
        rm -f /tmp/test_events.sql
    else
        echo -e "${YELLOW}⚠ Test events creation skipped${NC}"
        rm -f /tmp/test_events.sql
    fi
}

# Main execution
main() {
    check_database
    check_existing_data
    
    echo -e "${YELLOW}Starting database seeding...${NC}"
    
    # Seed customers (50 records)
    if execute_sql_file "scripts/seed-data.sql" "Seeding customers"; then
        echo -e "${GREEN}✓ 50 customers created${NC}"
    else
        echo -e "${RED}Failed to create customers${NC}"
        exit 1
    fi
    
    # Seed parcels (200 records)
    if execute_sql_file "scripts/seed-parcels.sql" "Seeding parcels"; then
        echo -e "${GREEN}✓ 200 parcels created${NC}"
    else
        echo -e "${RED}Failed to create parcels${NC}"
        exit 1
    fi
    
    # Seed EDI orders (100 records)
    if execute_sql_file "scripts/seed-edi-orders.sql" "Seeding EDI orders"; then
        echo -e "${GREEN}✓ 100 EDI orders created${NC}"
    else
        echo -e "${RED}Failed to create EDI orders${NC}"
        exit 1
    fi
    
    # Seed tracking events (50 records)
    if execute_sql_file "scripts/seed-tracking-events.sql" "Seeding tracking events"; then
        echo -e "${GREEN}✓ 50 tracking events created${NC}"
    else
        echo -e "${RED}Failed to create tracking events${NC}"
        exit 1
    fi
    
    # Create additional test events
    create_test_events
    
    echo ""
    echo -e "${GREEN}=== Database Seeding Completed Successfully! ===${NC}"
    
    get_record_counts
    echo ""
    show_sample_data
    
    echo ""
    echo -e "${GREEN}=== Next Steps ===${NC}"
    echo -e "${BLUE}1. Start the application:${NC} ./gradlew bootRun"
    echo -e "${BLUE}2. Test APIs:${NC}"
    echo "   - GET http://localhost:8080/api/v1/customers"
    echo "   - GET http://localhost:8080/api/v1/parcels"
    echo "   - GET http://localhost:8080/api/v1/edi/orders"
    echo -e "${BLUE}3. Monitor Kafka events:${NC} http://localhost:8090 (Kafka UI)"
    echo -e "${BLUE}4. Test event streaming:${NC} The system will process tracking events automatically"
}

# Handle script arguments
case "${1:-seed}" in
    "seed")
        main
        ;;
    "check")
        check_database
        get_record_counts
        ;;
    "sample")
        check_database
        show_sample_data
        ;;
    "events")
        check_database
        create_test_events
        ;;
    "help"|"-h"|"--help")
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  seed    - Seed the database with sample data (default)"
        echo "  check   - Check database connectivity and record counts"
        echo "  sample  - Show sample data from database"
        echo "  events  - Create test events for Kafka streaming"
        echo "  help    - Show this help message"
        echo ""
        echo "Environment Variables:"
        echo "  DB_HOST     - Database host (default: localhost)"
        echo "  DB_PORT     - Database port (default: 5432)"
        echo "  DB_NAME     - Database name (default: courier_db)"
        echo "  DB_USER     - Database user (default: courier_user)"
        echo "  DB_PASSWORD - Database password (default: courier_pass)"
        ;;
    *)
        echo -e "${RED}Unknown command: $1${NC}"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac
