package com.courier.integration;

import com.courier.dto.EDIParcelOrderDTO;
import com.courier.entity.Parcel;
import com.courier.service.EDIParcelConsumerService;
import com.courier.service.ParcelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"incoming-parcel-orders", "abc-transport-events", "parcel-tracking-events"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"}
)
@DirtiesContext
class EDIParcelProcessingIntegrationTest {
    
    @Autowired
    private EDIParcelConsumerService ediParcelConsumerService;
    
    @Autowired
    private ParcelService parcelService;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${kafka.topics.incoming-orders}")
    private String incomingOrdersTopic;
    
    @Test
    @Transactional
    void testDirectEDIOrderProcessing() throws Exception {
        // Create test EDI order
        EDIParcelOrderDTO ediOrder = createTestEDIOrder();
        
        // Process EDI order directly
        Parcel savedParcel = ediParcelConsumerService.processEDIOrder(ediOrder);
        
        // Verify parcel was saved to database
        assertNotNull(savedParcel);
        assertNotNull(savedParcel.getId());
        assertNotNull(savedParcel.getParcelId());
        assertEquals(ediOrder.getEdiReference(), savedParcel.getEdiReference());
        assertEquals(Parcel.ParcelStatus.REGISTERED, savedParcel.getStatus());
        assertEquals(Parcel.Priority.STANDARD, savedParcel.getPriority());
        
        // Verify sender information
        assertNotNull(savedParcel.getSender());
        assertEquals("John Doe", savedParcel.getSender().getName());
        assertEquals("john@example.com", savedParcel.getSender().getEmail());
        
        // Verify recipient information
        assertNotNull(savedParcel.getRecipient());
        assertEquals("Jane Smith", savedParcel.getRecipient().getName());
        assertEquals("jane@example.com", savedParcel.getRecipient().getEmail());
        
        // Verify addresses
        assertNotNull(savedParcel.getPickupAddress());
        assertEquals("123 Main St", savedParcel.getPickupAddress().getStreetAddress());
        assertEquals("New York", savedParcel.getPickupAddress().getCity());
        
        assertNotNull(savedParcel.getDeliveryAddress());
        assertEquals("456 Oak Ave", savedParcel.getDeliveryAddress().getStreetAddress());
        assertEquals("Los Angeles", savedParcel.getDeliveryAddress().getCity());
        
        // Verify parcel details
        assertEquals("Test package", savedParcel.getDescription());
        assertEquals(0, savedParcel.getWeight().compareTo(java.math.BigDecimal.valueOf(2.5)));
        assertEquals("10x10x10", savedParcel.getDimensions());
        
        // Verify timestamps
        assertNotNull(savedParcel.getCreatedAt());
        assertNotNull(savedParcel.getUpdatedAt());
    }
    
    @Test
    @Transactional
    void testEDIOrderValidation() {
        // Test with null EDI order
        assertThrows(IllegalArgumentException.class, () -> {
            ediParcelConsumerService.processEDIOrder(null);
        });
        
        // Test with missing EDI reference
        EDIParcelOrderDTO invalidOrder = createTestEDIOrder();
        invalidOrder.setEdiReference(null);
        
        assertThrows(IllegalArgumentException.class, () -> {
            ediParcelConsumerService.processEDIOrder(invalidOrder);
        });
        
        // Test with missing sender
        invalidOrder = createTestEDIOrder();
        invalidOrder.setSender(null);
        
        assertThrows(IllegalArgumentException.class, () -> {
            ediParcelConsumerService.processEDIOrder(invalidOrder);
        });
        
        // Test with invalid email
        invalidOrder = createTestEDIOrder();
        invalidOrder.getSender().setEmail("invalid-email");
        
        assertThrows(IllegalArgumentException.class, () -> {
            ediParcelConsumerService.processEDIOrder(invalidOrder);
        });
    }
    
    @Test
    @Transactional
    void testKafkaEDIOrderSubmission() throws Exception {
        // Create test EDI order
        EDIParcelOrderDTO ediOrder = createTestEDIOrder();
        String ediOrderJson = objectMapper.writeValueAsString(ediOrder);
        
        // Send to Kafka topic
        kafkaTemplate.send(incomingOrdersTopic, ediOrder.getEdiReference(), ediOrderJson).get(10, TimeUnit.SECONDS);
        
        // Wait a bit for async processing
        Thread.sleep(2000);
        
        // Verify parcel was processed and saved
        Optional<Parcel> savedParcelOpt = parcelService.findByEdiReference(ediOrder.getEdiReference());
        assertTrue(savedParcelOpt.isPresent());
        
        Parcel savedParcel = savedParcelOpt.get();
        assertEquals(ediOrder.getEdiReference(), savedParcel.getEdiReference());
        assertEquals(Parcel.ParcelStatus.REGISTERED, savedParcel.getStatus());
    }
    
    @Test
    @Transactional
    void testFindParcelByEdiReference() throws Exception {
        // Create and save a parcel
        EDIParcelOrderDTO ediOrder = createTestEDIOrder();
        Parcel savedParcel = ediParcelConsumerService.processEDIOrder(ediOrder);
        
        // Find by EDI reference
        Optional<Parcel> foundParcelOpt = parcelService.findByEdiReference(ediOrder.getEdiReference());
        assertTrue(foundParcelOpt.isPresent());
        
        Parcel foundParcel = foundParcelOpt.get();
        assertEquals(savedParcel.getId(), foundParcel.getId());
        assertEquals(savedParcel.getParcelId(), foundParcel.getParcelId());
        assertEquals(savedParcel.getEdiReference(), foundParcel.getEdiReference());
    }
    
    @Test
    @Transactional
    void testManualEDIOrderProcessing() throws Exception {
        // Create test EDI order JSON
        EDIParcelOrderDTO ediOrder = createTestEDIOrder();
        String ediOrderJson = objectMapper.writeValueAsString(ediOrder);
        
        // Process manually
        Parcel savedParcel = ediParcelConsumerService.processEDIOrderManually(ediOrderJson);
        
        // Verify processing
        assertNotNull(savedParcel);
        assertEquals(ediOrder.getEdiReference(), savedParcel.getEdiReference());
        
        // Verify it's in the database
        Optional<Parcel> dbParcelOpt = parcelService.findByEdiReference(ediOrder.getEdiReference());
        assertTrue(dbParcelOpt.isPresent());
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
        parcelDetails.setDescription("Test package");
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
}
