package com.municipal.dashboard.service;

import com.municipal.dashboard.model.Detector;
import com.municipal.dashboard.model.Violation;
import com.municipal.dashboard.repository.DetectorRepository;
import com.municipal.dashboard.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetectorService {
    
    private final DetectorRepository detectorRepository;
    private final ViolationRepository violationRepository;  // ← Added
    private final WebSocketMessagingService webSocketMessagingService;  // ← Added
    
    @Value("${app.violation.height-threshold}")
    private Double defaultClearanceHeight;
    
    @Value("${app.detector.heartbeat-timeout}")
    private Integer heartbeatTimeout;
    
    @Transactional
    public Detector registerDetector(String deviceId, String location, Double clearanceHeight) {
        Optional<Detector> existing = detectorRepository.findByDeviceId(deviceId);
        
        if (existing.isPresent()) {
            log.info("Detector already registered: {}", deviceId);
            return existing.get();
        }
        
        Detector detector = Detector.builder()
            .deviceId(deviceId)
            .location(location)
            .clearanceHeight(clearanceHeight != null ? clearanceHeight : defaultClearanceHeight)
            .active(true)
            .build();
        
        detector = detectorRepository.save(detector);
        log.info("Registered new detector: {} at {}", deviceId, location);
        return detector;
    }
    
    @Transactional
    public void updateHeartbeat(String deviceId) {
        detectorRepository.findByDeviceId(deviceId).ifPresent(detector -> {
            detector.setLastHeartbeat(LocalDateTime.now());
            detectorRepository.save(detector);
            log.debug("Updated heartbeat for detector: {}", deviceId);
        });
    }
    
    public Optional<Detector> findByDeviceId(String deviceId) {
        return detectorRepository.findByDeviceId(deviceId);
    }
    
    public List<Detector> findAllActive() {
        return detectorRepository.findByActive(true);
    }
    
    public List<Detector> findInactiveDetectors() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeout);
        return detectorRepository.findInactiveDetectors(threshold);
    }
    
    @Transactional
    public void deactivateDetector(String deviceId) {
        detectorRepository.findByDeviceId(deviceId).ifPresent(detector -> {
            detector.setActive(false);
            detectorRepository.save(detector);
            log.info("Deactivated detector: {}", deviceId);
        });
    }
    
    @Transactional
    public void deleteDetector(String deviceId) {
        Detector detector = detectorRepository.findByDeviceId(deviceId)
            .orElseThrow(() -> new RuntimeException("Detector not found: " + deviceId));
        
        // Delete all violations associated with this detector
        List<Violation> violations = violationRepository.findByDetectorDeviceId(deviceId);
        violationRepository.deleteAll(violations);
        
        // Delete the detector
        detectorRepository.delete(detector);
        
        log.info("Deleted detector and {} violations: {}", violations.size(), deviceId);
        
        // Broadcast deletion event (optional)
        webSocketMessagingService.broadcastSystemAlert(
            "Detector " + deviceId + " has been deleted from the system"
        );
    }
}