package com.courier.controller;

import com.courier.client.MainServiceClient;
import com.courier.dto.EDIParcelOrderDTO;
import com.courier.service.CustomerParcelService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Customer-facing controller for parcel registration and tracking
 * Part of the separate Customer Interface Microservice
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
    private MainServiceClient mainServiceClient;
    
    /**
     * Register a new parcel for delivery
     * POST /api/v1/customer/parcels/register
     */
    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> registerParcel(
            @Valid @RequestBody EDIParcelOrderDTO parcelOrder,
            Authentication authentication,
            @RequestHeader("Authorization") String authHeader) {
        
        logger.info("Parcel registration request from customer: {}", authentication.getName());
        
        return customerParcelService.registerParcel(parcelOrder, authentication, authHeader)
                .thenApply(response -> {
                    if ((Boolean) response.get("success")) {
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Error registering parcel for customer: {}", authentication.getName(), ex);
                    Map<String, Object> errorResponse = Map.of(
                        "success", false,
                        "message", "Failed to register parcel: " + ex.getMessage(),
                        "error", ex.getClass().getSimpleName()
                    );
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }
    
    /**
     * Get customer's parcels with pagination
     * GET /api/v1/customer/parcels/my
     */
    @GetMapping("/my")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getMyParcels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        
        logger.info("Fetching parcels for customer: {}", authentication.getName());
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return customerParcelService.getCustomerParcels(authentication, status, pageable)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Error fetching parcels for customer: {}", authentication.getName(), ex);
                    Map<String, Object> errorResponse = Map.of(
                        "success", false,
                        "message", "Failed to fetch parcels: " + ex.getMessage()
                    );
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }
    
    /**
     * Track a specific parcel
     * GET /api/v1/customer/parcels/{parcelId}/track
     */
    @GetMapping("/{parcelId}/track")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> trackParcel(
            @PathVariable String parcelId,
            Authentication authentication,
            @RequestHeader("Authorization") String authHeader) {
        
        logger.info("Tracking request for parcel {} from customer: {}", parcelId, authentication.getName());
        
        return customerParcelService.trackParcel(parcelId, authentication, authHeader)
                .thenApply(response -> {
                    if ((Boolean) response.get("success")) {
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Error tracking parcel {} for customer: {}", parcelId, authentication.getName(), ex);
                    Map<String, Object> errorResponse = Map.of(
                        "success", false,
                        "message", "Failed to track parcel: " + ex.getMessage()
                    );
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }
    
    /**
     * Get parcel details
     * GET /api/v1/customer/parcels/{parcelId}
     */
    @GetMapping("/{parcelId}")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getParcelDetails(
            @PathVariable String parcelId,
            Authentication authentication,
            @RequestHeader("Authorization") String authHeader) {
        
        logger.info("Parcel details request for {} from customer: {}", parcelId, authentication.getName());
        
        return customerParcelService.getParcelDetails(parcelId, authentication, authHeader)
                .thenApply(response -> {
                    if ((Boolean) response.get("success")) {
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Error getting parcel details for {} from customer: {}", 
                                parcelId, authentication.getName(), ex);
                    Map<String, Object> errorResponse = Map.of(
                        "success", false,
                        "message", "Failed to get parcel details: " + ex.getMessage()
                    );
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }
    
    /**
     * Cancel a parcel (if allowed)
     * PUT /api/v1/customer/parcels/{parcelId}/cancel
     */
    @PutMapping("/{parcelId}/cancel")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> cancelParcel(
            @PathVariable String parcelId,
            @RequestBody(required = false) Map<String, String> cancelRequest,
            Authentication authentication,
            @RequestHeader("Authorization") String authHeader) {
        
        logger.info("Cancel request for parcel {} from customer: {}", parcelId, authentication.getName());
        
        String reason = cancelRequest != null ? cancelRequest.get("reason") : "Customer requested cancellation";
        
        return customerParcelService.cancelParcel(parcelId, reason, authentication, authHeader)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Error cancelling parcel {} for customer: {}", parcelId, authentication.getName(), ex);
                    Map<String, Object> errorResponse = Map.of(
                        "success", false,
                        "message", "Failed to cancel parcel: " + ex.getMessage()
                    );
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
                });
    }
    
    /**
     * Get customer dashboard summary
     * GET /api/v1/customer/parcels/dashboard
     */
    @GetMapping("/dashboard")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getDashboard(Authentication authentication) {
        logger.info("Dashboard request from customer: {}", authentication.getName());
        
        return customerParcelService.getCustomerDashboard(authentication)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Error getting dashboard for customer: {}", authentication.getName(), ex);
                    Map<String, Object> errorResponse = Map.of(
                        "success", false,
                        "message", "Failed to get dashboard data: " + ex.getMessage()
                    );
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                });
    }
    
    /**
     * Health check endpoint specific to customer service
     * GET /api/v1/customer/parcels/health
     */
    @GetMapping("/health")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "customer-interface-service");
        health.put("timestamp", System.currentTimeMillis());
        health.put("mainServiceAvailable", mainServiceClient.isMainServiceAvailable());
        
        return ResponseEntity.ok(health);
    }
}
