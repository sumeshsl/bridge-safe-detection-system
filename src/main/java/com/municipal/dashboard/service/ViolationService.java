package com.municipal.dashboard.service;

import com.municipal.dashboard.dto.HeightDetectionMessage;
import com.municipal.dashboard.dto.ViolationResponse;
import com.municipal.dashboard.model.Detector;
import com.municipal.dashboard.model.Violation;
import com.municipal.dashboard.model.ViolationStatus;
import com.municipal.dashboard.repository.ViolationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViolationService {
    
    private final ViolationRepository violationRepository;
    private final DetectorService detectorService;
    private final WebSocketMessagingService webSocketMessagingService;
    
    @Transactional
    public ViolationResponse processHeightDetection(HeightDetectionMessage message) {
        log.info("Processing height detection from device: {}, height: {}", 
                 message.getDeviceId(), message.getHeight());
        
        Detector detector = detectorService.findByDeviceId(message.getDeviceId())
            .orElseThrow(() -> new RuntimeException("Detector not found: " + message.getDeviceId()));
        
        if (message.getHeight() > detector.getClearanceHeight()) {
            return recordViolation(detector, message);
        }
        
        log.debug("No violation - height {} is within clearance {}", 
                  message.getHeight(), detector.getClearanceHeight());
        return null;
    }
    
    @Transactional
    protected ViolationResponse recordViolation(Detector detector, HeightDetectionMessage message) {
        Violation violation = Violation.builder()
            .detector(detector)
            .detectedHeight(message.getHeight())
            .clearanceHeight(detector.getClearanceHeight())
            .build();
        
        violation = violationRepository.save(violation);
        log.warn("VIOLATION DETECTED - Device: {}, Location: {}, Height: {}, Clearance: {}, Excess: {}, Severity: {}",
                 detector.getDeviceId(), 
                 detector.getLocation(),
                 message.getHeight(),
                 detector.getClearanceHeight(),
                 violation.getExcessHeight(),
                 violation.getSeverity());
        
        ViolationResponse response = mapToResponse(violation);
        
        webSocketMessagingService.broadcastNewViolation(response);
        
        return response;
    }
    
    public List<ViolationResponse> getPendingViolations() {
        return violationRepository.findPendingViolations().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    public List<ViolationResponse> getViolationsByDevice(String deviceId) {
        return violationRepository.findByDetectorDeviceId(deviceId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    public List<ViolationResponse> getViolationsByDateRange(LocalDateTime start, LocalDateTime end) {
        return violationRepository.findByDetectedAtBetween(start, end).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public ViolationResponse acknowledgeViolation(Long violationId, String notes) {
        Violation violation = violationRepository.findById(violationId)
            .orElseThrow(() -> new RuntimeException("Violation not found"));
        
        violation.setStatus(ViolationStatus.ACKNOWLEDGED);
        violation.setAcknowledgedAt(LocalDateTime.now());
        violation.setNotes(notes);
        
        violation = violationRepository.save(violation);
        log.info("Violation {} acknowledged", violationId);
        
        ViolationResponse response = mapToResponse(violation);
        
        webSocketMessagingService.broadcastViolationAcknowledged(response);
        
        return response;
    }
    
    private ViolationResponse mapToResponse(Violation violation) {
        return ViolationResponse.builder()
            .id(violation.getId())
            .deviceId(violation.getDetector().getDeviceId())
            .location(violation.getDetector().getLocation())
            .detectedHeight(violation.getDetectedHeight())
            .clearanceHeight(violation.getClearanceHeight())
            .excessHeight(violation.getExcessHeight())
            .severity(violation.getSeverity())
            .status(violation.getStatus())
            .notes(violation.getNotes())
            .detectedAt(violation.getDetectedAt())
            .acknowledgedAt(violation.getAcknowledgedAt())
            .build();
    }
}