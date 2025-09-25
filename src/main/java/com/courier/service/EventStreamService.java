package com.courier.service;

import com.courier.entity.Parcel;
import com.courier.entity.TrackingEvent;
import com.courier.event.ABCTransportEvent;
import com.courier.event.TrackingEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling event streaming using Spring Cloud Stream
 * Compatible with both Kafka (local) and Azure Event Hub (production)
 */
@Service
public class EventStreamService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventStreamService.class);
    
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;
    
    public EventStreamService(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Send parcel information to ABC Transport system
     */
    public void sendToABCTransport(Parcel parcel) {
        try {
            ABCTransportEvent event = createABCTransportEvent(parcel);
            
            Message<ABCTransportEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader("partitionKey", parcel.getParcelId())
                    .setHeader("eventType", "PARCEL_REGISTRATION")
                    .setHeader("correlationId", event.getCorrelationId())
                    .build();
            
            boolean sent = streamBridge.send("abcTransport-out-0", message);
            
            if (sent) {
                logger.info("Successfully sent parcel {} to ABC Transport system", parcel.getParcelId());
            } else {
                logger.error("Failed to send parcel {} to ABC Transport system", parcel.getParcelId());
            }
            
        } catch (Exception e) {
            logger.error("Error sending parcel {} to ABC Transport system", parcel.getParcelId(), e);
        }
    }
    
    /**
     * Send tracking event for internal processing and external notifications
     */
    public void sendTrackingEvent(TrackingEvent trackingEvent) {
        try {
            TrackingEventMessage eventMessage = createTrackingEventMessage(trackingEvent);
            
            Message<TrackingEventMessage> message = MessageBuilder
                    .withPayload(eventMessage)
                    .setHeader("partitionKey", trackingEvent.getParcel().getParcelId())
                    .setHeader("eventType", "TRACKING_UPDATE")
                    .setHeader("correlationId", eventMessage.getCorrelationId())
                    .build();
            
            boolean sent = streamBridge.send("parcelEvents-out-0", message);
            
            if (sent) {
                logger.info("Successfully sent tracking event for parcel {}", 
                    trackingEvent.getParcel().getParcelId());
            } else {
                logger.error("Failed to send tracking event for parcel {}", 
                    trackingEvent.getParcel().getParcelId());
            }
            
        } catch (Exception e) {
            logger.error("Error sending tracking event for parcel {}", 
                trackingEvent.getParcel().getParcelId(), e);
        }
    }
    
    /**
     * Send raw event with custom binding
     */
    public void sendEvent(String bindingName, Object event, Map<String, Object> headers) {
        try {
            MessageBuilder<Object> messageBuilder = MessageBuilder.withPayload(event);
            
            if (headers != null) {
                headers.forEach(messageBuilder::setHeader);
            }
            
            Message<Object> message = messageBuilder.build();
            boolean sent = streamBridge.send(bindingName, message);
            
            if (sent) {
                logger.info("Successfully sent event to binding: {}", bindingName);
            } else {
                logger.error("Failed to send event to binding: {}", bindingName);
            }
            
        } catch (Exception e) {
            logger.error("Error sending event to binding: {}", bindingName, e);
        }
    }
    
    private ABCTransportEvent createABCTransportEvent(Parcel parcel) {
        ABCTransportEvent event = new ABCTransportEvent("PARCEL_REGISTRATION", parcel.getParcelId());
        event.setEdiReference(parcel.getEdiReference());
        event.setStatus(parcel.getStatus().toString());
        event.setPriority(parcel.getPriority().toString());
        event.setCorrelationId(generateCorrelationId(parcel.getParcelId()));
        
        // Set sender information
        ABCTransportEvent.CustomerInfo sender = new ABCTransportEvent.CustomerInfo(
            parcel.getSender().getName(),
            parcel.getSender().getEmail(),
            parcel.getSender().getPhone()
        );
        event.setSender(sender);
        
        // Set recipient information
        ABCTransportEvent.CustomerInfo recipient = new ABCTransportEvent.CustomerInfo(
            parcel.getRecipient().getName(),
            parcel.getRecipient().getEmail(),
            parcel.getRecipient().getPhone()
        );
        event.setRecipient(recipient);
        
        // Set pickup address
        ABCTransportEvent.AddressInfo pickupAddress = new ABCTransportEvent.AddressInfo();
        pickupAddress.setStreetAddress(parcel.getPickupAddress().getStreetAddress());
        pickupAddress.setCity(parcel.getPickupAddress().getCity());
        pickupAddress.setState(parcel.getPickupAddress().getState());
        pickupAddress.setPostalCode(parcel.getPickupAddress().getPostalCode());
        pickupAddress.setCountry(parcel.getPickupAddress().getCountry());
        event.setPickupAddress(pickupAddress);
        
        // Set delivery address
        ABCTransportEvent.AddressInfo deliveryAddress = new ABCTransportEvent.AddressInfo();
        deliveryAddress.setStreetAddress(parcel.getDeliveryAddress().getStreetAddress());
        deliveryAddress.setCity(parcel.getDeliveryAddress().getCity());
        deliveryAddress.setState(parcel.getDeliveryAddress().getState());
        deliveryAddress.setPostalCode(parcel.getDeliveryAddress().getPostalCode());
        deliveryAddress.setCountry(parcel.getDeliveryAddress().getCountry());
        event.setDeliveryAddress(deliveryAddress);
        
        // Set parcel details
        ABCTransportEvent.ParcelDetails parcelDetails = new ABCTransportEvent.ParcelDetails();
        parcelDetails.setDescription(parcel.getDescription());
        parcelDetails.setWeight(parcel.getWeight());
        parcelDetails.setDimensions(parcel.getDimensions());
        event.setParcelDetails(parcelDetails);
        
        // Set metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("createdAt", parcel.getCreatedAt());
        metadata.put("estimatedDeliveryDate", parcel.getEstimatedDeliveryDate());
        event.setMetadata(metadata);
        
        return event;
    }
    
    private TrackingEventMessage createTrackingEventMessage(TrackingEvent trackingEvent) {
        TrackingEventMessage eventMessage = new TrackingEventMessage(
            trackingEvent.getParcel().getParcelId(),
            trackingEvent.getEventType().toString()
        );
        
        eventMessage.setDescription(trackingEvent.getDescription());
        eventMessage.setEventTimestamp(trackingEvent.getEventTimestamp());
        eventMessage.setLocation(trackingEvent.getLocation());
        eventMessage.setVehicleId(trackingEvent.getVehicleId());
        eventMessage.setDriverName(trackingEvent.getDriverName());
        eventMessage.setAdditionalInfo(trackingEvent.getAdditionalInfo());
        eventMessage.setCurrentStatus(trackingEvent.getParcel().getStatus().toString());
        eventMessage.setCorrelationId(generateCorrelationId(trackingEvent.getParcel().getParcelId()));
        
        // Set metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("trackingEventId", trackingEvent.getId());
        metadata.put("parcelStatus", trackingEvent.getParcel().getStatus());
        metadata.put("parcelPriority", trackingEvent.getParcel().getPriority());
        eventMessage.setMetadata(metadata);
        
        return eventMessage;
    }
    
    private String generateCorrelationId(String parcelId) {
        return parcelId + "-" + System.currentTimeMillis();
    }
}
