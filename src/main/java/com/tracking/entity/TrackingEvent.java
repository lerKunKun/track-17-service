package com.tracking.entity;

@Entity
@Table(name = "tracking_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tracking_id", nullable = false)
    private Long trackingId;
    
    @Column(name = "tracking_number", nullable = false, length = 100)
    private String trackingNumber;
    
    @Column(name = "event_time")
    private LocalDateTime eventTime;
    
    @Column(name = "event_description", columnDefinition = "TEXT")
    private String eventDescription;
    
    @Column(name = "event_location")
    private String eventLocation;
    
    @Column(name = "event_code", length = 50)
    private String eventCode;
    
    @Column(name = "stage", length = 50)
    private String stage;
    
    @Column(name = "substatus", length = 50)
    private String substatus;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "json")
    private Map<String, Object> eventData;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}