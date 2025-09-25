package com.courier.service;

import com.courier.entity.Parcel;
import com.courier.entity.TrackingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service to consume responses from ABC Transport system
 */
@Service
public class ABCTransportConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ABCTransportConsumerService.class);
    
    @Autowired
    private ParcelService parcelService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Consumer function for ABC Transport responses
     * This will be automatically bound to the abcTransportResponse-in-0 binding
     */
    @Bean
    public Consumer<Message<String>> abcTransportResponse() {
        return message -> {
            try {
                String payload = message.getPayload();
                logger.info("Received ABC Transport response: {}", payload);
                
                JsonNode responseNode = objectMapper.readTree(payload);
                processABCTransportResponse(responseNode, message);
                
            } catch (Exception e) {
                logger.error("Error processing ABC Transport response", e);
                // In production, you might want to send to a dead letter queue
            }
        };
    }
    
    /**
     * Consumer function for internal parcel events
     * This will be automatically bound to the parcelEvents-in-0 binding
     */
    @Bean
    public Consumer<Message<String>> parcelEvents() {
        return message -> {
            try {
                String payload = message.getPayload();
                logger.info("Received internal parcel event: {}", payload);
                
                JsonNode eventNode = objectMapper.readTree(payload);
                processInternalParcelEvent(eventNode, message);
                
            } catch (Exception e) {
                logger.error("Error processing internal parcel event", e);
            }
        };
    }
    
    private void processABCTransportResponse(JsonNode responseNode, Message<String> message) {
        try {
            String messageType = responseNode.path("messageType").asText();
            String parcelId = responseNode.path("parcelId").asText();
            
            if (parcelId.isEmpty()) {
                logger.warn("Received ABC Transport response without parcelId");
                return;
            }
            
            Optional<Parcel> parcelOpt = parcelService.findByParcelId(parcelId);
            if (parcelOpt.isEmpty()) {
                logger.warn("Received ABC Transport response for unknown parcel: {}", parcelId);
                return;
            }
            
            Parcel parcel = parcelOpt.get();
            
            switch (messageType) {
                case "PICKUP_SCHEDULED":
                    handlePickupScheduled(parcel, responseNode);
                    break;
                case "PARCEL_PICKED_UP":
                    handleParcelPickedUp(parcel, responseNode);
                    break;
                case "PARCEL_IN_TRANSIT":
                    handleParcelInTransit(parcel, responseNode);
                    break;
                case "PARCEL_LOADED_IN_TRUCK":
                    handleParcelLoadedInTruck(parcel, responseNode);
                    break;
                case "PARCEL_OUT_FOR_DELIVERY":
                    handleParcelOutForDelivery(parcel, responseNode);
                    break;
                case "PARCEL_DELIVERED":
                    handleParcelDelivered(parcel, responseNode);
                    break;
                case "DELIVERY_FAILED":
                    handleDeliveryFailed(parcel, responseNode);
                    break;
                case "PARCEL_RETURNED":
                    handleParcelReturned(parcel, responseNode);
                    break;
                default:
                    logger.warn("Unknown message type from ABC Transport: {}", messageType);
            }
            
        } catch (Exception e) {
            logger.error("Error processing ABC Transport response", e);
        }
    }
    
    private void processInternalParcelEvent(JsonNode eventNode, Message<String> message) {
        try {
            String eventType = eventNode.path("eventType").asText();
            String parcelId = eventNode.path("parcelId").asText();
            
            logger.info("Processing internal event {} for parcel {}", eventType, parcelId);
            
            // Add any internal event processing logic here
            // For example, updating analytics, sending notifications, etc.
            
        } catch (Exception e) {
            logger.error("Error processing internal parcel event", e);
        }
    }
    
    private void handlePickupScheduled(Parcel parcel, JsonNode responseNode) {
        String scheduledTime = responseNode.path("scheduledPickupTime").asText();
        String vehicleId = responseNode.path("vehicleId").asText();
        String driverName = responseNode.path("driverName").asText();
        
        parcelService.createTrackingEvent(
            parcel,
            TrackingEvent.EventType.PICKUP_SCHEDULED,
            "Pickup scheduled by ABC Transport for " + scheduledTime,
            null,
            "Vehicle: " + vehicleId + ", Driver: " + driverName
        );
        
        logger.info("Pickup scheduled for parcel {} at {}", parcel.getParcelId(), scheduledTime);
    }
    
    private void handleParcelPickedUp(Parcel parcel, JsonNode responseNode) {
        String location = responseNode.path("location").asText();
        String vehicleId = responseNode.path("vehicleId").asText();
        String driverName = responseNode.path("driverName").asText();
        
        parcelService.updateParcelStatus(
            parcel.getParcelId(),
            Parcel.ParcelStatus.PICKED_UP,
            location,
            "Picked up by " + driverName + " (Vehicle: " + vehicleId + ")"
        );
        
        logger.info("Parcel {} picked up at {}", parcel.getParcelId(), location);
    }
    
    private void handleParcelInTransit(Parcel parcel, JsonNode responseNode) {
        String location = responseNode.path("location").asText();
        String vehicleId = responseNode.path("vehicleId").asText();
        
        parcelService.updateParcelStatus(
            parcel.getParcelId(),
            Parcel.ParcelStatus.IN_TRANSIT,
            location,
            "In transit via vehicle: " + vehicleId
        );
        
        logger.info("Parcel {} in transit at {}", parcel.getParcelId(), location);
    }
    
    private void handleParcelLoadedInTruck(Parcel parcel, JsonNode responseNode) {
        String location = responseNode.path("location").asText();
        String vehicleId = responseNode.path("vehicleId").asText();
        String driverName = responseNode.path("driverName").asText();
        
        parcelService.updateParcelStatus(
            parcel.getParcelId(),
            Parcel.ParcelStatus.LOADED_IN_TRUCK,
            location,
            "Loaded in delivery truck. Driver: " + driverName + ", Vehicle: " + vehicleId
        );
        
        logger.info("Parcel {} loaded in truck {} at {}", parcel.getParcelId(), vehicleId, location);
    }
    
    private void handleParcelOutForDelivery(Parcel parcel, JsonNode responseNode) {
        String location = responseNode.path("location").asText();
        String vehicleId = responseNode.path("vehicleId").asText();
        String driverName = responseNode.path("driverName").asText();
        String estimatedDelivery = responseNode.path("estimatedDeliveryTime").asText();
        
        parcelService.updateParcelStatus(
            parcel.getParcelId(),
            Parcel.ParcelStatus.OUT_FOR_DELIVERY,
            location,
            "Out for delivery. ETA: " + estimatedDelivery + ", Driver: " + driverName
        );
        
        logger.info("Parcel {} out for delivery, ETA: {}", parcel.getParcelId(), estimatedDelivery);
    }
    
    private void handleParcelDelivered(Parcel parcel, JsonNode responseNode) {
        String location = responseNode.path("deliveryLocation").asText();
        String recipientName = responseNode.path("recipientName").asText();
        String deliveryTime = responseNode.path("deliveryTime").asText();
        String signature = responseNode.path("signature").asText();
        
        parcelService.updateParcelStatus(
            parcel.getParcelId(),
            Parcel.ParcelStatus.DELIVERED,
            location,
            "Delivered to " + recipientName + " at " + deliveryTime + 
            (signature != null && !signature.isEmpty() ? ". Signature: " + signature : "")
        );
        
        logger.info("Parcel {} delivered to {} at {}", parcel.getParcelId(), recipientName, location);
    }
    
    private void handleDeliveryFailed(Parcel parcel, JsonNode responseNode) {
        String location = responseNode.path("location").asText();
        String reason = responseNode.path("failureReason").asText();
        String nextAttempt = responseNode.path("nextAttemptTime").asText();
        
        parcelService.updateParcelStatus(
            parcel.getParcelId(),
            Parcel.ParcelStatus.FAILED_DELIVERY,
            location,
            "Delivery failed: " + reason + 
            (nextAttempt != null && !nextAttempt.isEmpty() ? ". Next attempt: " + nextAttempt : "")
        );
        
        logger.info("Delivery failed for parcel {} at {}: {}", parcel.getParcelId(), location, reason);
    }
    
    private void handleParcelReturned(Parcel parcel, JsonNode responseNode) {
        String location = responseNode.path("location").asText();
        String reason = responseNode.path("returnReason").asText();
        
        parcelService.updateParcelStatus(
            parcel.getParcelId(),
            Parcel.ParcelStatus.RETURNED,
            location,
            "Returned to facility: " + reason
        );
        
        logger.info("Parcel {} returned to {} due to: {}", parcel.getParcelId(), location, reason);
    }
}
