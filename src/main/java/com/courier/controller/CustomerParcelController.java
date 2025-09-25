package com.courier.controller;

import com.courier.dto.EDIParcelOrderDTO;
import com.courier.entity.Parcel;
import com.courier.entity.TrackingEvent;
import com.courier.entity.User;
import com.courier.service.CustomerParcelService;
import com.courier.service.ParcelService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Customer-facing controller for parcel registration and tracking
 * Deployed as part of the customer interface microservice
 */
@RestController
@RequestMapping("/parcels")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerParcelController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerParcelController.class);
    
    @Autowired
    private CustomerParcelService customerParcelService;
    
    @Autowired
    private ParcelService parcelService;
    
    /**
     * Register a new parcel for delivery
     * POST /api/v1/customer/parcels/register
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerParcel(
            @Valid @RequestBody EDIParcelOrderDTO parcelOrder,
            Authentication authentication) {
        
        logger.info("Parcel registration request from customer: {}", authentication.getName());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User customer = (User) authentication.getPrincipal();
            
            // Validate customer permissions and limits
            customerParcelService.validateCustomerLimits(customer.getId());
            
            // Process parcel registration
            Parcel registeredParcel = customerParcelService.registerParcel(parcelOrder, customer);
            
            response.put("success", true);
            response.put("message", "Parcel registered successfully");
            response.put("parcelId", registeredParcel.getParcelId());
            response.put("ediReference", registeredParcel.getEdiReference());
            response.put("status", registeredParcel.getStatus().toString());
            response.put("estimatedDeliveryDate", registeredParcel.getEstimatedDeliveryDate());
            response.put("createdAt", registeredParcel.getCreatedAt());
            
            logger.info("Parcel {} registered successfully for customer {}", 
                       registeredParcel.getParcelId(), customer.getUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error registering parcel for customer: {}", authentication.getName(), e);
            
            response.put("success", false);
            response.put("message", "Failed to register parcel: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Get customer's parcels with pagination
     * GET /api/v1/customer/parcels/my
     */
    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyParcels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        
        logger.info("Fetching parcels for customer: {}", authentication.getName());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User customer = (User) authentication.getPrincipal();
            
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                       Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Parcel> parcelsPage = customerParcelService.getCustomerParcels(
                customer.getId(), status, pageable);
            
            response.put("success", true);
            response.put("message", "Parcels retrieved successfully");
            response.put("parcels", parcelsPage.getContent());
            response.put("currentPage", parcelsPage.getNumber());
            response.put("totalPages", parcelsPage.getTotalPages());
            response.put("totalElements", parcelsPage.getTotalElements());
            response.put("hasNext", parcelsPage.hasNext());
            response.put("hasPrevious", parcelsPage.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching parcels for customer: {}", authentication.getName(), e);
            
            response.put("success", false);
            response.put("message", "Failed to fetch parcels: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Track a specific parcel
     * GET /api/v1/customer/parcels/{parcelId}/track
     */
    @GetMapping("/{parcelId}/track")
    public ResponseEntity<Map<String, Object>> trackParcel(
            @PathVariable String parcelId,
            Authentication authentication) {
        
        logger.info("Tracking request for parcel {} from customer: {}", parcelId, authentication.getName());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User customer = (User) authentication.getPrincipal();
            
            // Verify customer owns this parcel
            Optional<Parcel> parcelOpt = customerParcelService.getCustomerParcel(customer.getId(), parcelId);
            
            if (parcelOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Parcel not found or access denied");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Parcel parcel = parcelOpt.get();
            List<TrackingEvent> trackingHistory = parcelService.getTrackingHistory(parcelId);
            
            response.put("success", true);
            response.put("message", "Parcel tracking information retrieved");
            response.put("parcel", createParcelSummary(parcel));
            response.put("trackingHistory", trackingHistory);
            response.put("currentStatus", parcel.getStatus().toString());
            response.put("lastUpdated", parcel.getUpdatedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error tracking parcel {} for customer: {}", parcelId, authentication.getName(), e);
            
            response.put("success", false);
            response.put("message", "Failed to track parcel: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get parcel details
     * GET /api/v1/customer/parcels/{parcelId}
     */
    @GetMapping("/{parcelId}")
    public ResponseEntity<Map<String, Object>> getParcelDetails(
            @PathVariable String parcelId,
            Authentication authentication) {
        
        logger.info("Parcel details request for {} from customer: {}", parcelId, authentication.getName());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User customer = (User) authentication.getPrincipal();
            
            Optional<Parcel> parcelOpt = customerParcelService.getCustomerParcel(customer.getId(), parcelId);
            
            if (parcelOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Parcel not found or access denied");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            Parcel parcel = parcelOpt.get();
            
            response.put("success", true);
            response.put("message", "Parcel details retrieved successfully");
            response.put("parcel", createDetailedParcelInfo(parcel));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting parcel details for {} from customer: {}", 
                        parcelId, authentication.getName(), e);
            
            response.put("success", false);
            response.put("message", "Failed to get parcel details: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Cancel a parcel (if allowed)
     * PUT /api/v1/customer/parcels/{parcelId}/cancel
     */
    @PutMapping("/{parcelId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelParcel(
            @PathVariable String parcelId,
            @RequestBody(required = false) Map<String, String> cancelRequest,
            Authentication authentication) {
        
        logger.info("Cancel request for parcel {} from customer: {}", parcelId, authentication.getName());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User customer = (User) authentication.getPrincipal();
            String reason = cancelRequest != null ? cancelRequest.get("reason") : "Customer requested cancellation";
            
            boolean cancelled = customerParcelService.cancelParcel(customer.getId(), parcelId, reason);
            
            if (cancelled) {
                response.put("success", true);
                response.put("message", "Parcel cancelled successfully");
                response.put("parcelId", parcelId);
                response.put("status", "CANCELLED");
            } else {
                response.put("success", false);
                response.put("message", "Parcel cannot be cancelled at this stage");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error cancelling parcel {} for customer: {}", parcelId, authentication.getName(), e);
            
            response.put("success", false);
            response.put("message", "Failed to cancel parcel: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * Get customer dashboard summary
     * GET /api/v1/customer/parcels/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(Authentication authentication) {
        logger.info("Dashboard request from customer: {}", authentication.getName());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User customer = (User) authentication.getPrincipal();
            
            Map<String, Object> dashboard = customerParcelService.getCustomerDashboard(customer.getId());
            
            response.put("success", true);
            response.put("message", "Dashboard data retrieved successfully");
            response.putAll(dashboard);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting dashboard for customer: {}", authentication.getName(), e);
            
            response.put("success", false);
            response.put("message", "Failed to get dashboard data: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    private Map<String, Object> createParcelSummary(Parcel parcel) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("parcelId", parcel.getParcelId());
        summary.put("ediReference", parcel.getEdiReference());
        summary.put("status", parcel.getStatus().toString());
        summary.put("priority", parcel.getPriority().toString());
        summary.put("description", parcel.getDescription());
        summary.put("createdAt", parcel.getCreatedAt());
        summary.put("updatedAt", parcel.getUpdatedAt());
        summary.put("estimatedDeliveryDate", parcel.getEstimatedDeliveryDate());
        summary.put("actualDeliveryDate", parcel.getActualDeliveryDate());
        
        // Delivery address summary
        Map<String, String> deliveryAddress = new HashMap<>();
        deliveryAddress.put("city", parcel.getDeliveryAddress().getCity());
        deliveryAddress.put("state", parcel.getDeliveryAddress().getState());
        deliveryAddress.put("country", parcel.getDeliveryAddress().getCountry());
        summary.put("deliveryAddress", deliveryAddress);
        
        return summary;
    }
    
    private Map<String, Object> createDetailedParcelInfo(Parcel parcel) {
        Map<String, Object> details = new HashMap<>();
        details.put("parcelId", parcel.getParcelId());
        details.put("ediReference", parcel.getEdiReference());
        details.put("status", parcel.getStatus().toString());
        details.put("priority", parcel.getPriority().toString());
        details.put("description", parcel.getDescription());
        details.put("weight", parcel.getWeight());
        details.put("dimensions", parcel.getDimensions());
        details.put("cost", parcel.getCost());
        details.put("createdAt", parcel.getCreatedAt());
        details.put("updatedAt", parcel.getUpdatedAt());
        details.put("estimatedDeliveryDate", parcel.getEstimatedDeliveryDate());
        details.put("actualDeliveryDate", parcel.getActualDeliveryDate());
        
        // Recipient info (limited for privacy)
        Map<String, String> recipient = new HashMap<>();
        recipient.put("name", parcel.getRecipient().getName());
        recipient.put("phone", parcel.getRecipient().getPhone());
        details.put("recipient", recipient);
        
        // Addresses
        details.put("pickupAddress", parcel.getPickupAddress());
        details.put("deliveryAddress", parcel.getDeliveryAddress());
        
        return details;
    }
}
