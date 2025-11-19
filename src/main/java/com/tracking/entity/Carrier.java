package com.tracking.entity;

@Entity
@Table(name = "carriers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrier {
    
    @Id
    @Column(name = "carrier_code")
    private Integer carrierCode;
    
    @Column(name = "carrier_name", nullable = false, length = 100)
    private String carrierName;
    
    @Column(name = "carrier_key", length = 50)
    private String carrierKey;
    
    @Column(name = "country", length = 5)
    private String country;
    
    @Column(name = "phone", length = 50)
    private String phone;
    
    @Column(name = "website")
    private String website;
    
    @Column(name = "logo_url")
    private String logoUrl;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}