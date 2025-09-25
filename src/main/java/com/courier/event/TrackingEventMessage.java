package com.courier.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Event for tracking updates
 */
public class TrackingEventMessage extends ParcelEvent {
    
    @NotBlank
    @JsonProperty("trackingEventType")
    private String trackingEventType;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("eventTimestamp")
    private LocalDateTime eventTimestamp;
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("vehicleId")
    private String vehicleId;
    
    @JsonProperty("driverName")
    private String driverName;
    
    @JsonProperty("additionalInfo")
    private String additionalInfo;
    
    @JsonProperty("previousStatus")
    private String previousStatus;
    
    @JsonProperty("currentStatus")
    private String currentStatus;
    
    // Constructors
    public TrackingEventMessage() {
        super();
    }
    
    public TrackingEventMessage(String parcelId, String trackingEventType) {
        super("TRACKING_EVENT", parcelId);
        this.trackingEventType = trackingEventType;
        this.eventTimestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getTrackingEventType() {
        return trackingEventType;
    }
    
    public void setTrackingEventType(String trackingEventType) {
        this.trackingEventType = trackingEventType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }
    
    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getVehicleId() {
        return vehicleId;
    }
    
    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }
    
    public String getDriverName() {
        return driverName;
    }
    
    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }
    
    public String getAdditionalInfo() {
        return additionalInfo;
    }
    
    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
    
    public String getPreviousStatus() {
        return previousStatus;
    }
    
    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }
    
    public String getCurrentStatus() {
        return currentStatus;
    }
    
    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }
}
