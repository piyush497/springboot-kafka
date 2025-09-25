package com.courier.repository;

import com.courier.entity.TrackingEvent;
import com.courier.entity.TrackingEvent.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    
    List<TrackingEvent> findByParcelIdOrderByEventTimestampDesc(Long parcelId);
    
    List<TrackingEvent> findByParcelParcelIdOrderByEventTimestampDesc(String parcelId);
    
    List<TrackingEvent> findByEventType(EventType eventType);
    
    @Query("SELECT te FROM TrackingEvent te WHERE te.parcel.id = :parcelId AND te.eventType = :eventType")
    List<TrackingEvent> findByParcelIdAndEventType(@Param("parcelId") Long parcelId, 
                                                   @Param("eventType") EventType eventType);
    
    @Query("SELECT te FROM TrackingEvent te WHERE te.eventTimestamp BETWEEN :startDate AND :endDate ORDER BY te.eventTimestamp DESC")
    List<TrackingEvent> findByEventTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                                    @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT te FROM TrackingEvent te WHERE te.parcel.id = :parcelId ORDER BY te.eventTimestamp DESC LIMIT 1")
    TrackingEvent findLatestEventByParcelId(@Param("parcelId") Long parcelId);
    
    List<TrackingEvent> findByLocationContainingIgnoreCase(String location);
    
    List<TrackingEvent> findByVehicleId(String vehicleId);
}
