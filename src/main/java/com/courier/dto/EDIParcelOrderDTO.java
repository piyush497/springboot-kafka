package com.courier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EDIParcelOrderDTO {
    
    @JsonProperty("edi_reference")
    @NotBlank(message = "EDI reference is required")
    @Size(max = 50)
    private String ediReference;
    
    @JsonProperty("parcel_id")
    @Size(max = 50)
    private String parcelId;
    
    @JsonProperty("sender")
    @NotNull(message = "Sender information is required")
    @Valid
    private CustomerDTO sender;
    
    @JsonProperty("recipient")
    @NotNull(message = "Recipient information is required")
    @Valid
    private CustomerDTO recipient;
    
    @JsonProperty("pickup_address")
    @NotNull(message = "Pickup address is required")
    @Valid
    private AddressDTO pickupAddress;
    
    @JsonProperty("delivery_address")
    @NotNull(message = "Delivery address is required")
    @Valid
    private AddressDTO deliveryAddress;
    
    @JsonProperty("parcel_details")
    @Valid
    private ParcelDetailsDTO parcelDetails;
    
    @JsonProperty("service_options")
    @Valid
    private ServiceOptionsDTO serviceOptions;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    // Constructors
    public EDIParcelOrderDTO() {}
    
    // Getters and Setters
    public String getEdiReference() {
        return ediReference;
    }
    
    public void setEdiReference(String ediReference) {
        this.ediReference = ediReference;
    }
    
    public String getParcelId() {
        return parcelId;
    }
    
    public void setParcelId(String parcelId) {
        this.parcelId = parcelId;
    }
    
    public CustomerDTO getSender() {
        return sender;
    }
    
    public void setSender(CustomerDTO sender) {
        this.sender = sender;
    }
    
    public CustomerDTO getRecipient() {
        return recipient;
    }
    
    public void setRecipient(CustomerDTO recipient) {
        this.recipient = recipient;
    }
    
    public AddressDTO getPickupAddress() {
        return pickupAddress;
    }
    
    public void setPickupAddress(AddressDTO pickupAddress) {
        this.pickupAddress = pickupAddress;
    }
    
    public AddressDTO getDeliveryAddress() {
        return deliveryAddress;
    }
    
    public void setDeliveryAddress(AddressDTO deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    
    public ParcelDetailsDTO getParcelDetails() {
        return parcelDetails;
    }
    
    public void setParcelDetails(ParcelDetailsDTO parcelDetails) {
        this.parcelDetails = parcelDetails;
    }
    
    public ServiceOptionsDTO getServiceOptions() {
        return serviceOptions;
    }
    
    public void setServiceOptions(ServiceOptionsDTO serviceOptions) {
        this.serviceOptions = serviceOptions;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    // Nested DTOs
    public static class ParcelDetailsDTO {
        @JsonProperty("description")
        @Size(max = 200)
        private String description;
        
        @JsonProperty("weight")
        @DecimalMin(value = "0.0", inclusive = false, message = "Weight must be greater than 0")
        private BigDecimal weight;
        
        @JsonProperty("dimensions")
        @Size(max = 20)
        private String dimensions;
        
        @JsonProperty("value")
        private BigDecimal value;
        
        @JsonProperty("fragile")
        private Boolean fragile;
        
        // Getters and Setters
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
        
        public BigDecimal getValue() {
            return value;
        }
        
        public void setValue(BigDecimal value) {
            this.value = value;
        }
        
        public Boolean getFragile() {
            return fragile;
        }
        
        public void setFragile(Boolean fragile) {
            this.fragile = fragile;
        }
    }
    
    public static class ServiceOptionsDTO {
        @JsonProperty("priority")
        private String priority;
        
        @JsonProperty("insurance")
        private Boolean insurance;
        
        @JsonProperty("signature_required")
        private Boolean signatureRequired;
        
        @JsonProperty("estimated_delivery_date")
        private LocalDateTime estimatedDeliveryDate;
        
        // Getters and Setters
        public String getPriority() {
            return priority;
        }
        
        public void setPriority(String priority) {
            this.priority = priority;
        }
        
        public Boolean getInsurance() {
            return insurance;
        }
        
        public void setInsurance(Boolean insurance) {
            this.insurance = insurance;
        }
        
        public Boolean getSignatureRequired() {
            return signatureRequired;
        }
        
        public void setSignatureRequired(Boolean signatureRequired) {
            this.signatureRequired = signatureRequired;
        }
        
        public LocalDateTime getEstimatedDeliveryDate() {
            return estimatedDeliveryDate;
        }
        
        public void setEstimatedDeliveryDate(LocalDateTime estimatedDeliveryDate) {
            this.estimatedDeliveryDate = estimatedDeliveryDate;
        }
    }
}
