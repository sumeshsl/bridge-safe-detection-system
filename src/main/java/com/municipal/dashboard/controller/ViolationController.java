package com.municipal.dashboard.controller;

import com.municipal.dashboard.dto.ViolationResponse;
import com.municipal.dashboard.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ViolationController {
    
    private final ViolationService violationService;
    
    @GetMapping("/pending")
    public ResponseEntity<List<ViolationResponse>> getPendingViolations() {
        List<ViolationResponse> violations = violationService.getPendingViolations();
        return ResponseEntity.ok(violations);
    }
    
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<ViolationResponse>> getViolationsByDevice(
            @PathVariable String deviceId) {
        List<ViolationResponse> violations = violationService.getViolationsByDevice(deviceId);
        return ResponseEntity.ok(violations);
    }
    
    @GetMapping
    public ResponseEntity<List<ViolationResponse>> getViolationsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<ViolationResponse> violations = violationService.getViolationsByDateRange(startDate, endDate);
        return ResponseEntity.ok(violations);
    }
    
    @PutMapping("/{violationId}/acknowledge")
    public ResponseEntity<ViolationResponse> acknowledgeViolation(
            @PathVariable Long violationId,
            @RequestBody(required = false) String notes) {
        ViolationResponse response = violationService.acknowledgeViolation(violationId, notes);
        return ResponseEntity.ok(response);
    }
}