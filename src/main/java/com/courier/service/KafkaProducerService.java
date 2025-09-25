package com.courier.service;

import com.courier.entity.Parcel;
import com.courier.entity.TrackingEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Value("${kafka.topics.abc-transport}")
    private String abcTransportTopic;
    
    @Value("${kafka.topics.tracking-events}")
    private String trackingEventsTopic;
    
    private final ObjectMapper objectMapper;
    
    public KafkaProducerService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public void sendToABCTransport(Parcel parcel) {
        try {
            Map<String, Object> abcTransportMessage = createABCTransportMessage(parcel);
            String messageJson = objectMapper.writeValueAsString(abcTransportMessage);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(abcTransportTopic, parcel.getParcelId(), messageJson);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.info("Sent parcel {} to ABC Transport system with offset: {}", 
                        parcel.getParcelId(), result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send parcel {} to ABC Transport system", 
                        parcel.getParcelId(), exception);
                }
            });
            
        } catch (JsonProcessingException e) {
            logger.error("Error serializing parcel {} for ABC Transport", parcel.getParcelId(), e);
        }
    }
    
    public void sendTrackingEvent(TrackingEvent trackingEvent) {
        try {
            Map<String, Object> trackingMessage = createTrackingEventMessage(trackingEvent);
            String messageJson = objectMapper.writeValueAsString(trackingMessage);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(trackingEventsTopic, trackingEvent.getParcel().getParcelId(), messageJson);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.info("Sent tracking event for parcel {} with offset: {}", 
                        trackingEvent.getParcel().getParcelId(), result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send tracking event for parcel {}", 
                        trackingEvent.getParcel().getParcelId(), exception);
                }
            });
            
        } catch (JsonProcessingException e) {
            logger.error("Error serializing tracking event for parcel {}", 
                trackingEvent.getParcel().getParcelId(), e);
        }
    }
    
    private Map<String, Object> createABCTransportMessage(Parcel parcel) {
        Map<String, Object> message = new HashMap<>();
        message.put("messageType", "PARCEL_REGISTRATION");
        message.put("timestamp", LocalDateTime.now());
        message.put("parcelId", parcel.getParcelId());
        message.put("ediReference", parcel.getEdiReference());
        message.put("status", parcel.getStatus().toString());
        message.put("priority", parcel.getPriority().toString());
        
        // Sender information
        Map<String, Object> sender = new HashMap<>();
        sender.put("name", parcel.getSender().getName());
        sender.put("email", parcel.getSender().getEmail());
        sender.put("phone", parcel.getSender().getPhone());
        message.put("sender", sender);
        
        // Recipient information
        Map<String, Object> recipient = new HashMap<>();
        recipient.put("name", parcel.getRecipient().getName());
        recipient.put("email", parcel.getRecipient().getEmail());
        recipient.put("phone", parcel.getRecipient().getPhone());
        message.put("recipient", recipient);
        
        // Pickup address
        Map<String, Object> pickupAddress = new HashMap<>();
        pickupAddress.put("streetAddress", parcel.getPickupAddress().getStreetAddress());
        pickupAddress.put("city", parcel.getPickupAddress().getCity());
        pickupAddress.put("state", parcel.getPickupAddress().getState());
        pickupAddress.put("postalCode", parcel.getPickupAddress().getPostalCode());
        pickupAddress.put("country", parcel.getPickupAddress().getCountry());
        message.put("pickupAddress", pickupAddress);
        
        // Delivery address
        Map<String, Object> deliveryAddress = new HashMap<>();
        deliveryAddress.put("streetAddress", parcel.getDeliveryAddress().getStreetAddress());
        deliveryAddress.put("city", parcel.getDeliveryAddress().getCity());
        deliveryAddress.put("state", parcel.getDeliveryAddress().getState());
        deliveryAddress.put("postalCode", parcel.getDeliveryAddress().getPostalCode());
        deliveryAddress.put("country", parcel.getDeliveryAddress().getCountry());
        message.put("deliveryAddress", deliveryAddress);
        
        // Parcel details
        Map<String, Object> parcelDetails = new HashMap<>();
        parcelDetails.put("description", parcel.getDescription());
        parcelDetails.put("weight", parcel.getWeight());
        parcelDetails.put("dimensions", parcel.getDimensions());
        message.put("parcelDetails", parcelDetails);
        
        return message;
    }
    
    private Map<String, Object> createTrackingEventMessage(TrackingEvent trackingEvent) {
        Map<String, Object> message = new HashMap<>();
        message.put("messageType", "TRACKING_EVENT");
        message.put("timestamp", LocalDateTime.now());
        message.put("parcelId", trackingEvent.getParcel().getParcelId());
        message.put("eventType", trackingEvent.getEventType().toString());
        message.put("description", trackingEvent.getDescription());
        message.put("eventTimestamp", trackingEvent.getEventTimestamp());
        message.put("location", trackingEvent.getLocation());
        message.put("vehicleId", trackingEvent.getVehicleId());
        message.put("driverName", trackingEvent.getDriverName());
        message.put("additionalInfo", trackingEvent.getAdditionalInfo());
        
        return message;
    }
}
