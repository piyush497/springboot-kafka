package com.courier.repository;

import com.courier.entity.Parcel;
import com.courier.entity.Parcel.ParcelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, Long> {
    
    Optional<Parcel> findByParcelId(String parcelId);
    
    List<Parcel> findByStatus(ParcelStatus status);
    
    List<Parcel> findBySenderId(Long senderId);
    
    List<Parcel> findByRecipientId(Long recipientId);
    
    @Query("SELECT p FROM Parcel p WHERE p.sender.id = :customerId OR p.recipient.id = :customerId")
    List<Parcel> findByCustomerId(@Param("customerId") Long customerId);
    
    @Query("SELECT p FROM Parcel p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Parcel> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p FROM Parcel p WHERE p.status IN :statuses")
    List<Parcel> findByStatusIn(@Param("statuses") List<ParcelStatus> statuses);
    
    boolean existsByParcelId(String parcelId);
    
    Optional<Parcel> findByEdiReference(String ediReference);
    
    @Query("SELECT COUNT(p) FROM Parcel p WHERE p.status = :status")
    long countByStatus(@Param("status") ParcelStatus status);
    
    // Customer-specific queries for customer interface
    @Query("SELECT p FROM Parcel p WHERE p.sender.id = :customerId OR p.recipient.id = :customerId")
    Page<Parcel> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);
    
    @Query("SELECT p FROM Parcel p WHERE (p.sender.id = :customerId OR p.recipient.id = :customerId) AND p.status = :status")
    Page<Parcel> findByCustomerIdAndStatus(@Param("customerId") Long customerId, @Param("status") ParcelStatus status, Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Parcel p WHERE p.sender.id = :customerId OR p.recipient.id = :customerId")
    long countByCustomerId(@Param("customerId") Long customerId);
    
    @Query("SELECT COUNT(p) FROM Parcel p WHERE (p.sender.id = :customerId OR p.recipient.id = :customerId) AND p.status = :status")
    long countByCustomerIdAndStatus(@Param("customerId") Long customerId, @Param("status") ParcelStatus status);
    
    @Query("SELECT COUNT(p) FROM Parcel p WHERE (p.sender.id = :customerId OR p.recipient.id = :customerId) AND p.createdAt >= :startDate")
    long countByCustomerIdAndCreatedAtAfter(@Param("customerId") Long customerId, @Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT COUNT(p) FROM Parcel p WHERE (p.sender.id = :customerId OR p.recipient.id = :customerId) AND p.status IN :statuses")
    long countByCustomerIdAndStatusIn(@Param("customerId") Long customerId, @Param("statuses") java.util.List<ParcelStatus> statuses);
}
