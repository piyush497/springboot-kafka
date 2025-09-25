package com.courier.client;

import com.courier.dto.EDIParcelOrderDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class MainServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(MainServiceClient.class);
    
    private final WebClient webClient;
    
    public MainServiceClient(@Value("${courier.main-service.base-url}") String baseUrl,
                           @Value("${courier.main-service.timeout}") Duration timeout) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    @CircuitBreaker(name = "main-service", fallbackMethod = "processEDIFallback")
    @Retry(name = "main-service")
    @TimeLimiter(name = "main-service")
    public CompletableFuture<Map<String, Object>> processEDIOrder(EDIParcelOrderDTO ediOrder, String authToken) {
        logger.info("Sending EDI order to main service: {}", ediOrder.getEdiReference());
        
        return webClient.post()
                .uri("/api/v1/edi/process")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                .bodyValue(ediOrder)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(response -> logger.info("EDI order processed successfully: {}", ediOrder.getEdiReference()))
                .doOnError(error -> logger.error("Failed to process EDI order: {}", ediOrder.getEdiReference(), error))
                .toFuture();
    }
    
    @CircuitBreaker(name = "main-service", fallbackMethod = "validateUserFallback")
    @Retry(name = "main-service")
    @TimeLimiter(name = "main-service")
    public CompletableFuture<Map<String, Object>> validateUser(String authToken) {
        logger.debug("Validating user with main service");
        
        return webClient.get()
                .uri("/api/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(response -> logger.debug("User validation successful"))
                .doOnError(error -> logger.error("User validation failed", error))
                .toFuture();
    }
    
    @CircuitBreaker(name = "main-service", fallbackMethod = "getParcelDetailsFallback")
    @Retry(name = "main-service")
    @TimeLimiter(name = "main-service")
    public CompletableFuture<Map<String, Object>> getParcelDetails(String parcelId, String authToken) {
        logger.debug("Fetching parcel details from main service: {}", parcelId);
        
        return webClient.get()
                .uri("/api/v1/parcels/{parcelId}", parcelId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(response -> logger.debug("Parcel details retrieved: {}", parcelId))
                .doOnError(error -> logger.error("Failed to get parcel details: {}", parcelId, error))
                .toFuture();
    }
    
    @CircuitBreaker(name = "main-service", fallbackMethod = "getTrackingHistoryFallback")
    @Retry(name = "main-service")
    @TimeLimiter(name = "main-service")
    public CompletableFuture<Map<String, Object>> getTrackingHistory(String parcelId, String authToken) {
        logger.debug("Fetching tracking history from main service: {}", parcelId);
        
        return webClient.get()
                .uri("/api/v1/parcels/{parcelId}/tracking", parcelId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(response -> logger.debug("Tracking history retrieved: {}", parcelId))
                .doOnError(error -> logger.error("Failed to get tracking history: {}", parcelId, error))
                .toFuture();
    }
    
    @CircuitBreaker(name = "main-service", fallbackMethod = "updateParcelStatusFallback")
    @Retry(name = "main-service")
    @TimeLimiter(name = "main-service")
    public CompletableFuture<Map<String, Object>> updateParcelStatus(String parcelId, Map<String, Object> statusUpdate, String authToken) {
        logger.info("Updating parcel status via main service: {}", parcelId);
        
        return webClient.put()
                .uri("/api/v1/parcels/{parcelId}/status", parcelId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authToken)
                .bodyValue(statusUpdate)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(response -> logger.info("Parcel status updated: {}", parcelId))
                .doOnError(error -> logger.error("Failed to update parcel status: {}", parcelId, error))
                .toFuture();
    }
    
    // Fallback methods
    public CompletableFuture<Map<String, Object>> processEDIFallback(EDIParcelOrderDTO ediOrder, String authToken, Exception ex) {
        logger.error("EDI processing fallback triggered for: {}", ediOrder.getEdiReference(), ex);
        
        Map<String, Object> fallbackResponse = Map.of(
            "success", false,
            "message", "Main service is currently unavailable. Please try again later.",
            "error", "SERVICE_UNAVAILABLE",
            "fallback", true
        );
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }
    
    public CompletableFuture<Map<String, Object>> validateUserFallback(String authToken, Exception ex) {
        logger.error("User validation fallback triggered", ex);
        
        Map<String, Object> fallbackResponse = Map.of(
            "success", false,
            "message", "Authentication service is currently unavailable",
            "error", "AUTH_SERVICE_UNAVAILABLE",
            "fallback", true
        );
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }
    
    public CompletableFuture<Map<String, Object>> getParcelDetailsFallback(String parcelId, String authToken, Exception ex) {
        logger.error("Parcel details fallback triggered for: {}", parcelId, ex);
        
        Map<String, Object> fallbackResponse = Map.of(
            "success", false,
            "message", "Parcel details service is currently unavailable",
            "error", "PARCEL_SERVICE_UNAVAILABLE",
            "fallback", true
        );
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }
    
    public CompletableFuture<Map<String, Object>> getTrackingHistoryFallback(String parcelId, String authToken, Exception ex) {
        logger.error("Tracking history fallback triggered for: {}", parcelId, ex);
        
        Map<String, Object> fallbackResponse = Map.of(
            "success", false,
            "message", "Tracking service is currently unavailable",
            "error", "TRACKING_SERVICE_UNAVAILABLE",
            "fallback", true,
            "trackingHistory", java.util.Collections.emptyList()
        );
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }
    
    public CompletableFuture<Map<String, Object>> updateParcelStatusFallback(String parcelId, Map<String, Object> statusUpdate, String authToken, Exception ex) {
        logger.error("Parcel status update fallback triggered for: {}", parcelId, ex);
        
        Map<String, Object> fallbackResponse = Map.of(
            "success", false,
            "message", "Status update service is currently unavailable",
            "error", "STATUS_UPDATE_SERVICE_UNAVAILABLE",
            "fallback", true
        );
        
        return CompletableFuture.completedFuture(fallbackResponse);
    }
    
    public boolean isMainServiceAvailable() {
        try {
            return webClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(response -> "UP".equals(((Map<String, Object>) response).get("status")))
                    .block(Duration.ofSeconds(5));
        } catch (Exception e) {
            logger.warn("Main service health check failed", e);
            return false;
        }
    }
}
