package com.municipal.dashboard.dto;

import com.municipal.dashboard.model.ViolationSeverity;
import com.municipal.dashboard.model.ViolationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationResponse {
    private Long id;
    private String deviceId;
    private String location;
    private Double detectedHeight;
    private Double clearanceHeight;
    private Double excessHeight;
    private ViolationSeverity severity;
    private ViolationStatus status;
    private String notes;
    private LocalDateTime detectedAt;
    private LocalDateTime acknowledgedAt;
}