package com.municipal.dashboard.controller;

import com.municipal.dashboard.model.Detector;
import com.municipal.dashboard.service.DetectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/detectors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DetectorController {
    
    private final DetectorService detectorService;
    
    @PostMapping("/register")
    public ResponseEntity<Detector> registerDetector(
            @RequestParam String deviceId,
            @RequestParam String location,
            @RequestParam(required = false) Double clearanceHeight) {
        
        Detector detector = detectorService.registerDetector(deviceId, location, clearanceHeight);
        return ResponseEntity.status(HttpStatus.CREATED).body(detector);
    }
    
    @GetMapping
    public ResponseEntity<List<Detector>> getAllActiveDetectors() {
        List<Detector> detectors = detectorService.findAllActive();
        return ResponseEntity.ok(detectors);
    }
    
    @GetMapping("/{deviceId}")
    public ResponseEntity<Detector> getDetector(@PathVariable String deviceId) {
        return detectorService.findByDeviceId(deviceId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/inactive")
    public ResponseEntity<List<Detector>> getInactiveDetectors() {
        List<Detector> detectors = detectorService.findInactiveDetectors();
        return ResponseEntity.ok(detectors);
    }
    
    @PutMapping("/{deviceId}/deactivate")
    public ResponseEntity<Void> deactivateDetector(@PathVariable String deviceId) {
        detectorService.deactivateDetector(deviceId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{deviceId}/heartbeat")
    public ResponseEntity<Void> updateHeartbeat(@PathVariable String deviceId) {
        detectorService.updateHeartbeat(deviceId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Map<String, Object>> deleteDetector(@PathVariable String deviceId) {
        try {
            detectorService.deleteDetector(deviceId);
            return ResponseEntity.ok(Map.of(
                "message", "Detector and associated violations deleted successfully",
                "deviceId", deviceId,
                "timestamp", LocalDateTime.now().toString()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "error", e.getMessage(),
                    "deviceId", deviceId,
                    "timestamp", LocalDateTime.now().toString()
                ));
        }
    }
}