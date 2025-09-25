package com.courier.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Base event class for all parcel-related events
 */
public class ParcelEvent {
    
    @NotBlank
    @JsonProperty("eventId")
    private String eventId;
    
    @NotBlank
    @JsonProperty("eventType")
    private String eventType;
    
    @NotBlank
    @JsonProperty("parcelId")
    private String parcelId;
    
    @NotNull
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("version")
    private String version = "1.0";
    
    @JsonProperty("correlationId")
    private String correlationId;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // Constructors
    public ParcelEvent() {
        this.timestamp = LocalDateTime.now();
        this.source = "courier-management-system";
    }
    
    public ParcelEvent(String eventType, String parcelId) {
        this();
        this.eventType = eventType;
        this.parcelId = parcelId;
        this.eventId = generateEventId();
    }
    
    // Getters and Setters
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getParcelId() {
        return parcelId;
    }
    
    public void setParcelId(String parcelId) {
        this.parcelId = parcelId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    private String generateEventId() {
        return java.util.UUID.randomUUID().toString();
    }
    
    @Override
    public String toString() {
        return "ParcelEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", parcelId='" + parcelId + '\'' +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                '}';
    }
}
