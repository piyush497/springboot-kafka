package com.courier.integration;

import com.courier.dto.EDIParcelOrderDTO;
import com.courier.entity.Parcel;
import com.courier.service.ParcelService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"abc-transport-events", "parcel-tracking-events"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"}
)
@DirtiesContext
class EventingPlatformIntegrationTest {
    
    @Autowired
    private ParcelService parcelService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testCompleteEventFlow() throws Exception {
        // Create a test EDI order
        EDIParcelOrderDTO ediOrder = createTestEDIOrder();
        
        // Create Kafka consumer to verify events
        KafkaConsumer<String, String> consumer = createTestConsumer();
        consumer.subscribe(Collections.singletonList("abc-transport-events"));
        
        // Process the EDI order - this should trigger events
        Parcel createdParcel = parcelService.createParcelFromEDI(ediOrder);
        
        assertNotNull(createdParcel);
        assertNotNull(createdParcel.getParcelId());
        assertEquals(Parcel.ParcelStatus.REGISTERED, createdParcel.getStatus());
        
        // Poll for messages with timeout
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        
        assertFalse(records.isEmpty(), "Should have received at least one event");
        
        // Verify the ABC Transport event
        boolean foundABCTransportEvent = false;
        for (ConsumerRecord<String, String> record : records) {
            JsonNode eventNode = objectMapper.readTree(record.value());
            
            if ("ABC_TRANSPORT_EVENT".equals(eventNode.path("eventType").asText())) {
                foundABCTransportEvent = true;
                
                // Verify event structure
                assertEquals(createdParcel.getParcelId(), eventNode.path("parcelId").asText());
                assertEquals("PARCEL_REGISTRATION", eventNode.path("messageType").asText());
                assertEquals("REGISTERED", eventNode.path("status").asText());
                assertEquals("STANDARD", eventNode.path("priority").asText());
                
                // Verify sender information
                JsonNode sender = eventNode.path("sender");
                assertEquals("John Doe", sender.path("name").asText());
                assertEquals("john@example.com", sender.path("email").asText());
                
                // Verify recipient information
                JsonNode recipient = eventNode.path("recipient");
                assertEquals("Jane Smith", recipient.path("name").asText());
                assertEquals("jane@example.com", recipient.path("email").asText());
                
                break;
            }
        }
        
        assertTrue(foundABCTransportEvent, "Should have found ABC Transport event");
        
        consumer.close();
    }
    
    @Test
    void testTrackingEventFlow() throws Exception {
        // Create a test parcel first
        EDIParcelOrderDTO ediOrder = createTestEDIOrder();
        Parcel parcel = parcelService.createParcelFromEDI(ediOrder);
        
        // Create consumer for tracking events
        KafkaConsumer<String, String> consumer = createTestConsumer();
        consumer.subscribe(Collections.singletonList("parcel-tracking-events"));
        
        // Update parcel status - this should trigger tracking events
        parcelService.updateParcelStatus(
            parcel.getParcelId(),
            Parcel.ParcelStatus.PICKED_UP,
            "Pickup Location",
            "Picked up by driver"
        );
        
        // Poll for tracking events
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
        
        assertFalse(records.isEmpty(), "Should have received tracking events");
        
        boolean foundTrackingEvent = false;
        for (ConsumerRecord<String, String> record : records) {
            JsonNode eventNode = objectMapper.readTree(record.value());
            
            if ("TRACKING_EVENT".equals(eventNode.path("eventType").asText())) {
                foundTrackingEvent = true;
                
                assertEquals(parcel.getParcelId(), eventNode.path("parcelId").asText());
                assertEquals("PICKED_UP", eventNode.path("trackingEventType").asText());
                assertEquals("Pickup Location", eventNode.path("location").asText());
                
                break;
            }
        }
        
        assertTrue(foundTrackingEvent, "Should have found tracking event");
        
        consumer.close();
    }
    
    private EDIParcelOrderDTO createTestEDIOrder() {
        EDIParcelOrderDTO ediOrder = new EDIParcelOrderDTO();
        ediOrder.setEdiReference("EDI-TEST-" + System.currentTimeMillis());
        
        // Sender
        EDIParcelOrderDTO.CustomerDTO sender = new EDIParcelOrderDTO.CustomerDTO();
        sender.setName("John Doe");
        sender.setEmail("john@example.com");
        sender.setPhone("+1234567890");
        sender.setCustomerCode("SENDER-001");
        ediOrder.setSender(sender);
        
        // Recipient
        EDIParcelOrderDTO.CustomerDTO recipient = new EDIParcelOrderDTO.CustomerDTO();
        recipient.setName("Jane Smith");
        recipient.setEmail("jane@example.com");
        recipient.setPhone("+0987654321");
        recipient.setCustomerCode("RECIPIENT-001");
        ediOrder.setRecipient(recipient);
        
        // Pickup Address
        EDIParcelOrderDTO.AddressDTO pickupAddress = new EDIParcelOrderDTO.AddressDTO();
        pickupAddress.setStreetAddress("123 Main St");
        pickupAddress.setCity("New York");
        pickupAddress.setState("NY");
        pickupAddress.setPostalCode("10001");
        pickupAddress.setCountry("USA");
        ediOrder.setPickupAddress(pickupAddress);
        
        // Delivery Address
        EDIParcelOrderDTO.AddressDTO deliveryAddress = new EDIParcelOrderDTO.AddressDTO();
        deliveryAddress.setStreetAddress("456 Oak Ave");
        deliveryAddress.setCity("Los Angeles");
        deliveryAddress.setState("CA");
        deliveryAddress.setPostalCode("90001");
        deliveryAddress.setCountry("USA");
        ediOrder.setDeliveryAddress(deliveryAddress);
        
        // Parcel Details
        EDIParcelOrderDTO.ParcelDetailsDTO parcelDetails = new EDIParcelOrderDTO.ParcelDetailsDTO();
        parcelDetails.setDescription("Test package for integration test");
        parcelDetails.setWeight(2.5);
        parcelDetails.setDimensions("10x10x10");
        ediOrder.setParcelDetails(parcelDetails);
        
        // Service Options
        EDIParcelOrderDTO.ServiceOptionsDTO serviceOptions = new EDIParcelOrderDTO.ServiceOptionsDTO();
        serviceOptions.setPriority("STANDARD");
        serviceOptions.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(3));
        ediOrder.setServiceOptions(serviceOptions);
        
        return ediOrder;
    }
    
    private KafkaConsumer<String, String> createTestConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        
        return new KafkaConsumer<>(props);
    }
}
