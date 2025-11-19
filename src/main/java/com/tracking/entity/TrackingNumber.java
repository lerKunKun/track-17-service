package com.tracking.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

// ============= TrackingNumber Entity =============
@Entity
@Table(name = "tracking_numbers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingNumber {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tracking_number", unique = true, nullable = false, length = 100)
    private String trackingNumber;
    
    @Column(name = "carrier_code")
    private Integer carrierCode;
    
    @Column(name = "carrier_name", length = 100)
    private String carrierName;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private TrackingStatus status;
    
    @Column(name = "substatus", length = 50)
    private String substatus;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
    
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "track_info", columnDefinition = "json")
    private Map<String, Object> trackInfo;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = TrackingStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}