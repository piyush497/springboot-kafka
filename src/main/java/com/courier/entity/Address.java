package com.courier.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "addresses")
public class Address {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Street address is required")
    @Size(max = 200)
    @Column(name = "street_address", nullable = false)
    private String streetAddress;
    
    @Size(max = 100)
    @Column(name = "city")
    private String city;
    
    @Size(max = 100)
    @Column(name = "state")
    private String state;
    
    @Size(max = 20)
    @Column(name = "postal_code")
    private String postalCode;
    
    @Size(max = 100)
    @Column(name = "country")
    private String country;
    
    @Size(max = 50)
    @Column(name = "landmark")
    private String landmark;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "address_type")
    private AddressType addressType;
    
    // Constructors
    public Address() {}
    
    public Address(String streetAddress, String city, String state, String postalCode, String country) {
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public AddressType getAddressType() {
        return addressType;
    }
    
    public void setAddressType(AddressType addressType) {
        this.addressType = addressType;
    }
    
    public enum AddressType {
        PICKUP, DELIVERY, WAREHOUSE, TRANSIT_HUB
    }
}
