package com.municipal.dashboard.controller;

import com.municipal.dashboard.dto.DashboardStats;
import com.municipal.dashboard.model.Detector;
import com.municipal.dashboard.model.ViolationSeverity;
import com.municipal.dashboard.model.ViolationStatus;
import com.municipal.dashboard.repository.ViolationRepository;
import com.municipal.dashboard.service.DetectorService;
import com.municipal.dashboard.service.WebSocketMessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {
    
    private final ViolationRepository violationRepository;
    private final DetectorService detectorService;
    private final WebSocketMessagingService webSocketMessagingService;
    
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        List<Detector> activeDetectors = detectorService.findAllActive();
        List<Detector> inactiveDetectors = detectorService.findInactiveDetectors();
        
        Long pendingViolations = violationRepository.countByStatus(ViolationStatus.DETECTED);
        Long acknowledgedViolations = violationRepository.countByStatus(ViolationStatus.ACKNOWLEDGED);
        Long totalViolations = violationRepository.count();
        
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        Long violationsToday = (long) violationRepository
            .findByDetectedAtBetween(startOfDay, endOfDay)
            .size();
        
        Long criticalViolations = (long) violationRepository
            .findBySeverity(ViolationSeverity.CRITICAL)
            .stream()
            .filter(v -> v.getStatus() == ViolationStatus.DETECTED)
            .count();
        
        DashboardStats stats = DashboardStats.builder()
            .totalDetectors(activeDetectors.size() + inactiveDetectors.size())
            .activeDetectors(activeDetectors.size())
            .inactiveDetectors(inactiveDetectors.size())
            .pendingViolations(pendingViolations)
            .acknowledgedViolations(acknowledgedViolations)
            .totalViolations(totalViolations)
            .violationsToday(violationsToday)
            .criticalViolations(criticalViolations)
            .build();
        
        webSocketMessagingService.broadcastStatsUpdate(stats);
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Municipal Dashboard API");
        health.put("websocket", "enabled");
        return ResponseEntity.ok(health);
    }
}