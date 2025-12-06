package com.municipal.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketNotification {
    
    @JsonProperty("notification_type")
    private NotificationType notificationType;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private Object data;
    
    @JsonProperty("timestamp")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @JsonProperty("device_id")
    private String deviceId;
    
    @JsonProperty("location")
    private String location;
    
    public enum NotificationType {
        NEW_VIOLATION,
        VIOLATION_ACK,
        DETECTOR_ONLINE,
        DETECTOR_OFFLINE,
        SYSTEM_ALERT,
        STATS_UPDATE
    }
}