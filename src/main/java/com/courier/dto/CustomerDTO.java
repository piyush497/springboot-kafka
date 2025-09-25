package com.courier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CustomerDTO {
    
    @JsonProperty("customer_code")
    @Size(max = 50)
    private String customerCode;
    
    @JsonProperty("name")
    @NotBlank(message = "Customer name is required")
    @Size(max = 100)
    private String name;
    
    @JsonProperty("email")
    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;
    
    @JsonProperty("phone")
    @Size(max = 20)
    private String phone;
    
    @JsonProperty("company")
    @Size(max = 100)
    private String company;
    
    // Constructors
    public CustomerDTO() {}
    
    public CustomerDTO(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
    
    // Getters and Setters
    public String getCustomerCode() {
        return customerCode;
    }
    
    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
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
    
    public String getCompany() {
        return company;
    }
    
    public void setCompany(String company) {
        this.company = company;
    }
}
