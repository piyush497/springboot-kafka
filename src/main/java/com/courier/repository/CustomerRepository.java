package com.courier.repository;

import com.courier.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    Optional<Customer> findByCustomerCode(String customerCode);
    
    Optional<Customer> findByEmail(String email);
    
    boolean existsByCustomerCode(String customerCode);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT c FROM Customer c WHERE c.name LIKE %:name% OR c.email LIKE %:email%")
    java.util.List<Customer> findByNameOrEmailContaining(@Param("name") String name, @Param("email") String email);
}
