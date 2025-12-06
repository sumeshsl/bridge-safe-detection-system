package com.municipal.dashboard.repository;

import com.municipal.dashboard.model.Detector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DetectorRepository extends JpaRepository<Detector, Long> {
    
    Optional<Detector> findByDeviceId(String deviceId);
    
    List<Detector> findByActive(Boolean active);
    
    @Query("SELECT d FROM Detector d WHERE d.lastHeartbeat < :threshold")
    List<Detector> findInactiveDetectors(LocalDateTime threshold);
    
    boolean existsByDeviceId(String deviceId);
}