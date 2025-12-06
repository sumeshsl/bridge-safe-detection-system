package com.municipal.dashboard.service;

import com.municipal.dashboard.dto.DashboardStats;
import com.municipal.dashboard.dto.ViolationResponse;
import com.municipal.dashboard.dto.WebSocketNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessagingService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void broadcastNewViolation(ViolationResponse violation) {
        WebSocketNotification notification = WebSocketNotification.builder()
            .notificationType(WebSocketNotification.NotificationType.NEW_VIOLATION)
            .message(String.format("New violation detected at %s - Height: %.2f ft (Clearance: %.2f ft)",
                    violation.getLocation(),
                    violation.getDetectedHeight(),
                    violation.getClearanceHeight()))
            .deviceId(violation.getDeviceId())
            .location(violation.getLocation())
            .data(violation)
            .build();
        
        messagingTemplate.convertAndSend("/topic/violations", notification);
        log.info("Broadcasted new violation: {}", violation.getId());
    }
    
    public void broadcastViolationAcknowledged(ViolationResponse violation) {
        WebSocketNotification notification = WebSocketNotification.builder()
            .notificationType(WebSocketNotification.NotificationType.VIOLATION_ACK)
            .message(String.format("Violation #%d acknowledged", violation.getId()))
            .deviceId(violation.getDeviceId())
            .location(violation.getLocation())
            .data(violation)
            .build();
        
        messagingTemplate.convertAndSend("/topic/violations", notification);
        log.info("Broadcasted violation acknowledgement: {}", violation.getId());
    }
    
    public void broadcastDetectorStatusChange(String deviceId, String location, boolean online) {
        WebSocketNotification notification = WebSocketNotification.builder()
            .notificationType(online ? 
                WebSocketNotification.NotificationType.DETECTOR_ONLINE : 
                WebSocketNotification.NotificationType.DETECTOR_OFFLINE)
            .message(String.format("Detector %s is now %s", 
                    deviceId, 
                    online ? "ONLINE" : "OFFLINE"))
            .deviceId(deviceId)
            .location(location)
            .build();
        
        messagingTemplate.convertAndSend("/topic/detectors", notification);
        log.info("Broadcasted detector status change: {} - {}", deviceId, online ? "ONLINE" : "OFFLINE");
    }
    
    public void broadcastStatsUpdate(DashboardStats stats) {
        WebSocketNotification notification = WebSocketNotification.builder()
            .notificationType(WebSocketNotification.NotificationType.STATS_UPDATE)
            .message("Dashboard statistics updated")
            .data(stats)
            .build();
        
        messagingTemplate.convertAndSend("/topic/stats", notification);
        log.debug("Broadcasted stats update");
    }
    
    public void broadcastSystemAlert(String message) {
        WebSocketNotification notification = WebSocketNotification.builder()
            .notificationType(WebSocketNotification.NotificationType.SYSTEM_ALERT)
            .message(message)
            .build();
        
        messagingTemplate.convertAndSend("/topic/alerts", notification);
        log.warn("Broadcasted system alert: {}", message);
    }
}