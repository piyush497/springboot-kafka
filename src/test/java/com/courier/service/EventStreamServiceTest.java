package com.courier.service;

import com.courier.entity.*;
import com.courier.event.ABCTransportEvent;
import com.courier.event.TrackingEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventStreamServiceTest {
    
    @Mock
    private StreamBridge streamBridge;
    
    private EventStreamService eventStreamService;
    
    @BeforeEach
    void setUp() {
        eventStreamService = new EventStreamService(streamBridge);
    }
    
    @Test
    void testSendToABCTransport() {
        // Arrange
        Parcel parcel = createTestParcel();
        when(streamBridge.send(eq("abcTransport-out-0"), any(Message.class))).thenReturn(true);
        
        // Act
        eventStreamService.sendToABCTransport(parcel);
        
        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(streamBridge).send(eq("abcTransport-out-0"), messageCaptor.capture());
        
        Message<ABCTransportEvent> sentMessage = messageCaptor.getValue();
        ABCTransportEvent event = sentMessage.getPayload();
        
        assertNotNull(event);
        assertEquals("ABC_TRANSPORT_EVENT", event.getEventType());
        assertEquals(parcel.getParcelId(), event.getParcelId());
        assertEquals("PARCEL_REGISTRATION", event.getMessageType());
        assertEquals(parcel.getStatus().toString(), event.getStatus());
        assertEquals(parcel.getPriority().toString(), event.getPriority());
        
        // Verify headers
        assertEquals(parcel.getParcelId(), sentMessage.getHeaders().get("partitionKey"));
        assertEquals("PARCEL_REGISTRATION", sentMessage.getHeaders().get("eventType"));
        assertNotNull(sentMessage.getHeaders().get("correlationId"));
    }
    
    @Test
    void testSendTrackingEvent() {
        // Arrange
        Parcel parcel = createTestParcel();
        TrackingEvent trackingEvent = createTestTrackingEvent(parcel);
        when(streamBridge.send(eq("parcelEvents-out-0"), any(Message.class))).thenReturn(true);
        
        // Act
        eventStreamService.sendTrackingEvent(trackingEvent);
        
        // Assert
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(streamBridge).send(eq("parcelEvents-out-0"), messageCaptor.capture());
        
        Message<TrackingEventMessage> sentMessage = messageCaptor.getValue();
        TrackingEventMessage eventMessage = sentMessage.getPayload();
        
        assertNotNull(eventMessage);
        assertEquals("TRACKING_EVENT", eventMessage.getEventType());
        assertEquals(parcel.getParcelId(), eventMessage.getParcelId());
        assertEquals(trackingEvent.getEventType().toString(), eventMessage.getTrackingEventType());
        assertEquals(trackingEvent.getDescription(), eventMessage.getDescription());
        assertEquals(trackingEvent.getLocation(), eventMessage.getLocation());
        
        // Verify headers
        assertEquals(parcel.getParcelId(), sentMessage.getHeaders().get("partitionKey"));
        assertEquals("TRACKING_UPDATE", sentMessage.getHeaders().get("eventType"));
        assertNotNull(sentMessage.getHeaders().get("correlationId"));
    }
    
    @Test
    void testSendToABCTransportFailure() {
        // Arrange
        Parcel parcel = createTestParcel();
        when(streamBridge.send(eq("abcTransport-out-0"), any(Message.class))).thenReturn(false);
        
        // Act & Assert - should not throw exception, just log error
        assertDoesNotThrow(() -> eventStreamService.sendToABCTransport(parcel));
        
        verify(streamBridge).send(eq("abcTransport-out-0"), any(Message.class));
    }
    
    @Test
    void testSendTrackingEventFailure() {
        // Arrange
        Parcel parcel = createTestParcel();
        TrackingEvent trackingEvent = createTestTrackingEvent(parcel);
        when(streamBridge.send(eq("parcelEvents-out-0"), any(Message.class))).thenReturn(false);
        
        // Act & Assert - should not throw exception, just log error
        assertDoesNotThrow(() -> eventStreamService.sendTrackingEvent(trackingEvent));
        
        verify(streamBridge).send(eq("parcelEvents-out-0"), any(Message.class));
    }
    
    private Parcel createTestParcel() {
        Customer sender = new Customer();
        sender.setId(1L);
        sender.setName("John Doe");
        sender.setEmail("john@example.com");
        sender.setPhone("+1234567890");
        sender.setCustomerCode("CUST-001");
        
        Customer recipient = new Customer();
        recipient.setId(2L);
        recipient.setName("Jane Smith");
        recipient.setEmail("jane@example.com");
        recipient.setPhone("+0987654321");
        recipient.setCustomerCode("CUST-002");
        
        Address pickupAddress = new Address();
        pickupAddress.setStreetAddress("123 Main St");
        pickupAddress.setCity("New York");
        pickupAddress.setState("NY");
        pickupAddress.setPostalCode("10001");
        pickupAddress.setCountry("USA");
        pickupAddress.setAddressType(Address.AddressType.PICKUP);
        
        Address deliveryAddress = new Address();
        deliveryAddress.setStreetAddress("456 Oak Ave");
        deliveryAddress.setCity("Los Angeles");
        deliveryAddress.setState("CA");
        deliveryAddress.setPostalCode("90001");
        deliveryAddress.setCountry("USA");
        deliveryAddress.setAddressType(Address.AddressType.DELIVERY);
        
        Parcel parcel = new Parcel("PKG-TEST-123", sender, recipient, pickupAddress, deliveryAddress);
        parcel.setId(1L);
        parcel.setEdiReference("EDI-REF-123");
        parcel.setDescription("Test package");
        parcel.setWeight(2.5);
        parcel.setDimensions("10x10x10");
        parcel.setStatus(Parcel.ParcelStatus.REGISTERED);
        parcel.setPriority(Parcel.Priority.STANDARD);
        parcel.setCreatedAt(LocalDateTime.now());
        
        return parcel;
    }
    
    private TrackingEvent createTestTrackingEvent(Parcel parcel) {
        TrackingEvent trackingEvent = new TrackingEvent(
            parcel,
            TrackingEvent.EventType.LOADED_IN_TRUCK,
            "Parcel loaded in delivery truck",
            "Distribution Center NYC"
        );
        trackingEvent.setId(1L);
        trackingEvent.setVehicleId("TRUCK-001");
        trackingEvent.setDriverName("Mike Johnson");
        trackingEvent.setAdditionalInfo("Ready for delivery");
        trackingEvent.setEventTimestamp(LocalDateTime.now());
        
        return trackingEvent;
    }
}
