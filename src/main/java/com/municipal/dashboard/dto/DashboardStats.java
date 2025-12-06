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
public class DashboardStats {
    
    @JsonProperty("total_detectors")
    private Integer totalDetectors;
    
    @JsonProperty("active_detectors")
    private Integer activeDetectors;
    
    @JsonProperty("inactive_detectors")
    private Integer inactiveDetectors;
    
    @JsonProperty("pending_violations")
    private Long pendingViolations;
    
    @JsonProperty("acknowledged_violations")
    private Long acknowledgedViolations;
    
    @JsonProperty("total_violations")
    private Long totalViolations;
    
    @JsonProperty("violations_today")
    private Long violationsToday;
    
    @JsonProperty("critical_violations")
    private Long criticalViolations;
    
    @JsonProperty("last_updated")
    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();
}