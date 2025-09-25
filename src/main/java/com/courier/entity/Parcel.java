package com.courier.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "parcels")
public class Parcel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Parcel ID is required")
    @Size(max = 50)
    @Column(name = "parcel_id", unique = true, nullable = false)
    private String parcelId;
    
    @NotNull(message = "Sender is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Customer sender;
    
    @NotNull(message = "Recipient is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Customer recipient;
    
    @NotNull(message = "Pickup address is required")
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "pickup_address_id", nullable = false)
    private Address pickupAddress;
    
    @NotNull(message = "Delivery address is required")
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_address_id", nullable = false)
    private Address deliveryAddress;
    
    @Size(max = 200)
    @Column(name = "description")
    private String description;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Weight must be greater than 0")
    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;
    
    @Size(max = 20)
    @Column(name = "dimensions")
    private String dimensions;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ParcelStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private Priority priority;
    
    @Column(name = "estimated_delivery_date")
    private LocalDateTime estimatedDeliveryDate;
    
    @Column(name = "actual_delivery_date")
    private LocalDateTime actualDeliveryDate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Size(max = 50)
    @Column(name = "edi_reference")
    private String ediReference;
    
    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;
    
    @OneToMany(mappedBy = "parcel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TrackingEvent> trackingEvents;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ParcelStatus.REGISTERED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public Parcel() {}
    
    public Parcel(String parcelId, Customer sender, Customer recipient, 
                  Address pickupAddress, Address deliveryAddress) {
        this.parcelId = parcelId;
        this.sender = sender;
        this.recipient = recipient;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.status = ParcelStatus.REGISTERED;
        this.priority = Priority.STANDARD;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getParcelId() {
        return parcelId;
    }
    
    public void setParcelId(String parcelId) {
        this.parcelId = parcelId;
    }
    
    public Customer getSender() {
        return sender;
    }
    
    public void setSender(Customer sender) {
        this.sender = sender;
    }
    
    public Customer getRecipient() {
        return recipient;
    }
    
    public void setRecipient(Customer recipient) {
        this.recipient = recipient;
    }
    
    public Address getPickupAddress() {
        return pickupAddress;
    }
    
    public void setPickupAddress(Address pickupAddress) {
        this.pickupAddress = pickupAddress;
    }
    
    public Address getDeliveryAddress() {
        return deliveryAddress;
    }
    
    public void setDeliveryAddress(Address deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getWeight() {
        return weight;
    }
    
    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }
    
    public String getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }
    
    public ParcelStatus getStatus() {
        return status;
    }
    
    public void setStatus(ParcelStatus status) {
        this.status = status;
    }
    
    public Priority getPriority() {
        return priority;
    }
    
    public void setPriority(Priority priority) {
        this.priority = priority;
    }
    
    public LocalDateTime getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }
    
    public void setEstimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }
    
    public LocalDateTime getActualDeliveryDate() {
        return actualDeliveryDate;
    }
    
    public void setActualDeliveryDate(LocalDateTime actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getEdiReference() {
        return ediReference;
    }
    
    public void setEdiReference(String ediReference) {
        this.ediReference = ediReference;
    }
    
    public BigDecimal getCost() {
        return cost;
    }
    
    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }
    
    public List<TrackingEvent> getTrackingEvents() {
        return trackingEvents;
    }
    
    public void setTrackingEvents(List<TrackingEvent> trackingEvents) {
        this.trackingEvents = trackingEvents;
    }
    
    public enum ParcelStatus {
        REGISTERED,
        PICKED_UP,
        IN_TRANSIT,
        LOADED_IN_TRUCK,
        OUT_FOR_DELIVERY,
        DELIVERED,
        FAILED_DELIVERY,
        RETURNED,
        CANCELLED
    }
    
    public enum Priority {
        STANDARD,
        EXPRESS,
        URGENT
    }
}
