package com.municipal.dashboard.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "violations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Violation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detector_id", nullable = false)
    private Detector detector;
    
    @Column(nullable = false)
    private Double detectedHeight;
    
    @Column(nullable = false)
    private Double clearanceHeight;
    
    @Column(nullable = false)
    private Double excessHeight;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationSeverity severity;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViolationStatus status = ViolationStatus.DETECTED;
    
    private String notes;
    
    @Column(nullable = false)
    private LocalDateTime detectedAt;
    
    private LocalDateTime acknowledgedAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        detectedAt = LocalDateTime.now();
        
        excessHeight = detectedHeight - clearanceHeight;
        
        if (excessHeight > 2.0) {
            severity = ViolationSeverity.CRITICAL;
        } else if (excessHeight > 1.0) {
            severity = ViolationSeverity.HIGH;
        } else if (excessHeight > 0.5) {
            severity = ViolationSeverity.MEDIUM;
        } else {
            severity = ViolationSeverity.LOW;
        }
    }
}