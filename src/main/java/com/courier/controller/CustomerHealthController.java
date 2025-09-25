package com.courier.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for customer interface microservice
 * Provides detailed health information for AKS monitoring
 */
@RestController
@RequestMapping("/health")
public class CustomerHealthController implements HealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerHealthController.class);
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private StreamBridge streamBridge;
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    @Value("${customer-interface.features.parcel-registration:true}")
    private boolean parcelRegistrationEnabled;
    
    /**
     * Basic health check endpoint
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Check database connectivity
            boolean dbHealthy = checkDatabaseHealth();
            
            // Check Kafka connectivity
            boolean kafkaHealthy = checkKafkaHealth();
            
            // Check application features
            boolean featuresHealthy = checkFeaturesHealth();
            
            boolean overallHealthy = dbHealthy && kafkaHealthy && featuresHealthy;
            
            health.put("status", overallHealthy ? "UP" : "DOWN");
            health.put("application", applicationName);
            health.put("timestamp", System.currentTimeMillis());
            health.put("checks", Map.of(
                "database", dbHealthy ? "UP" : "DOWN",
                "kafka", kafkaHealthy ? "UP" : "DOWN",
                "features", featuresHealthy ? "UP" : "DOWN"
            ));
            
            if (overallHealthy) {
                return ResponseEntity.ok(health);
            } else {
                return ResponseEntity.status(503).body(health);
            }
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }
    
    /**
     * Detailed health check for monitoring systems
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            health.put("application", applicationName);
            health.put("timestamp", System.currentTimeMillis());
            health.put("version", "1.0.0");
            health.put("profile", "customer-interface");
            
            // Database health details
            Map<String, Object> dbHealth = getDetailedDatabaseHealth();
            health.put("database", dbHealth);
            
            // Kafka health details
            Map<String, Object> kafkaHealth = getDetailedKafkaHealth();
            health.put("kafka", kafkaHealth);
            
            // Feature health details
            Map<String, Object> featureHealth = getDetailedFeatureHealth();
            health.put("features", featureHealth);
            
            // System metrics
            Map<String, Object> systemMetrics = getSystemMetrics();
            health.put("system", systemMetrics);
            
            // Determine overall status
            boolean overallHealthy = 
                "UP".equals(dbHealth.get("status")) &&
                "UP".equals(kafkaHealth.get("status")) &&
                "UP".equals(featureHealth.get("status"));
            
            health.put("status", overallHealthy ? "UP" : "DOWN");
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Detailed health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }
    
    /**
     * Readiness probe endpoint for Kubernetes
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> readiness() {
        Map<String, String> response = new HashMap<>();
        
        try {
            boolean ready = checkDatabaseHealth() && checkKafkaHealth();
            
            if (ready) {
                response.put("status", "READY");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "NOT_READY");
                return ResponseEntity.status(503).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Readiness check failed", e);
            response.put("status", "NOT_READY");
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }
    
    /**
     * Liveness probe endpoint for Kubernetes
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> liveness() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ALIVE");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }
    
    @Override
    public Health health() {
        try {
            boolean dbHealthy = checkDatabaseHealth();
            boolean kafkaHealthy = checkKafkaHealth();
            boolean featuresHealthy = checkFeaturesHealth();
            
            if (dbHealthy && kafkaHealthy && featuresHealthy) {
                return Health.up()
                    .withDetail("database", "UP")
                    .withDetail("kafka", "UP")
                    .withDetail("features", "UP")
                    .build();
            } else {
                return Health.down()
                    .withDetail("database", dbHealthy ? "UP" : "DOWN")
                    .withDetail("kafka", kafkaHealthy ? "UP" : "DOWN")
                    .withDetail("features", featuresHealthy ? "UP" : "DOWN")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
    
    private boolean checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (Exception e) {
            logger.warn("Database health check failed", e);
            return false;
        }
    }
    
    private boolean checkKafkaHealth() {
        try {
            // Simple check - if KafkaTemplate is available, assume Kafka is healthy
            // In a real implementation, you might want to send a test message
            return kafkaTemplate != null;
        } catch (Exception e) {
            logger.warn("Kafka health check failed", e);
            return false;
        }
    }
    
    private boolean checkFeaturesHealth() {
        try {
            // Check if critical features are enabled
            return parcelRegistrationEnabled;
        } catch (Exception e) {
            logger.warn("Features health check failed", e);
            return false;
        }
    }
    
    private Map<String, Object> getDetailedDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);
            dbHealth.put("status", isValid ? "UP" : "DOWN");
            dbHealth.put("database", connection.getMetaData().getDatabaseProductName());
            dbHealth.put("version", connection.getMetaData().getDatabaseProductVersion());
            dbHealth.put("url", connection.getMetaData().getURL());
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        
        return dbHealth;
    }
    
    private Map<String, Object> getDetailedKafkaHealth() {
        Map<String, Object> kafkaHealth = new HashMap<>();
        
        try {
            kafkaHealth.put("status", kafkaTemplate != null ? "UP" : "DOWN");
            kafkaHealth.put("streamBridge", streamBridge != null ? "UP" : "DOWN");
        } catch (Exception e) {
            kafkaHealth.put("status", "DOWN");
            kafkaHealth.put("error", e.getMessage());
        }
        
        return kafkaHealth;
    }
    
    private Map<String, Object> getDetailedFeatureHealth() {
        Map<String, Object> featureHealth = new HashMap<>();
        
        featureHealth.put("status", parcelRegistrationEnabled ? "UP" : "DOWN");
        featureHealth.put("parcelRegistration", parcelRegistrationEnabled);
        
        return featureHealth;
    }
    
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        metrics.put("memory", Map.of(
            "max", maxMemory,
            "total", totalMemory,
            "used", usedMemory,
            "free", freeMemory,
            "usagePercent", (double) usedMemory / totalMemory * 100
        ));
        
        metrics.put("processors", runtime.availableProcessors());
        metrics.put("uptime", System.currentTimeMillis());
        
        return metrics;
    }
}
