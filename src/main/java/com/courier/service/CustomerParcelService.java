package com.courier.service;

import com.courier.dto.EDIParcelOrderDTO;
import com.courier.entity.Parcel;
import com.courier.entity.TrackingEvent;
import com.courier.entity.User;
import com.courier.repository.ParcelRepository;
import com.courier.repository.TrackingEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service layer for customer interface operations
 * Handles customer-specific parcel operations and business logic
 */
@Service
@Transactional
public class CustomerParcelService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerParcelService.class);
    
    @Autowired
    private ParcelService parcelService;
    
    @Autowired
    private ParcelRepository parcelRepository;
    
    @Autowired
    private TrackingEventRepository trackingEventRepository;
    
    @Autowired
    private StreamBridge streamBridge;
    
    @Value("${customer-interface.limits.max-parcels-per-day:100}")
    private int maxParcelsPerDay;
    
    @Value("${customer-interface.features.parcel-registration:true}")
    private boolean parcelRegistrationEnabled;
    
    /**
     * Register a new parcel for a customer
     */
    public Parcel registerParcel(EDIParcelOrderDTO parcelOrder, User customer) {
        logger.info("Registering parcel for customer: {}", customer.getUsername());
        
        if (!parcelRegistrationEnabled) {
            throw new RuntimeException("Parcel registration is currently disabled");
        }
        
        // Validate customer limits
        validateCustomerLimits(customer.getId());
        
        // Set customer information in the parcel order
        enrichParcelOrderWithCustomerInfo(parcelOrder, customer);
        
        // Create parcel using the main parcel service
        Parcel registeredParcel = parcelService.createParcelFromEDI(parcelOrder);
        
        // Send customer-specific event
        sendCustomerParcelEvent(registeredParcel, customer, "PARCEL_REGISTERED");
        
        logger.info("Parcel {} registered successfully for customer {}", 
                   registeredParcel.getParcelId(), customer.getUsername());
        
        return registeredParcel;
    }
    
    /**
     * Get customer's parcels with optional status filter
     */
    public Page<Parcel> getCustomerParcels(Long customerId, String status, Pageable pageable) {
        logger.debug("Fetching parcels for customer ID: {} with status: {}", customerId, status);
        
        if (status != null && !status.trim().isEmpty()) {
            try {
                Parcel.ParcelStatus parcelStatus = Parcel.ParcelStatus.valueOf(status.toUpperCase());
                return parcelRepository.findByCustomerIdAndStatus(customerId, parcelStatus, pageable);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status filter: {}", status);
                return parcelRepository.findByCustomerId(customerId, pageable);
            }
        }
        
        return parcelRepository.findByCustomerId(customerId, pageable);
    }
    
    /**
     * Get a specific parcel for a customer (with ownership verification)
     */
    public Optional<Parcel> getCustomerParcel(Long customerId, String parcelId) {
        logger.debug("Fetching parcel {} for customer ID: {}", parcelId, customerId);
        
        Optional<Parcel> parcelOpt = parcelRepository.findByParcelId(parcelId);
        
        if (parcelOpt.isPresent()) {
            Parcel parcel = parcelOpt.get();
            // Verify customer ownership
            if (parcel.getSender().getId().equals(customerId)) {
                return parcelOpt;
            } else {
                logger.warn("Customer {} attempted to access parcel {} they don't own", customerId, parcelId);
                return Optional.empty();
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Cancel a parcel if it's in a cancellable state
     */
    public boolean cancelParcel(Long customerId, String parcelId, String reason) {
        logger.info("Cancel request for parcel {} from customer ID: {}", parcelId, customerId);
        
        Optional<Parcel> parcelOpt = getCustomerParcel(customerId, parcelId);
        
        if (parcelOpt.isEmpty()) {
            throw new RuntimeException("Parcel not found or access denied");
        }
        
        Parcel parcel = parcelOpt.get();
        
        // Check if parcel can be cancelled
        if (!canBeCancelled(parcel.getStatus())) {
            logger.warn("Parcel {} cannot be cancelled in status: {}", parcelId, parcel.getStatus());
            return false;
        }
        
        // Update parcel status
        parcel.setStatus(Parcel.ParcelStatus.CANCELLED);
        parcelRepository.save(parcel);
        
        // Create tracking event
        parcelService.createTrackingEvent(
            parcel,
            TrackingEvent.EventType.CANCELLED,
            "Parcel cancelled by customer: " + reason,
            null,
            reason
        );
        
        // Send cancellation event
        sendCustomerParcelEvent(parcel, null, "PARCEL_CANCELLED");
        
        logger.info("Parcel {} cancelled successfully", parcelId);
        return true;
    }
    
    /**
     * Get customer dashboard data
     */
    public Map<String, Object> getCustomerDashboard(Long customerId) {
        logger.debug("Generating dashboard for customer ID: {}", customerId);
        
        Map<String, Object> dashboard = new HashMap<>();
        
        // Get parcel counts by status
        Map<String, Long> statusCounts = new HashMap<>();
        for (Parcel.ParcelStatus status : Parcel.ParcelStatus.values()) {
            long count = parcelRepository.countByCustomerIdAndStatus(customerId, status);
            statusCounts.put(status.toString(), count);
        }
        dashboard.put("parcelsByStatus", statusCounts);
        
        // Get total parcels
        long totalParcels = parcelRepository.countByCustomerId(customerId);
        dashboard.put("totalParcels", totalParcels);
        
        // Get recent parcels (last 5)
        Pageable recentParcelsPageable = org.springframework.data.domain.PageRequest.of(0, 5, 
            org.springframework.data.domain.Sort.by("createdAt").descending());
        Page<Parcel> recentParcels = parcelRepository.findByCustomerId(customerId, recentParcelsPageable);
        dashboard.put("recentParcels", recentParcels.getContent());
        
        // Get parcels created this month
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long thisMonthCount = parcelRepository.countByCustomerIdAndCreatedAtAfter(customerId, startOfMonth);
        dashboard.put("parcelsThisMonth", thisMonthCount);
        
        // Get active deliveries (in transit, out for delivery)
        long activeDeliveries = parcelRepository.countByCustomerIdAndStatusIn(customerId, 
            java.util.List.of(Parcel.ParcelStatus.IN_TRANSIT, Parcel.ParcelStatus.OUT_FOR_DELIVERY));
        dashboard.put("activeDeliveries", activeDeliveries);
        
        return dashboard;
    }
    
    /**
     * Validate customer limits (daily parcel limit, etc.)
     */
    public void validateCustomerLimits(Long customerId) {
        logger.debug("Validating limits for customer ID: {}", customerId);
        
        // Check daily parcel limit
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayCount = parcelRepository.countByCustomerIdAndCreatedAtAfter(customerId, startOfDay);
        
        if (todayCount >= maxParcelsPerDay) {
            throw new RuntimeException("Daily parcel limit exceeded. Maximum " + maxParcelsPerDay + " parcels per day.");
        }
        
        logger.debug("Customer {} has created {} parcels today (limit: {})", 
                    customerId, todayCount, maxParcelsPerDay);
    }
    
    /**
     * Enrich parcel order with customer information
     */
    private void enrichParcelOrderWithCustomerInfo(EDIParcelOrderDTO parcelOrder, User customer) {
        // Set sender information from customer profile
        if (parcelOrder.getSender() == null) {
            parcelOrder.setSender(new EDIParcelOrderDTO.CustomerDTO());
        }
        
        EDIParcelOrderDTO.CustomerDTO sender = parcelOrder.getSender();
        if (sender.getName() == null || sender.getName().trim().isEmpty()) {
            sender.setName(customer.getFullName());
        }
        if (sender.getEmail() == null || sender.getEmail().trim().isEmpty()) {
            sender.setEmail(customer.getEmail());
        }
        if (sender.getPhone() == null || sender.getPhone().trim().isEmpty()) {
            sender.setPhone(customer.getPhone());
        }
        
        // Generate EDI reference if not provided
        if (parcelOrder.getEdiReference() == null || parcelOrder.getEdiReference().trim().isEmpty()) {
            parcelOrder.setEdiReference("CUST-" + customer.getId() + "-" + System.currentTimeMillis());
        }
    }
    
    /**
     * Send customer-specific parcel event
     */
    private void sendCustomerParcelEvent(Parcel parcel, User customer, String eventType) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("parcelId", parcel.getParcelId());
            event.put("customerId", parcel.getSender().getId());
            event.put("customerEmail", parcel.getSender().getEmail());
            event.put("status", parcel.getStatus().toString());
            event.put("timestamp", LocalDateTime.now());
            
            if (customer != null) {
                event.put("customerUsername", customer.getUsername());
            }
            
            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(event)
                    .setHeader("customerId", parcel.getSender().getId())
                    .setHeader("eventType", eventType)
                    .build();
            
            streamBridge.send("customerParcels-out-0", message);
            
            logger.debug("Sent customer parcel event: {} for parcel: {}", eventType, parcel.getParcelId());
            
        } catch (Exception e) {
            logger.error("Failed to send customer parcel event for parcel: {}", parcel.getParcelId(), e);
        }
    }
    
    /**
     * Check if a parcel can be cancelled based on its current status
     */
    private boolean canBeCancelled(Parcel.ParcelStatus status) {
        return status == Parcel.ParcelStatus.REGISTERED || 
               status == Parcel.ParcelStatus.PICKED_UP;
    }
}
