package com.courier.service;

import com.courier.dto.EDIParcelOrderDTO;
import com.courier.entity.*;
import com.courier.repository.CustomerRepository;
import com.courier.repository.ParcelRepository;
import com.courier.repository.TrackingEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ParcelService {
    
    @Autowired
    private ParcelRepository parcelRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private TrackingEventRepository trackingEventRepository;
    
    @Autowired
    private KafkaProducerService kafkaProducerService;
    
    @Autowired
    private EventStreamService eventStreamService;
    
    public Parcel createParcelFromEDI(EDIParcelOrderDTO ediOrder) {
        // Find or create sender
        Customer sender = findOrCreateCustomer(ediOrder.getSender());
        
        // Find or create recipient
        Customer recipient = findOrCreateCustomer(ediOrder.getRecipient());
        
        // Create addresses
        Address pickupAddress = createAddressFromDTO(ediOrder.getPickupAddress());
        pickupAddress.setAddressType(Address.AddressType.PICKUP);
        
        Address deliveryAddress = createAddressFromDTO(ediOrder.getDeliveryAddress());
        deliveryAddress.setAddressType(Address.AddressType.DELIVERY);
        
        // Generate parcel ID if not provided
        String parcelId = ediOrder.getParcelId() != null ? 
            ediOrder.getParcelId() : generateParcelId();
        
        // Create parcel
        Parcel parcel = new Parcel(parcelId, sender, recipient, pickupAddress, deliveryAddress);
        parcel.setEdiReference(ediOrder.getEdiReference());
        
        // Set parcel details if provided
        if (ediOrder.getParcelDetails() != null) {
            parcel.setDescription(ediOrder.getParcelDetails().getDescription());
            if (ediOrder.getParcelDetails().getWeight() != null) {
                parcel.setWeight(BigDecimal.valueOf(ediOrder.getParcelDetails().getWeight()));
            }
            parcel.setDimensions(ediOrder.getParcelDetails().getDimensions());
        }
        
        // Set service options if provided
        if (ediOrder.getServiceOptions() != null) {
            if (ediOrder.getServiceOptions().getPriority() != null) {
                try {
                    parcel.setPriority(Parcel.Priority.valueOf(
                        ediOrder.getServiceOptions().getPriority().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    parcel.setPriority(Parcel.Priority.STANDARD);
                }
            }
            parcel.setEstimatedDeliveryDate(ediOrder.getServiceOptions().getEstimatedDeliveryDate());
        }
        
        // Save parcel
        Parcel savedParcel = parcelRepository.save(parcel);
        
        // Create initial tracking event
        createTrackingEvent(savedParcel, TrackingEvent.EventType.REGISTERED, 
            "Parcel registered in the system", null);
        
        // Send to ABC transport system using both legacy Kafka and new Stream service
        kafkaProducerService.sendToABCTransport(savedParcel);
        eventStreamService.sendToABCTransport(savedParcel);
        
        return savedParcel;
    }
    
    public Optional<Parcel> findByParcelId(String parcelId) {
        return parcelRepository.findByParcelId(parcelId);
    }
    
    public Optional<Parcel> findByEdiReference(String ediReference) {
        return parcelRepository.findByEdiReference(ediReference);
    }
    
    public List<Parcel> findByCustomerId(Long customerId) {
        return parcelRepository.findByCustomerId(customerId);
    }
    
    public List<Parcel> findByStatus(Parcel.ParcelStatus status) {
        return parcelRepository.findByStatus(status);
    }
    
    public Parcel updateParcelStatus(String parcelId, Parcel.ParcelStatus newStatus, 
                                   String location, String additionalInfo) {
        Optional<Parcel> parcelOpt = parcelRepository.findByParcelId(parcelId);
        if (parcelOpt.isEmpty()) {
            throw new RuntimeException("Parcel not found: " + parcelId);
        }
        
        Parcel parcel = parcelOpt.get();
        Parcel.ParcelStatus oldStatus = parcel.getStatus();
        parcel.setStatus(newStatus);
        
        // Set delivery date if delivered
        if (newStatus == Parcel.ParcelStatus.DELIVERED) {
            parcel.setActualDeliveryDate(LocalDateTime.now());
        }
        
        Parcel savedParcel = parcelRepository.save(parcel);
        
        // Create tracking event
        TrackingEvent.EventType eventType = mapStatusToEventType(newStatus);
        String description = String.format("Status changed from %s to %s", oldStatus, newStatus);
        createTrackingEvent(savedParcel, eventType, description, location, additionalInfo);
        
        return savedParcel;
    }
    
    public void createTrackingEvent(Parcel parcel, TrackingEvent.EventType eventType, 
                                  String description, String location) {
        createTrackingEvent(parcel, eventType, description, location, null);
    }
    
    public void createTrackingEvent(Parcel parcel, TrackingEvent.EventType eventType, 
                                  String description, String location, String additionalInfo) {
        TrackingEvent event = new TrackingEvent(parcel, eventType, description, location);
        event.setAdditionalInfo(additionalInfo);
        trackingEventRepository.save(event);
        
        // Send tracking event using both legacy Kafka and new Stream service
        kafkaProducerService.sendTrackingEvent(event);
        eventStreamService.sendTrackingEvent(event);
    }
    
    public List<TrackingEvent> getTrackingHistory(String parcelId) {
        return trackingEventRepository.findByParcelParcelIdOrderByEventTimestampDesc(parcelId);
    }
    
    private Customer findOrCreateCustomer(com.courier.dto.CustomerDTO customerDTO) {
        // Try to find existing customer by email or customer code
        Optional<Customer> existingCustomer = Optional.empty();
        
        if (customerDTO.getCustomerCode() != null) {
            existingCustomer = customerRepository.findByCustomerCode(customerDTO.getCustomerCode());
        }
        
        if (existingCustomer.isEmpty() && customerDTO.getEmail() != null) {
            existingCustomer = customerRepository.findByEmail(customerDTO.getEmail());
        }
        
        if (existingCustomer.isPresent()) {
            return existingCustomer.get();
        }
        
        // Create new customer
        Customer newCustomer = new Customer();
        newCustomer.setName(customerDTO.getName());
        newCustomer.setEmail(customerDTO.getEmail());
        newCustomer.setPhone(customerDTO.getPhone());
        newCustomer.setCustomerCode(customerDTO.getCustomerCode() != null ? 
            customerDTO.getCustomerCode() : generateCustomerCode());
        
        return customerRepository.save(newCustomer);
    }
    
    private Address createAddressFromDTO(com.courier.dto.AddressDTO addressDTO) {
        Address address = new Address();
        address.setStreetAddress(addressDTO.getStreetAddress());
        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setPostalCode(addressDTO.getPostalCode());
        address.setCountry(addressDTO.getCountry());
        address.setLandmark(addressDTO.getLandmark());
        return address;
    }
    
    private String generateParcelId() {
        return "PKG-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private String generateCustomerCode() {
        return "CUST-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    private TrackingEvent.EventType mapStatusToEventType(Parcel.ParcelStatus status) {
        return switch (status) {
            case REGISTERED -> TrackingEvent.EventType.REGISTERED;
            case PICKED_UP -> TrackingEvent.EventType.PICKED_UP;
            case IN_TRANSIT -> TrackingEvent.EventType.IN_TRANSIT;
            case LOADED_IN_TRUCK -> TrackingEvent.EventType.LOADED_IN_TRUCK;
            case OUT_FOR_DELIVERY -> TrackingEvent.EventType.OUT_FOR_DELIVERY;
            case DELIVERED -> TrackingEvent.EventType.DELIVERED;
            case FAILED_DELIVERY -> TrackingEvent.EventType.FAILED_DELIVERY;
            case RETURNED -> TrackingEvent.EventType.RETURNED_TO_FACILITY;
            case CANCELLED -> TrackingEvent.EventType.CANCELLED;
        };
    }
}
