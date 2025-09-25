-- Insert 50 sample tracking events for various parcels
INSERT INTO parcel_tracking_events (id, parcel_id, event_type, status, location, description, event_timestamp, created_at) VALUES

-- Events for delivered parcels
(1, 1, 'PARCEL_REGISTERED', 'REGISTERED', 'New York, NY', 'Parcel registered in system', '2024-01-10 09:00:00', '2024-01-10 09:00:00'),
(2, 1, 'PICKED_UP', 'PICKED_UP', 'New York, NY', 'Parcel picked up from sender', '2024-01-10 14:30:00', '2024-01-10 14:30:00'),
(3, 1, 'IN_TRANSIT', 'IN_TRANSIT', 'Newark, NJ', 'Parcel in transit to destination', '2024-01-11 08:15:00', '2024-01-11 08:15:00'),
(4, 1, 'LOADED_IN_TRUCK', 'LOADED_IN_TRUCK', 'Brooklyn, NY', 'Parcel loaded in delivery truck', '2024-01-14 09:30:00', '2024-01-14 09:30:00'),
(5, 1, 'OUT_FOR_DELIVERY', 'OUT_FOR_DELIVERY', 'Brooklyn, NY', 'Parcel out for delivery', '2024-01-14 11:00:00', '2024-01-14 11:00:00'),
(6, 1, 'DELIVERED', 'DELIVERED', 'Brooklyn, NY', 'Parcel delivered successfully', '2024-01-14 16:30:00', '2024-01-14 16:30:00'),

(7, 2, 'PARCEL_REGISTERED', 'REGISTERED', 'Los Angeles, CA', 'Parcel registered in system', '2024-01-08 14:30:00', '2024-01-08 14:30:00'),
(8, 2, 'PICKED_UP', 'PICKED_UP', 'Los Angeles, CA', 'Express pickup completed', '2024-01-08 16:45:00', '2024-01-08 16:45:00'),
(9, 2, 'IN_TRANSIT', 'IN_TRANSIT', 'Los Angeles, CA', 'Parcel in express transit', '2024-01-09 06:00:00', '2024-01-09 06:00:00'),
(10, 2, 'DELIVERED', 'DELIVERED', 'Pasadena, CA', 'Express delivery completed', '2024-01-11 11:45:00', '2024-01-11 11:45:00'),

(11, 3, 'PARCEL_REGISTERED', 'REGISTERED', 'San Francisco, CA', 'Priority parcel registered', '2024-01-12 10:15:00', '2024-01-12 10:15:00'),
(12, 3, 'PICKED_UP', 'PICKED_UP', 'San Francisco, CA', 'Priority pickup completed', '2024-01-12 15:20:00', '2024-01-12 15:20:00'),
(13, 3, 'ARRIVED_AT_FACILITY', 'IN_TRANSIT', 'Oakland, CA', 'Arrived at sorting facility', '2024-01-13 08:30:00', '2024-01-13 08:30:00'),
(14, 3, 'OUT_FOR_DELIVERY', 'OUT_FOR_DELIVERY', 'San Jose, CA', 'Out for priority delivery', '2024-01-17 10:00:00', '2024-01-17 10:00:00'),
(15, 3, 'DELIVERED', 'DELIVERED', 'San Jose, CA', 'Electronics delivered safely', '2024-01-17 14:20:00', '2024-01-17 14:20:00'),

-- Events for out for delivery parcels
(16, 51, 'PARCEL_REGISTERED', 'REGISTERED', 'Portland, OR', 'Gift parcel registered', '2024-02-10 10:30:00', '2024-02-10 10:30:00'),
(17, 51, 'PICKED_UP', 'PICKED_UP', 'Portland, OR', 'Gift pickup completed', '2024-02-10 16:15:00', '2024-02-10 16:15:00'),
(18, 51, 'IN_TRANSIT', 'IN_TRANSIT', 'Portland, OR', 'En route to Salem', '2024-02-11 07:45:00', '2024-02-11 07:45:00'),
(19, 51, 'LOADED_IN_TRUCK', 'LOADED_IN_TRUCK', 'Salem, OR', 'Loaded for local delivery', '2024-02-14 08:30:00', '2024-02-14 08:30:00'),
(20, 51, 'OUT_FOR_DELIVERY', 'OUT_FOR_DELIVERY', 'Salem, OR', 'Out for delivery today', '2024-02-14 13:45:00', '2024-02-14 13:45:00'),

(21, 52, 'PARCEL_REGISTERED', 'REGISTERED', 'Las Vegas, NV', 'Sports equipment registered', '2024-02-11 14:15:00', '2024-02-11 14:15:00'),
(22, 52, 'PICKED_UP', 'PICKED_UP', 'Las Vegas, NV', 'Priority pickup completed', '2024-02-11 17:30:00', '2024-02-11 17:30:00'),
(23, 52, 'ARRIVED_AT_FACILITY', 'IN_TRANSIT', 'Las Vegas, NV', 'Processing at facility', '2024-02-12 09:00:00', '2024-02-12 09:00:00'),
(24, 52, 'OUT_FOR_DELIVERY', 'OUT_FOR_DELIVERY', 'Henderson, NV', 'Priority delivery in progress', '2024-02-14 16:20:00', '2024-02-14 16:20:00'),

-- Events for loaded in truck parcels
(25, 81, 'PARCEL_REGISTERED', 'REGISTERED', 'Kansas City, MO', 'Office supplies registered', '2024-02-13 14:40:00', '2024-02-13 14:40:00'),
(26, 81, 'PICKED_UP', 'PICKED_UP', 'Kansas City, MO', 'Standard pickup completed', '2024-02-13 18:20:00', '2024-02-13 18:20:00'),
(27, 81, 'IN_TRANSIT', 'IN_TRANSIT', 'Kansas City, MO', 'Interstate transport', '2024-02-14 06:15:00', '2024-02-14 06:15:00'),
(28, 81, 'LOADED_IN_TRUCK', 'LOADED_IN_TRUCK', 'Topeka, KS', 'Loaded for final delivery', '2024-02-14 16:15:00', '2024-02-14 16:15:00'),

(29, 82, 'PARCEL_REGISTERED', 'REGISTERED', 'Salt Lake City, UT', 'Outdoor gear registered', '2024-02-14 10:50:00', '2024-02-14 10:50:00'),
(30, 82, 'PICKED_UP', 'PICKED_UP', 'Salt Lake City, UT', 'Express pickup completed', '2024-02-14 11:45:00', '2024-02-14 11:45:00'),
(31, 82, 'LOADED_IN_TRUCK', 'LOADED_IN_TRUCK', 'Provo, UT', 'Express truck loading', '2024-02-14 12:30:00', '2024-02-14 12:30:00'),

-- Events for in transit parcels
(32, 121, 'PARCEL_REGISTERED', 'REGISTERED', 'Omaha, NE', 'Books parcel registered', '2024-02-15 09:20:00', '2024-02-15 09:20:00'),
(33, 121, 'PICKED_UP', 'PICKED_UP', 'Omaha, NE', 'Standard pickup completed', '2024-02-15 10:45:00', '2024-02-15 10:45:00'),
(34, 121, 'IN_TRANSIT', 'IN_TRANSIT', 'Omaha, NE', 'Interstate transport to Lincoln', '2024-02-15 11:30:00', '2024-02-15 11:30:00'),

(35, 122, 'PARCEL_REGISTERED', 'REGISTERED', 'Tulsa, OK', 'Clothing parcel registered', '2024-02-15 13:45:00', '2024-02-15 13:45:00'),
(36, 122, 'PICKED_UP', 'PICKED_UP', 'Tulsa, OK', 'Priority pickup completed', '2024-02-15 14:30:00', '2024-02-15 14:30:00'),
(37, 122, 'IN_TRANSIT', 'IN_TRANSIT', 'Oklahoma City, OK', 'Priority transport in progress', '2024-02-15 15:20:00', '2024-02-15 15:20:00'),

-- Events for picked up parcels
(38, 161, 'PARCEL_REGISTERED', 'REGISTERED', 'Tucson, AZ', 'Electronics registered', '2024-02-16 08:15:00', '2024-02-16 08:15:00'),
(39, 161, 'PICKED_UP', 'PICKED_UP', 'Tucson, AZ', 'Express electronics pickup', '2024-02-16 10:45:00', '2024-02-16 10:45:00'),

(40, 162, 'PARCEL_REGISTERED', 'REGISTERED', 'Colorado Springs, CO', 'Home goods registered', '2024-02-16 12:30:00', '2024-02-16 12:30:00'),
(41, 162, 'PICKED_UP', 'PICKED_UP', 'Colorado Springs, CO', 'Priority home goods pickup', '2024-02-16 14:15:00', '2024-02-16 14:15:00'),

-- Events for registered parcels
(42, 191, 'PARCEL_REGISTERED', 'REGISTERED', 'Corpus Christi, TX', 'Documents registered for standard delivery', '2024-02-16 16:00:00', '2024-02-16 16:00:00'),

(43, 192, 'PARCEL_REGISTERED', 'REGISTERED', 'Reno, NV', 'Gift items registered for express delivery', '2024-02-16 17:30:00', '2024-02-16 17:30:00'),

-- Additional tracking events for various parcels
(44, 4, 'PARCEL_REGISTERED', 'REGISTERED', 'Chicago, IL', 'Birthday gift registered', '2024-01-15 16:45:00', '2024-01-15 16:45:00'),
(45, 4, 'PICKED_UP', 'PICKED_UP', 'Chicago, IL', 'Gift pickup completed', '2024-01-16 09:30:00', '2024-01-16 09:30:00'),
(46, 4, 'DELIVERED', 'DELIVERED', 'Evanston, IL', 'Birthday gift delivered', '2024-01-19 10:30:00', '2024-01-19 10:30:00'),

(47, 5, 'PARCEL_REGISTERED', 'REGISTERED', 'Houston, TX', 'Medical supplies registered', '2024-01-11 08:20:00', '2024-01-11 08:20:00'),
(48, 5, 'PICKED_UP', 'PICKED_UP', 'Houston, TX', 'Express medical pickup', '2024-01-11 10:15:00', '2024-01-11 10:15:00'),
(49, 5, 'ARRIVED_AT_FACILITY', 'IN_TRANSIT', 'Austin, TX', 'Medical facility processing', '2024-01-12 14:30:00', '2024-01-12 14:30:00'),
(50, 5, 'DELIVERED', 'DELIVERED', 'Austin, TX', 'Medical supplies delivered', '2024-01-15 13:15:00', '2024-01-15 13:15:00');
