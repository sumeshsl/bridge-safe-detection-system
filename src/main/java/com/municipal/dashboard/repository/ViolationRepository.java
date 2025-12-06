package com.municipal.dashboard.repository;

import com.municipal.dashboard.model.Violation;
import com.municipal.dashboard.model.ViolationSeverity;
import com.municipal.dashboard.model.ViolationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {
    
    List<Violation> findByDetectorDeviceId(String deviceId);
    
    List<Violation> findByStatus(ViolationStatus status);
    
    List<Violation> findBySeverity(ViolationSeverity severity);
    
    List<Violation> findByDetectedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT v FROM Violation v WHERE v.detector.deviceId = :deviceId " +
           "AND v.detectedAt BETWEEN :start AND :end")
    List<Violation> findByDeviceAndDateRange(String deviceId, 
                                             LocalDateTime start, 
                                             LocalDateTime end);
    
    @Query("SELECT COUNT(v) FROM Violation v WHERE v.status = :status")
    Long countByStatus(ViolationStatus status);
    
    @Query("SELECT v FROM Violation v WHERE v.status = 'DETECTED' " +
           "ORDER BY v.severity DESC, v.detectedAt DESC")
    List<Violation> findPendingViolations();
}