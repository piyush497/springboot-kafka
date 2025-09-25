package com.courier.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Event for ABC Transport system integration
 */
public class ABCTransportEvent extends ParcelEvent {
    
    @NotNull
    @JsonProperty("messageType")
    private String messageType;
    
    @JsonProperty("ediReference")
    private String ediReference;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("priority")
    private String priority;
    
    @JsonProperty("sender")
    private CustomerInfo sender;
    
    @JsonProperty("recipient")
    private CustomerInfo recipient;
    
    @JsonProperty("pickupAddress")
    private AddressInfo pickupAddress;
    
    @JsonProperty("deliveryAddress")
    private AddressInfo deliveryAddress;
    
    @JsonProperty("parcelDetails")
    private ParcelDetails parcelDetails;
    
    // Constructors
    public ABCTransportEvent() {
        super();
    }
    
    public ABCTransportEvent(String messageType, String parcelId) {
        super("ABC_TRANSPORT_EVENT", parcelId);
        this.messageType = messageType;
    }
    
    // Getters and Setters
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public String getEdiReference() {
        return ediReference;
    }
    
    public void setEdiReference(String ediReference) {
        this.ediReference = ediReference;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public CustomerInfo getSender() {
        return sender;
    }
    
    public void setSender(CustomerInfo sender) {
        this.sender = sender;
    }
    
    public CustomerInfo getRecipient() {
        return recipient;
    }
    
    public void setRecipient(CustomerInfo recipient) {
        this.recipient = recipient;
    }
    
    public AddressInfo getPickupAddress() {
        return pickupAddress;
    }
    
    public void setPickupAddress(AddressInfo pickupAddress) {
        this.pickupAddress = pickupAddress;
    }
    
    public AddressInfo getDeliveryAddress() {
        return deliveryAddress;
    }
    
    public void setDeliveryAddress(AddressInfo deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    
    public ParcelDetails getParcelDetails() {
        return parcelDetails;
    }
    
    public void setParcelDetails(ParcelDetails parcelDetails) {
        this.parcelDetails = parcelDetails;
    }
    
    // Inner classes for structured data
    public static class CustomerInfo {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("email")
        private String email;
        
        @JsonProperty("phone")
        private String phone;
        
        // Constructors, getters, and setters
        public CustomerInfo() {}
        
        public CustomerInfo(String name, String email, String phone) {
            this.name = name;
            this.email = email;
            this.phone = phone;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
    
    public static class AddressInfo {
        @JsonProperty("streetAddress")
        private String streetAddress;
        
        @JsonProperty("city")
        private String city;
        
        @JsonProperty("state")
        private String state;
        
        @JsonProperty("postalCode")
        private String postalCode;
        
        @JsonProperty("country")
        private String country;
        
        // Constructors, getters, and setters
        public AddressInfo() {}
        
        public String getStreetAddress() { return streetAddress; }
        public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }
    
    public static class ParcelDetails {
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("weight")
        private Double weight;
        
        @JsonProperty("dimensions")
        private String dimensions;
        
        // Constructors, getters, and setters
        public ParcelDetails() {}
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
        
        public String getDimensions() { return dimensions; }
        public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    }
}
