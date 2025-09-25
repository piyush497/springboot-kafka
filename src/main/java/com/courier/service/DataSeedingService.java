package com.courier.service;

import com.courier.entity.Customer;
import com.courier.entity.Parcel;
import com.courier.entity.ParcelTrackingEvent;
import com.courier.entity.EDIParcelOrder;
import com.courier.repository.CustomerRepository;
import com.courier.repository.ParcelRepository;
import com.courier.repository.ParcelTrackingEventRepository;
import com.courier.repository.EDIParcelOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Service to seed the database with sample data
 * Creates approximately 400 rows of realistic courier management data
 */
@Service
public class DataSeedingService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeedingService.class);
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private ParcelRepository parcelRepository;
    
    @Autowired
    private ParcelTrackingEventRepository trackingEventRepository;
    
    @Autowired
    private EDIParcelOrderRepository ediParcelOrderRepository;
    
    private final Random random = new Random();
    
    // Sample data arrays
    private final String[] businessNames = {
        "Acme Corporation", "Global Logistics Ltd", "TechStart Inc", "MegaCorp Industries",
        "QuickShip Solutions", "FastTrack Enterprises", "Rapid Delivery Co", "SwiftCargo Ltd",
        "Lightning Logistics", "Express Solutions Inc", "UltraFast Shipping", "Prime Logistics Group",
        "Turbo Transport Co", "Rocket Delivery Services", "Velocity Logistics", "Sonic Shipping Solutions",
        "Blitz Cargo Express", "Flash Forward Logistics", "Zoom Delivery Network", "Dash Distribution Co",
        "Jet Stream Logistics", "Warp Speed Shipping", "Hyper Logistics Hub", "Quantum Delivery Systems",
        "Nexus Transport Group", "Infinity Logistics Solutions"
    };
    
    private final String[] firstNames = {
        "John", "Sarah", "Emily", "Michael", "Lisa", "David", "Jennifer", "Robert",
        "Amanda", "Christopher", "Michelle", "Kevin", "Nicole", "Brian", "Rachel",
        "Daniel", "Stephanie", "Matthew", "Ashley", "Joshua", "Megan", "Tyler",
        "Brittany", "Jordan", "Alice", "Bob", "Carol", "David", "Eva", "Frank",
        "Grace", "Henry", "Iris", "Jack", "Kate", "Liam", "Mia", "Noah", "Olivia",
        "Paul", "Quinn", "Rachel", "Sam", "Tina", "Uma", "Victor", "Wendy"
    };
    
    private final String[] lastNames = {
        "Smith", "Johnson", "Davis", "Brown", "Wilson", "Miller", "Taylor", "Anderson",
        "White", "Lee", "Garcia", "Martinez", "Rodriguez", "Thompson", "Clark",
        "Lewis", "Walker", "Hall", "Young", "King", "Wright", "Green", "Adams",
        "Baker", "Cooper", "Wilson", "Davis", "Martinez", "Garcia", "Rodriguez",
        "Thompson", "White", "Lee", "Clark", "Lewis", "Walker", "Hall", "Young"
    };
    
    private final String[] cities = {
        "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia",
        "San Antonio", "San Diego", "Dallas", "San Jose", "Austin", "Jacksonville",
        "Fort Worth", "Columbus", "Charlotte", "San Francisco", "Indianapolis",
        "Seattle", "Denver", "Washington", "Boston", "El Paso", "Nashville",
        "Detroit", "Oklahoma City", "Portland", "Las Vegas", "Memphis", "Louisville",
        "Baltimore", "Milwaukee", "Albuquerque", "Tucson", "Fresno", "Sacramento",
        "Mesa", "Kansas City", "Atlanta", "Long Beach", "Colorado Springs", "Raleigh",
        "Miami", "Virginia Beach", "Omaha", "Oakland", "Minneapolis", "Tulsa",
        "Arlington", "Tampa", "New Orleans", "Wichita", "Cleveland", "Bakersfield"
    };
    
    private final String[] states = {
        "NY", "CA", "IL", "TX", "AZ", "PA", "FL", "WA", "CO", "MA", "NV", "TN",
        "MI", "OK", "OR", "KY", "MD", "WI", "NM", "MN", "KS", "GA", "VA", "NE",
        "OH", "NC", "LA", "UT", "AL", "AR", "CT", "DE", "HI", "ID", "IN", "IA"
    };
    
    private final String[] priorities = {"STANDARD", "EXPRESS", "PRIORITY"};
    private final String[] statuses = {"REGISTERED", "PICKED_UP", "IN_TRANSIT", "LOADED_IN_TRUCK", "OUT_FOR_DELIVERY", "DELIVERED"};
    private final String[] customerTypes = {"INDIVIDUAL", "BUSINESS"};
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (shouldSeedData()) {
            logger.info("Starting database seeding with sample data...");
            seedData();
            logger.info("Database seeding completed successfully!");
        } else {
            logger.info("Database already contains data. Skipping seeding.");
        }
    }
    
    private boolean shouldSeedData() {
        return customerRepository.count() == 0 && parcelRepository.count() == 0;
    }
    
    private void seedData() {
        // Create 50 customers
        List<Customer> customers = createCustomers(50);
        customerRepository.saveAll(customers);
        logger.info("Created {} customers", customers.size());
        
        // Create 200 parcels
        List<Parcel> parcels = createParcels(customers, 200);
        parcelRepository.saveAll(parcels);
        logger.info("Created {} parcels", parcels.size());
        
        // Create 100 EDI orders
        List<EDIParcelOrder> ediOrders = createEDIOrders(customers, 100);
        ediParcelOrderRepository.saveAll(ediOrders);
        logger.info("Created {} EDI orders", ediOrders.size());
        
        // Create tracking events (approximately 50 events)
        List<ParcelTrackingEvent> trackingEvents = createTrackingEvents(parcels, 50);
        trackingEventRepository.saveAll(trackingEvents);
        logger.info("Created {} tracking events", trackingEvents.size());
        
        logger.info("Total records created: {}", 
            customers.size() + parcels.size() + ediOrders.size() + trackingEvents.size());
    }
    
    private List<Customer> createCustomers(int count) {
        List<Customer> customers = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            Customer customer = new Customer();
            
            boolean isBusiness = random.nextBoolean();
            customer.setCustomerType(isBusiness ? "BUSINESS" : "INDIVIDUAL");
            
            if (isBusiness) {
                customer.setName(businessNames[random.nextInt(businessNames.length)] + " " + i);
                customer.setEmail("orders" + i + "@" + customer.getName().toLowerCase().replace(" ", "") + ".com");
            } else {
                String firstName = firstNames[random.nextInt(firstNames.length)];
                String lastName = lastNames[random.nextInt(lastNames.length)];
                customer.setName(firstName + " " + lastName);
                customer.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@email.com");
            }
            
            customer.setPhone("+1-555-" + String.format("%04d", 100 + i));
            customer.setAddressLine1((100 + i * 10) + " " + getRandomStreetName());
            
            if (random.nextBoolean()) {
                customer.setAddressLine2(getRandomAddressLine2());
            }
            
            String city = cities[random.nextInt(cities.length)];
            String state = states[random.nextInt(states.length)];
            customer.setCity(city);
            customer.setState(state);
            customer.setPostalCode(String.format("%05d", 10000 + random.nextInt(90000)));
            customer.setCountry("USA");
            customer.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(365)));
            customer.setUpdatedAt(customer.getCreatedAt().plusDays(random.nextInt(30)));
            
            customers.add(customer);
        }
        
        return customers;
    }
    
    private List<Parcel> createParcels(List<Customer> customers, int count) {
        List<Parcel> parcels = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            Parcel parcel = new Parcel();
            
            parcel.setTrackingNumber("TRK" + String.format("%09d", 1000000 + i));
            
            Customer sender = customers.get(random.nextInt(customers.size()));
            parcel.setSender(sender);
            
            // Generate recipient
            String recipientFirstName = firstNames[random.nextInt(firstNames.length)];
            String recipientLastName = lastNames[random.nextInt(lastNames.length)];
            parcel.setRecipientName(recipientFirstName + " " + recipientLastName);
            parcel.setRecipientPhone("+1-555-" + String.format("%04d", 9000 + i));
            parcel.setRecipientEmail(recipientFirstName.toLowerCase() + "." + recipientLastName.toLowerCase() + "@email.com");
            
            // Pickup address (same as sender)
            parcel.setPickupAddressLine1(sender.getAddressLine1());
            parcel.setPickupAddressLine2(sender.getAddressLine2());
            parcel.setPickupCity(sender.getCity());
            parcel.setPickupState(sender.getState());
            parcel.setPickupPostalCode(sender.getPostalCode());
            parcel.setPickupCountry(sender.getCountry());
            
            // Delivery address (random)
            parcel.setDeliveryAddressLine1((500 + i * 5) + " " + getRandomStreetName());
            if (random.nextBoolean()) {
                parcel.setDeliveryAddressLine2(getRandomAddressLine2());
            }
            String deliveryCity = cities[random.nextInt(cities.length)];
            String deliveryState = states[random.nextInt(states.length)];
            parcel.setDeliveryCity(deliveryCity);
            parcel.setDeliveryState(deliveryState);
            parcel.setDeliveryPostalCode(String.format("%05d", 20000 + random.nextInt(80000)));
            parcel.setDeliveryCountry("USA");
            
            // Package details
            parcel.setWeightKg(BigDecimal.valueOf(0.5 + random.nextDouble() * 10).setScale(2, BigDecimal.ROUND_HALF_UP));
            parcel.setLengthCm(15 + random.nextInt(35));
            parcel.setWidthCm(10 + random.nextInt(25));
            parcel.setHeightCm(5 + random.nextInt(20));
            
            parcel.setDeclaredValue(BigDecimal.valueOf(25 + random.nextDouble() * 500).setScale(2, BigDecimal.ROUND_HALF_UP));
            parcel.setInsuranceAmount(parcel.getDeclaredValue().multiply(BigDecimal.valueOf(0.1)));
            
            parcel.setPriority(priorities[random.nextInt(priorities.length)]);
            parcel.setStatus(statuses[random.nextInt(statuses.length)]);
            
            if (random.nextBoolean()) {
                parcel.setSpecialInstructions(getRandomSpecialInstructions());
            }
            
            LocalDateTime createdDate = LocalDateTime.now().minusDays(random.nextInt(60));
            parcel.setCreatedAt(createdDate);
            parcel.setUpdatedAt(createdDate.plusHours(random.nextInt(48)));
            
            // Set delivery dates based on status
            if ("DELIVERED".equals(parcel.getStatus())) {
                parcel.setEstimatedDeliveryDate(createdDate.plusDays(1 + random.nextInt(7)).toLocalDate());
                parcel.setActualDeliveryDate(parcel.getEstimatedDeliveryDate().minusDays(random.nextInt(2)));
            } else {
                parcel.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(1 + random.nextInt(7)).toLocalDate());
            }
            
            parcels.add(parcel);
        }
        
        return parcels;
    }
    
    private List<EDIParcelOrder> createEDIOrders(List<Customer> customers, int count) {
        List<EDIParcelOrder> ediOrders = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            EDIParcelOrder ediOrder = new EDIParcelOrder();
            
            ediOrder.setEdiReference("EDI" + String.format("%08d", 10000000 + i));
            ediOrder.setCustomer(customers.get(random.nextInt(customers.size())));
            ediOrder.setStatus(random.nextBoolean() ? "PROCESSED" : "PENDING");
            
            // Generate JSON payload
            String jsonPayload = generateEDIJsonPayload(ediOrder.getEdiReference(), ediOrder.getCustomer());
            ediOrder.setJsonPayload(jsonPayload);
            
            LocalDateTime processedDate = LocalDateTime.now().minusDays(random.nextInt(30));
            ediOrder.setProcessedAt(processedDate);
            ediOrder.setCreatedAt(processedDate.minusHours(random.nextInt(24)));
            ediOrder.setUpdatedAt(processedDate.plusHours(random.nextInt(12)));
            
            if (random.nextBoolean()) {
                ediOrder.setErrorMessage(null);
            } else {
                ediOrder.setErrorMessage("Validation warning: Address verification recommended");
            }
            
            ediOrders.add(ediOrder);
        }
        
        return ediOrders;
    }
    
    private List<ParcelTrackingEvent> createTrackingEvents(List<Parcel> parcels, int count) {
        List<ParcelTrackingEvent> events = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            Parcel parcel = parcels.get(random.nextInt(parcels.size()));
            
            ParcelTrackingEvent event = new ParcelTrackingEvent();
            event.setParcel(parcel);
            event.setEventType(getRandomEventType());
            event.setStatus(statuses[random.nextInt(statuses.length)]);
            event.setLocation(cities[random.nextInt(cities.length)] + ", " + states[random.nextInt(states.length)]);
            event.setDescription(generateEventDescription(event.getEventType(), event.getStatus()));
            
            LocalDateTime eventTime = parcel.getCreatedAt().plusHours(random.nextInt(72));
            event.setEventTimestamp(eventTime);
            event.setCreatedAt(eventTime);
            
            events.add(event);
        }
        
        return events;
    }
    
    private String getRandomStreetName() {
        String[] streetTypes = {"St", "Ave", "Blvd", "Dr", "Lane", "Way", "Plaza", "Court"};
        String[] streetNames = {"Main", "Oak", "Pine", "Maple", "Cedar", "Elm", "Park", "First", "Second", "Third",
                               "Business", "Commerce", "Industrial", "Corporate", "Express", "Fast", "Quick", "Speed"};
        return streetNames[random.nextInt(streetNames.length)] + " " + streetTypes[random.nextInt(streetTypes.length)];
    }
    
    private String getRandomAddressLine2() {
        String[] types = {"Suite", "Apt", "Unit", "Floor", "Building"};
        String type = types[random.nextInt(types.length)];
        if ("Floor".equals(type)) {
            return type + " " + (1 + random.nextInt(20));
        } else if ("Building".equals(type)) {
            return type + " " + (char)('A' + random.nextInt(6));
        } else {
            return type + " " + (1 + random.nextInt(500)) + (random.nextBoolean() ? "" : (char)('A' + random.nextInt(4)));
        }
    }
    
    private String getRandomSpecialInstructions() {
        String[] instructions = {
            "Fragile - Handle with care",
            "Signature required",
            "Leave at front door",
            "Ring doorbell",
            "Deliver to back entrance",
            "Electronics - Keep dry",
            "Medical supplies",
            "Documents enclosed",
            "Gift wrapping included",
            "Perishable items",
            "Heavy item - Use care",
            "This side up",
            "Do not stack",
            "Temperature sensitive"
        };
        return instructions[random.nextInt(instructions.length)];
    }
    
    private String getRandomEventType() {
        String[] eventTypes = {
            "PARCEL_REGISTERED", "PICKUP_SCHEDULED", "PICKED_UP", "IN_TRANSIT",
            "ARRIVED_AT_FACILITY", "OUT_FOR_DELIVERY", "DELIVERY_ATTEMPTED", "DELIVERED"
        };
        return eventTypes[random.nextInt(eventTypes.length)];
    }
    
    private String generateEventDescription(String eventType, String status) {
        switch (eventType) {
            case "PARCEL_REGISTERED":
                return "Parcel registered in system";
            case "PICKUP_SCHEDULED":
                return "Pickup scheduled with courier";
            case "PICKED_UP":
                return "Parcel picked up from sender";
            case "IN_TRANSIT":
                return "Parcel in transit to destination";
            case "ARRIVED_AT_FACILITY":
                return "Parcel arrived at sorting facility";
            case "OUT_FOR_DELIVERY":
                return "Parcel out for delivery";
            case "DELIVERY_ATTEMPTED":
                return "Delivery attempted - recipient not available";
            case "DELIVERED":
                return "Parcel delivered successfully";
            default:
                return "Status updated to " + status;
        }
    }
    
    private String generateEDIJsonPayload(String ediReference, Customer customer) {
        return String.format("""
            {
              "ediReference": "%s",
              "sender": {
                "name": "%s",
                "email": "%s",
                "phone": "%s"
              },
              "recipient": {
                "name": "%s",
                "phone": "+1-555-%04d",
                "email": "recipient@example.com"
              },
              "package": {
                "weight": %.2f,
                "dimensions": {
                  "length": %d,
                  "width": %d,
                  "height": %d
                },
                "value": %.2f
              },
              "service": {
                "priority": "%s",
                "insurance": true
              }
            }""",
            ediReference,
            customer.getName(),
            customer.getEmail(),
            customer.getPhone(),
            firstNames[random.nextInt(firstNames.length)] + " " + lastNames[random.nextInt(lastNames.length)],
            8000 + random.nextInt(1000),
            0.5 + random.nextDouble() * 5,
            15 + random.nextInt(20),
            10 + random.nextInt(15),
            5 + random.nextInt(10),
            50 + random.nextDouble() * 200,
            priorities[random.nextInt(priorities.length)]
        );
    }
}
