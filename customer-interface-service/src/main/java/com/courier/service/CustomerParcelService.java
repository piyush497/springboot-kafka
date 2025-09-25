package com.courier.service;

import com.courier.client.MainServiceClient;
import com.courier.dto.EDIParcelOrderDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service layer for customer interface operations
 * Handles customer-specific parcel operations and business logic
 * Part of the separate Customer Interface Microservice
 */
@Service
public class CustomerParcelService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerParcelService.class);
    
    @Autowired
    private MainServiceClient mainServiceClient;
    
    @Autowired
    private StreamBridge streamBridge;
    
    @Value("${customer-interface.limits.max-parcels-per-day:100}")
    private int maxParcelsPerDay;
    
    @Value("${customer-interface.features.parcel-registration:true}")
    private boolean parcelRegistrationEnabled;
    
    /**
     * Register a new parcel for a customer
     */
    public CompletableFuture<Map<String, Object>> registerParcel(EDIParcelOrderDTO parcelOrder, 
                                                               Authentication authentication, 
                                                               String authHeader) {
        logger.info("Registering parcel for customer: {}", authentication.getName());
        
        if (!parcelRegistrationEnabled) {
            return CompletableFuture.completedFuture(Map.of(
                "success", false,
                "message", "Parcel registration is currently disabled"
            ));
        }
        
        // Extract token from Bearer header
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        
        // Enrich parcel order with customer information
        enrichParcelOrderWithCustomerInfo(parcelOrder, authentication);
        
        // Send to main service for processing
        return mainServiceClient.processEDIOrder(parcelOrder, token)
                .thenApply(response -> {
                    if ((Boolean) response.getOrDefault("success", false)) {
                        // Send customer-specific event
                        sendCustomerParcelEvent(parcelOrder, authentication, "PARCEL_REGISTERED");
                        
                        logger.info("Parcel {} registered successfully for customer {}", 
                                   response.get("parcelId"), authentication.getName());
                    }
                    return response;
                })
                .exceptionally(ex -> {
                    logger.error("Failed to register parcel for customer: {}", authentication.getName(), ex);
                    return Map.of(
                        "success", false,
                        "message", "Failed to register parcel: " + ex.getMessage(),
                        "error", ex.getClass().getSimpleName()
                    );
                });
    }
    
    /**
     * Get customer's parcels with optional status filter
     */
    public CompletableFuture<Map<String, Object>> getCustomerParcels(Authentication authentication, 
                                                                   String status, 
                                                                   Pageable pageable) {
        logger.debug("Fetching parcels for customer: {} with status: {}", authentication.getName(), status);
        
        // For now, return mock data since we don't have direct database access
        // In a real implementation, this would query a customer-specific database or cache
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Parcels retrieved successfully");
        response.put("parcels", java.util.Collections.emptyList());
        response.put("currentPage", pageable.getPageNumber());
        response.put("totalPages", 0);
        response.put("totalElements", 0L);
        response.put("hasNext", false);
        response.put("hasPrevious", false);
        
        return CompletableFuture.completedFuture(response);
    }
    
    /**
     * Track a specific parcel for a customer
     */
    public CompletableFuture<Map<String, Object>> trackParcel(String parcelId, 
                                                            Authentication authentication, 
                                                            String authHeader) {
        logger.debug("Tracking parcel {} for customer: {}", parcelId, authentication.getName());
        
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        
        // Get parcel details and tracking history from main service
        CompletableFuture<Map<String, Object>> parcelDetailsFuture = 
            mainServiceClient.getParcelDetails(parcelId, token);
        
        CompletableFuture<Map<String, Object>> trackingHistoryFuture = 
            mainServiceClient.getTrackingHistory(parcelId, token);
        
        return CompletableFuture.allOf(parcelDetailsFuture, trackingHistoryFuture)
                .thenApply(v -> {
                    Map<String, Object> parcelDetails = parcelDetailsFuture.join();
                    Map<String, Object> trackingHistory = trackingHistoryFuture.join();
                    
                    Map<String, Object> response = new HashMap<>();
                    
                    if ((Boolean) parcelDetails.getOrDefault("success", false) && 
                        (Boolean) trackingHistory.getOrDefault("success", false)) {
                        
                        response.put("success", true);
                        response.put("message", "Parcel tracking information retrieved");
                        response.put("parcel", parcelDetails.get("parcel"));
                        response.put("trackingHistory", trackingHistory.get("trackingHistory"));
                        response.put("currentStatus", parcelDetails.get("status"));
                        response.put("lastUpdated", parcelDetails.get("updatedAt"));
                        
                    } else {
                        response.put("success", false);
                        response.put("message", "Parcel not found or access denied");
                    }
                    
                    return response;
                })
                .exceptionally(ex -> {
                    logger.error("Failed to track parcel {} for customer: {}", parcelId, authentication.getName(), ex);
                    return Map.of(
                        "success", false,
                        "message", "Failed to track parcel: " + ex.getMessage()
                    );
                });
    }
    
    /**
     * Get parcel details for a customer
     */
    public CompletableFuture<Map<String, Object>> getParcelDetails(String parcelId, 
                                                                 Authentication authentication, 
                                                                 String authHeader) {
        logger.debug("Getting parcel details {} for customer: {}", parcelId, authentication.getName());
        
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        
        return mainServiceClient.getParcelDetails(parcelId, token)
                .thenApply(response -> {
                    if ((Boolean) response.getOrDefault("success", false)) {
                        // Filter sensitive information for customer view
                        Map<String, Object> filteredResponse = new HashMap<>(response);
                        Map<String, Object> parcel = (Map<String, Object>) response.get("parcel");
                        
                        if (parcel != null) {
                            // Create customer-safe parcel information
                            Map<String, Object> customerParcel = createCustomerSafeParcelInfo(parcel);
                            filteredResponse.put("parcel", customerParcel);
                        }
                        
                        return filteredResponse;
                    } else {
                        return Map.of(
                            "success", false,
                            "message", "Parcel not found or access denied"
                        );
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Failed to get parcel details {} for customer: {}", parcelId, authentication.getName(), ex);
                    return Map.of(
                        "success", false,
                        "message", "Failed to get parcel details: " + ex.getMessage()
                    );
                });
    }
    
    /**
     * Cancel a parcel if it's in a cancellable state
     */
    public CompletableFuture<Map<String, Object>> cancelParcel(String parcelId, 
                                                             String reason, 
                                                             Authentication authentication, 
                                                             String authHeader) {
        logger.info("Cancel request for parcel {} from customer: {}", parcelId, authentication.getName());
        
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        
        Map<String, Object> statusUpdate = Map.of(
            "status", "CANCELLED",
            "reason", reason,
            "cancelledBy", authentication.getName(),
            "cancelledAt", LocalDateTime.now()
        );
        
        return mainServiceClient.updateParcelStatus(parcelId, statusUpdate, token)
                .thenApply(response -> {
                    if ((Boolean) response.getOrDefault("success", false)) {
                        // Send cancellation event
                        sendParcelCancellationEvent(parcelId, reason, authentication);
                        
                        logger.info("Parcel {} cancelled successfully by customer {}", parcelId, authentication.getName());
                        
                        return Map.of(
                            "success", true,
                            "message", "Parcel cancelled successfully",
                            "parcelId", parcelId,
                            "status", "CANCELLED"
                        );
                    } else {
                        return Map.of(
                            "success", false,
                            "message", response.getOrDefault("message", "Parcel cannot be cancelled at this stage")
                        );
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Failed to cancel parcel {} for customer: {}", parcelId, authentication.getName(), ex);
                    return Map.of(
                        "success", false,
                        "message", "Failed to cancel parcel: " + ex.getMessage()
                    );
                });
    }
    
    /**
     * Get customer dashboard data
     */
    public CompletableFuture<Map<String, Object>> getCustomerDashboard(Authentication authentication) {
        logger.debug("Generating dashboard for customer: {}", authentication.getName());
        
        // For now, return mock dashboard data
        // In a real implementation, this would aggregate data from various sources
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("success", true);
        dashboard.put("message", "Dashboard data retrieved successfully");
        dashboard.put("totalParcels", 0);
        dashboard.put("activeDeliveries", 0);
        dashboard.put("parcelsThisMonth", 0);
        dashboard.put("parcelsByStatus", Map.of());
        dashboard.put("recentParcels", java.util.Collections.emptyList());
        
        return CompletableFuture.completedFuture(dashboard);
    }
    
    /**
     * Enrich parcel order with customer information
     */
    private void enrichParcelOrderWithCustomerInfo(EDIParcelOrderDTO parcelOrder, Authentication authentication) {
        // Set sender information from customer profile
        if (parcelOrder.getSender() == null) {
            parcelOrder.setSender(new EDIParcelOrderDTO.CustomerDTO());
        }
        
        EDIParcelOrderDTO.CustomerDTO sender = parcelOrder.getSender();
        if (sender.getName() == null || sender.getName().trim().isEmpty()) {
            sender.setName(authentication.getName());
        }
        
        // Generate EDI reference if not provided
        if (parcelOrder.getEdiReference() == null || parcelOrder.getEdiReference().trim().isEmpty()) {
            parcelOrder.setEdiReference("CUST-" + authentication.getName() + "-" + System.currentTimeMillis());
        }
    }
    
    /**
     * Send customer-specific parcel event
     */
    private void sendCustomerParcelEvent(EDIParcelOrderDTO parcelOrder, Authentication authentication, String eventType) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("ediReference", parcelOrder.getEdiReference());
            event.put("customerName", authentication.getName());
            event.put("timestamp", LocalDateTime.now());
            event.put("source", "customer-interface-service");
            
            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(event)
                    .setHeader("customerId", authentication.getName())
                    .setHeader("eventType", eventType)
                    .build();
            
            streamBridge.send("customerParcels-out-0", message);
            
            logger.debug("Sent customer parcel event: {} for EDI: {}", eventType, parcelOrder.getEdiReference());
            
        } catch (Exception e) {
            logger.error("Failed to send customer parcel event for EDI: {}", parcelOrder.getEdiReference(), e);
        }
    }
    
    /**
     * Send parcel cancellation event
     */
    private void sendParcelCancellationEvent(String parcelId, String reason, Authentication authentication) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "PARCEL_CANCELLED");
            event.put("parcelId", parcelId);
            event.put("reason", reason);
            event.put("cancelledBy", authentication.getName());
            event.put("timestamp", LocalDateTime.now());
            event.put("source", "customer-interface-service");
            
            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(event)
                    .setHeader("parcelId", parcelId)
                    .setHeader("eventType", "PARCEL_CANCELLED")
                    .build();
            
            streamBridge.send("customerNotifications-out-0", message);
            
            logger.debug("Sent parcel cancellation event for parcel: {}", parcelId);
            
        } catch (Exception e) {
            logger.error("Failed to send parcel cancellation event for parcel: {}", parcelId, e);
        }
    }
    
    /**
     * Create customer-safe parcel information (filter sensitive data)
     */
    private Map<String, Object> createCustomerSafeParcelInfo(Map<String, Object> parcel) {
        Map<String, Object> customerParcel = new HashMap<>();
        
        // Include only customer-relevant information
        customerParcel.put("parcelId", parcel.get("parcelId"));
        customerParcel.put("ediReference", parcel.get("ediReference"));
        customerParcel.put("status", parcel.get("status"));
        customerParcel.put("priority", parcel.get("priority"));
        customerParcel.put("description", parcel.get("description"));
        customerParcel.put("weight", parcel.get("weight"));
        customerParcel.put("dimensions", parcel.get("dimensions"));
        customerParcel.put("createdAt", parcel.get("createdAt"));
        customerParcel.put("updatedAt", parcel.get("updatedAt"));
        customerParcel.put("estimatedDeliveryDate", parcel.get("estimatedDeliveryDate"));
        customerParcel.put("actualDeliveryDate", parcel.get("actualDeliveryDate"));
        
        // Include addresses
        customerParcel.put("pickupAddress", parcel.get("pickupAddress"));
        customerParcel.put("deliveryAddress", parcel.get("deliveryAddress"));
        
        // Include recipient info (limited for privacy)
        Map<String, Object> recipient = (Map<String, Object>) parcel.get("recipient");
        if (recipient != null) {
            Map<String, Object> limitedRecipient = new HashMap<>();
            limitedRecipient.put("name", recipient.get("name"));
            limitedRecipient.put("phone", recipient.get("phone"));
            customerParcel.put("recipient", limitedRecipient);
        }
        
        return customerParcel;
    }
}
