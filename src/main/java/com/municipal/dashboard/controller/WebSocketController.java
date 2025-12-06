package com.municipal.dashboard.controller;

import com.municipal.dashboard.dto.DashboardStats;
import com.municipal.dashboard.model.ViolationSeverity;
import com.municipal.dashboard.model.ViolationStatus;
import com.municipal.dashboard.repository.ViolationRepository;
import com.municipal.dashboard.service.DetectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {
    
    private final ViolationRepository violationRepository;
    private final DetectorService detectorService;
    
    @MessageMapping("/dashboard/stats")
    @SendTo("/topic/stats")
    public DashboardStats getDashboardStats() {
        log.info("WebSocket: Dashboard stats requested");
        
        var activeDetectors = detectorService.findAllActive();
        var inactiveDetectors = detectorService.findInactiveDetectors();
        
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
        
        return DashboardStats.builder()
            .totalDetectors(activeDetectors.size() + inactiveDetectors.size())
            .activeDetectors(activeDetectors.size())
            .inactiveDetectors(inactiveDetectors.size())
            .pendingViolations(pendingViolations)
            .acknowledgedViolations(acknowledgedViolations)
            .totalViolations(totalViolations)
            .violationsToday(violationsToday)
            .criticalViolations(criticalViolations)
            .build();
    }
    
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String handlePing() {
        return "pong";
    }
}