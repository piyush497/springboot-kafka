package com.courier.service;

import com.courier.dto.EDIParcelOrderDTO;
import com.courier.entity.Parcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

/**
 * Service to consume EDI parcel orders from Kafka topic and process them
 */
@Service
public class EDIParcelConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(EDIParcelConsumerService.class);
    
    @Autowired
    private ParcelService parcelService;
    
    @Autowired
    private EventStreamService eventStreamService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Kafka listener for incoming EDI parcel orders
     * Listens to the incoming-parcel-orders topic
     */
    @KafkaListener(
        topics = "${kafka.topics.incoming-orders}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeEDIParcelOrder(
            @Payload String ediOrderJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_MESSAGE_KEY, required = false) String key,
            Acknowledgment acknowledgment) {
        
        logger.info("Received EDI parcel order from topic: {}, partition: {}, offset: {}, key: {}", 
                   topic, partition, offset, key);
        
        try {
            // Parse the JSON payload to EDI order DTO
            EDIParcelOrderDTO ediOrder = objectMapper.readValue(ediOrderJson, EDIParcelOrderDTO.class);
            
            logger.info("Processing EDI order with reference: {}", ediOrder.getEdiReference());
            
            // Validate the EDI order
            validateEDIOrder(ediOrder);
            
            // Save parcel to database using ParcelService
            Parcel savedParcel = parcelService.createParcelFromEDI(ediOrder);
            
            logger.info("Successfully saved parcel with ID: {} to database", savedParcel.getParcelId());
            
            // Publish event to ABC transport topic
            publishToABCTransport(savedParcel);
            
            // Acknowledge successful processing
            acknowledgment.acknowledge();
            
            logger.info("Successfully processed EDI order for parcel: {}", savedParcel.getParcelId());
            
        } catch (Exception e) {
            logger.error("Error processing EDI parcel order from topic: {}, offset: {}", topic, offset, e);
            
            // In production, you might want to:
            // 1. Send to dead letter queue
            // 2. Implement retry logic
            // 3. Alert monitoring systems
            
            // For now, we'll acknowledge to prevent infinite retries
            // In a real scenario, you'd implement proper error handling
            acknowledgment.acknowledge();
        }
    }
    
    /**
     * Spring Cloud Stream consumer function for EDI orders
     * Alternative to @KafkaListener, uses Spring Cloud Stream
     */
    @Bean
    public Consumer<Message<String>> ediParcelOrders() {
        return message -> {
            try {
                String ediOrderJson = message.getPayload();
                String correlationId = (String) message.getHeaders().get("correlationId");
                
                logger.info("Received EDI parcel order via Spring Cloud Stream, correlationId: {}", correlationId);
                
                // Parse and process the EDI order
                EDIParcelOrderDTO ediOrder = objectMapper.readValue(ediOrderJson, EDIParcelOrderDTO.class);
                
                // Set correlation ID if provided
                if (correlationId != null) {
                    ediOrder.setCorrelationId(correlationId);
                }
                
                // Process the order
                processEDIOrder(ediOrder);
                
            } catch (Exception e) {
                logger.error("Error processing EDI order via Spring Cloud Stream", e);
                throw new RuntimeException("Failed to process EDI order", e);
            }
        };
    }
    
    /**
     * Process EDI order and save to database
     */
    @Transactional
    public Parcel processEDIOrder(EDIParcelOrderDTO ediOrder) {
        try {
            logger.info("Processing EDI order with reference: {}", ediOrder.getEdiReference());
            
            // Validate the EDI order
            validateEDIOrder(ediOrder);
            
            // Save parcel to database using ParcelService
            Parcel savedParcel = parcelService.createParcelFromEDI(ediOrder);
            
            logger.info("Successfully saved parcel with ID: {} to database", savedParcel.getParcelId());
            
            // Publish event to ABC transport topic
            publishToABCTransport(savedParcel);
            
            return savedParcel;
            
        } catch (Exception e) {
            logger.error("Error processing EDI order with reference: {}", ediOrder.getEdiReference(), e);
            throw new RuntimeException("Failed to process EDI order", e);
        }
    }
    
    /**
     * Validate EDI order before processing
     */
    private void validateEDIOrder(EDIParcelOrderDTO ediOrder) {
        if (ediOrder == null) {
            throw new IllegalArgumentException("EDI order cannot be null");
        }
        
        if (ediOrder.getEdiReference() == null || ediOrder.getEdiReference().trim().isEmpty()) {
            throw new IllegalArgumentException("EDI reference is required");
        }
        
        if (ediOrder.getSender() == null) {
            throw new IllegalArgumentException("Sender information is required");
        }
        
        if (ediOrder.getRecipient() == null) {
            throw new IllegalArgumentException("Recipient information is required");
        }
        
        if (ediOrder.getPickupAddress() == null) {
            throw new IllegalArgumentException("Pickup address is required");
        }
        
        if (ediOrder.getDeliveryAddress() == null) {
            throw new IllegalArgumentException("Delivery address is required");
        }
        
        // Validate sender details
        validateCustomer(ediOrder.getSender(), "Sender");
        
        // Validate recipient details
        validateCustomer(ediOrder.getRecipient(), "Recipient");
        
        // Validate addresses
        validateAddress(ediOrder.getPickupAddress(), "Pickup address");
        validateAddress(ediOrder.getDeliveryAddress(), "Delivery address");
        
        logger.debug("EDI order validation passed for reference: {}", ediOrder.getEdiReference());
    }
    
    /**
     * Validate customer information
     */
    private void validateCustomer(EDIParcelOrderDTO.CustomerDTO customer, String customerType) {
        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new IllegalArgumentException(customerType + " name is required");
        }
        
        if (customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException(customerType + " email is required");
        }
        
        // Basic email validation
        if (!customer.getEmail().contains("@")) {
            throw new IllegalArgumentException(customerType + " email format is invalid");
        }
    }
    
    /**
     * Validate address information
     */
    private void validateAddress(EDIParcelOrderDTO.AddressDTO address, String addressType) {
        if (address.getStreetAddress() == null || address.getStreetAddress().trim().isEmpty()) {
            throw new IllegalArgumentException(addressType + " street address is required");
        }
        
        if (address.getCity() == null || address.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException(addressType + " city is required");
        }
        
        if (address.getPostalCode() == null || address.getPostalCode().trim().isEmpty()) {
            throw new IllegalArgumentException(addressType + " postal code is required");
        }
        
        if (address.getCountry() == null || address.getCountry().trim().isEmpty()) {
            throw new IllegalArgumentException(addressType + " country is required");
        }
    }
    
    /**
     * Publish parcel information to ABC Transport topic
     */
    private void publishToABCTransport(Parcel savedParcel) {
        try {
            logger.info("Publishing parcel {} to ABC Transport topic", savedParcel.getParcelId());
            
            // Use EventStreamService to publish to ABC transport topic
            eventStreamService.sendToABCTransport(savedParcel);
            
            logger.info("Successfully published parcel {} to ABC Transport topic", savedParcel.getParcelId());
            
        } catch (Exception e) {
            logger.error("Failed to publish parcel {} to ABC Transport topic", savedParcel.getParcelId(), e);
            
            // In production, you might want to:
            // 1. Retry the publication
            // 2. Store in a retry queue
            // 3. Alert monitoring systems
            // 4. Mark parcel for manual processing
            
            throw new RuntimeException("Failed to publish to ABC Transport topic", e);
        }
    }
    
    /**
     * Manual processing method for testing or retry scenarios
     */
    public Parcel processEDIOrderManually(String ediOrderJson) {
        try {
            EDIParcelOrderDTO ediOrder = objectMapper.readValue(ediOrderJson, EDIParcelOrderDTO.class);
            return processEDIOrder(ediOrder);
        } catch (Exception e) {
            logger.error("Error in manual EDI order processing", e);
            throw new RuntimeException("Failed to process EDI order manually", e);
        }
    }
}
