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
public class HeightDetectionMessage {
    
    @JsonProperty("device_id")
    private String deviceId;
    
    @JsonProperty("height")
    private Double height;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("sensor_status")
    private String sensorStatus;
    
    @JsonProperty("temperature")
    private Double temperature;
}