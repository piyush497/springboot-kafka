package com.courier.controller;

import com.courier.dto.EDIParcelOrderDTO;
import com.courier.entity.Parcel;
import com.courier.service.EDIParcelConsumerService;
import com.courier.service.ParcelService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for EDI Parcel Order processing
 */
@RestController
@RequestMapping("/api/v1/edi")
@CrossOrigin(origins = "*")
public class EDIParcelController {
    
    private static final Logger logger = LoggerFactory.getLogger(EDIParcelController.class);
    
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
    
    /**
     * Direct EDI order processing (synchronous)
     * POST /api/v1/edi/process
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processEDIOrder(@Valid @RequestBody EDIParcelOrderDTO ediOrder) {
        logger.info("Received direct EDI order processing request for reference: {}", ediOrder.getEdiReference());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Process EDI order directly
            Parcel savedParcel = ediParcelConsumerService.processEDIOrder(ediOrder);
            
            response.put("success", true);
            response.put("message", "EDI order processed successfully");
            response.put("parcelId", savedParcel.getParcelId());
            response.put("ediReference", savedParcel.getEdiReference());
            response.put("status", savedParcel.getStatus().toString());
            response.put("createdAt", savedParcel.getCreatedAt());
            
            logger.info("Successfully processed EDI order directly for parcel: {}", savedParcel.getParcelId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing EDI order directly", e);
            
            response.put("success", false);
            response.put("message", "Failed to process EDI order: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Submit EDI order to Kafka topic for asynchronous processing
     * POST /api/v1/edi/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitEDIOrder(@Valid @RequestBody EDIParcelOrderDTO ediOrder) {
        logger.info("Received EDI order submission request for reference: {}", ediOrder.getEdiReference());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Convert EDI order to JSON
            String ediOrderJson = objectMapper.writeValueAsString(ediOrder);
            
            // Send to Kafka topic for asynchronous processing
            kafkaTemplate.send(incomingOrdersTopic, ediOrder.getEdiReference(), ediOrderJson)
                .whenComplete((result, exception) -> {
                    if (exception == null) {
                        logger.info("Successfully sent EDI order {} to Kafka topic {} with offset: {}",
                            ediOrder.getEdiReference(), incomingOrdersTopic, 
                            result.getRecordMetadata().offset());
                    } else {
                        logger.error("Failed to send EDI order {} to Kafka topic {}",
                            ediOrder.getEdiReference(), incomingOrdersTopic, exception);
                    }
                });
            
            response.put("success", true);
            response.put("message", "EDI order submitted for processing");
            response.put("ediReference", ediOrder.getEdiReference());
            response.put("topic", incomingOrdersTopic);
            response.put("status", "SUBMITTED");
            
            logger.info("Successfully submitted EDI order {} to Kafka topic", ediOrder.getEdiReference());
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            logger.error("Error submitting EDI order to Kafka", e);
            
            response.put("success", false);
            response.put("message", "Failed to submit EDI order: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get parcel status by EDI reference
     * GET /api/v1/edi/status/{ediReference}
     */
    @GetMapping("/status/{ediReference}")
    public ResponseEntity<Map<String, Object>> getParcelStatusByEDIReference(@PathVariable String ediReference) {
        logger.info("Received status request for EDI reference: {}", ediReference);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Find parcel by EDI reference
            var parcelOpt = parcelService.findByEdiReference(ediReference);
            
            if (parcelOpt.isPresent()) {
                Parcel parcel = parcelOpt.get();
                
                response.put("success", true);
                response.put("message", "Parcel found");
                response.put("ediReference", ediReference);
                response.put("parcelId", parcel.getParcelId());
                response.put("status", parcel.getStatus().toString());
                response.put("priority", parcel.getPriority().toString());
                response.put("createdAt", parcel.getCreatedAt());
                response.put("updatedAt", parcel.getUpdatedAt());
                response.put("estimatedDeliveryDate", parcel.getEstimatedDeliveryDate());
                response.put("actualDeliveryDate", parcel.getActualDeliveryDate());
                
                // Add sender and recipient info
                Map<String, Object> senderInfo = new HashMap<>();
                senderInfo.put("name", parcel.getSender().getName());
                senderInfo.put("email", parcel.getSender().getEmail());
                response.put("sender", senderInfo);
                
                Map<String, Object> recipientInfo = new HashMap<>();
                recipientInfo.put("name", parcel.getRecipient().getName());
                recipientInfo.put("email", parcel.getRecipient().getEmail());
                response.put("recipient", recipientInfo);
                
            } else {
                response.put("success", false);
                response.put("message", "Parcel not found for EDI reference: " + ediReference);
                response.put("ediReference", ediReference);
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting parcel status for EDI reference: {}", ediReference, e);
            
            response.put("success", false);
            response.put("message", "Failed to get parcel status: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Health check endpoint
     * GET /api/v1/edi/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "EDI Parcel Processing Service");
        response.put("timestamp", System.currentTimeMillis());
        response.put("kafkaTopic", incomingOrdersTopic);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create a sample EDI order for testing
     * GET /api/v1/edi/sample
     */
    @GetMapping("/sample")
    public ResponseEntity<EDIParcelOrderDTO> getSampleEDIOrder() {
        EDIParcelOrderDTO sampleOrder = createSampleEDIOrder();
        return ResponseEntity.ok(sampleOrder);
    }
    
    private EDIParcelOrderDTO createSampleEDIOrder() {
        EDIParcelOrderDTO ediOrder = new EDIParcelOrderDTO();
        ediOrder.setEdiReference("EDI-SAMPLE-" + System.currentTimeMillis());
        
        // Sender
        EDIParcelOrderDTO.CustomerDTO sender = new EDIParcelOrderDTO.CustomerDTO();
        sender.setName("John Doe");
        sender.setEmail("john.doe@example.com");
        sender.setPhone("+1-555-123-4567");
        sender.setCustomerCode("SENDER-001");
        ediOrder.setSender(sender);
        
        // Recipient
        EDIParcelOrderDTO.CustomerDTO recipient = new EDIParcelOrderDTO.CustomerDTO();
        recipient.setName("Jane Smith");
        recipient.setEmail("jane.smith@example.com");
        recipient.setPhone("+1-555-987-6543");
        recipient.setCustomerCode("RECIPIENT-001");
        ediOrder.setRecipient(recipient);
        
        // Pickup Address
        EDIParcelOrderDTO.AddressDTO pickupAddress = new EDIParcelOrderDTO.AddressDTO();
        pickupAddress.setStreetAddress("123 Main Street");
        pickupAddress.setCity("New York");
        pickupAddress.setState("NY");
        pickupAddress.setPostalCode("10001");
        pickupAddress.setCountry("USA");
        pickupAddress.setLandmark("Near Central Park");
        ediOrder.setPickupAddress(pickupAddress);
        
        // Delivery Address
        EDIParcelOrderDTO.AddressDTO deliveryAddress = new EDIParcelOrderDTO.AddressDTO();
        deliveryAddress.setStreetAddress("456 Oak Avenue");
        deliveryAddress.setCity("Los Angeles");
        deliveryAddress.setState("CA");
        deliveryAddress.setPostalCode("90001");
        deliveryAddress.setCountry("USA");
        deliveryAddress.setLandmark("Business District");
        ediOrder.setDeliveryAddress(deliveryAddress);
        
        // Parcel Details
        EDIParcelOrderDTO.ParcelDetailsDTO parcelDetails = new EDIParcelOrderDTO.ParcelDetailsDTO();
        parcelDetails.setDescription("Sample package for testing");
        parcelDetails.setWeight(2.5);
        parcelDetails.setDimensions("30x20x15 cm");
        ediOrder.setParcelDetails(parcelDetails);
        
        // Service Options
        EDIParcelOrderDTO.ServiceOptionsDTO serviceOptions = new EDIParcelOrderDTO.ServiceOptionsDTO();
        serviceOptions.setPriority("STANDARD");
        serviceOptions.setEstimatedDeliveryDate(java.time.LocalDateTime.now().plusDays(3));
        ediOrder.setServiceOptions(serviceOptions);
        
        return ediOrder;
    }
}
