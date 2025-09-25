package com.courier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddressDTO {
    
    @JsonProperty("street_address")
    @NotBlank(message = "Street address is required")
    @Size(max = 200)
    private String streetAddress;
    
    @JsonProperty("city")
    @Size(max = 100)
    private String city;
    
    @JsonProperty("state")
    @Size(max = 100)
    private String state;
    
    @JsonProperty("postal_code")
    @Size(max = 20)
    private String postalCode;
    
    @JsonProperty("country")
    @Size(max = 100)
    private String country;
    
    @JsonProperty("landmark")
    @Size(max = 50)
    private String landmark;
    
    @JsonProperty("contact_person")
    @Size(max = 100)
    private String contactPerson;
    
    @JsonProperty("contact_phone")
    @Size(max = 20)
    private String contactPhone;
    
    // Constructors
    public AddressDTO() {}
    
    public AddressDTO(String streetAddress, String city, String state, String postalCode, String country) {
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }
    
    // Getters and Setters
    public String getStreetAddress() {
        return streetAddress;
    }
    
    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getPostalCode() {
        return postalCode;
    }
    
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getLandmark() {
        return landmark;
    }
    
    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }
    
    public String getContactPerson() {
        return contactPerson;
    }
    
    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }
    
    public String getContactPhone() {
        return contactPhone;
    }
    
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}
