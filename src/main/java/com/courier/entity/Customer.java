package com.courier.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Customer name is required")
    @Size(max = 100)
    @Column(name = "name", nullable = false)
    private String name;
    
    @Email(message = "Invalid email format")
    @Size(max = 100)
    @Column(name = "email")
    private String email;
    
    @Size(max = 20)
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "customer_code", unique = true)
    private String customerCode;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Parcel> sentParcels;
    
    @OneToMany(mappedBy = "recipient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Parcel> receivedParcels;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public Customer() {}
    
    public Customer(String name, String email, String phone, String customerCode) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.customerCode = customerCode;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getCustomerCode() {
        return customerCode;
    }
    
    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
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
    
    public List<Parcel> getSentParcels() {
        return sentParcels;
    }
    
    public void setSentParcels(List<Parcel> sentParcels) {
        this.sentParcels = sentParcels;
    }
    
    public List<Parcel> getReceivedParcels() {
        return receivedParcels;
    }
    
    public void setReceivedParcels(List<Parcel> receivedParcels) {
        this.receivedParcels = receivedParcels;
    }
}
