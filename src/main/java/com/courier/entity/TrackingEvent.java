package com.courier.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "tracking_events")
public class TrackingEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Parcel is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private Parcel parcel;
    
    @NotNull(message = "Event type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;
    
    @NotBlank(message = "Event description is required")
    @Size(max = 500)
    @Column(name = "description", nullable = false)
    private String description;
    
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;
    
    @Size(max = 100)
    @Column(name = "location")
    private String location;
    
    @Size(max = 50)
    @Column(name = "vehicle_id")
    private String vehicleId;
    
    @Size(max = 100)
    @Column(name = "driver_name")
    private String driverName;
    
    @Size(max = 1000)
    @Column(name = "additional_info")
    private String additionalInfo;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (eventTimestamp == null) {
            eventTimestamp = LocalDateTime.now();
        }
    }
    
    // Constructors
    public TrackingEvent() {}
    
    public TrackingEvent(Parcel parcel, EventType eventType, String description, String location) {
        this.parcel = parcel;
        this.eventType = eventType;
        this.description = description;
        this.location = location;
        this.eventTimestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Parcel getParcel() {
        return parcel;
    }
    
    public void setParcel(Parcel parcel) {
        this.parcel = parcel;
    }
    
    public EventType getEventType() {
        return eventType;
    }
    
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public enum EventType {
        REGISTERED,
        PICKUP_SCHEDULED,
        PICKED_UP,
        ARRIVED_AT_FACILITY,
        DEPARTED_FROM_FACILITY,
        LOADED_IN_TRUCK,
        IN_TRANSIT,
        OUT_FOR_DELIVERY,
        DELIVERY_ATTEMPTED,
        DELIVERED,
        FAILED_DELIVERY,
        RETURNED_TO_FACILITY,
        CANCELLED,
        EXCEPTION
    }
}
