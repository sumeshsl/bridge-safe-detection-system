package com.municipal.dashboard.model;

public enum ViolationStatus {
    DETECTED,      // Just detected
    ACKNOWLEDGED,  // Reviewed by admin
    RESOLVED,      // Action taken
    IGNORED        // False positive
}