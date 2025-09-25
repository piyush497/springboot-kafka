-- Initialize the courier database with necessary extensions and configurations

-- Create extensions if they don't exist
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create indexes for better performance
-- These will be created automatically by Hibernate, but we can add custom ones here

-- Index for parcel lookups by customer
CREATE INDEX IF NOT EXISTS idx_parcel_sender_id ON parcel(sender_id);
CREATE INDEX IF NOT EXISTS idx_parcel_recipient_id ON parcel(recipient_id);

-- Index for tracking events by parcel
CREATE INDEX IF NOT EXISTS idx_tracking_event_parcel_id ON tracking_event(parcel_id);
CREATE INDEX IF NOT EXISTS idx_tracking_event_timestamp ON tracking_event(event_timestamp);

-- Index for customer lookups
CREATE INDEX IF NOT EXISTS idx_customer_email ON customer(email);
CREATE INDEX IF NOT EXISTS idx_customer_code ON customer(customer_code);

-- Index for parcel status and priority
CREATE INDEX IF NOT EXISTS idx_parcel_status ON parcel(status);
CREATE INDEX IF NOT EXISTS idx_parcel_priority ON parcel(priority);

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE courier_db TO courier_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO courier_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO courier_user;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO courier_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO courier_user;
