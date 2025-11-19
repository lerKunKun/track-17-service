package com.tracking.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("tracking_number")
    private String trackingNumber;

    @JsonProperty("carrier_code")
    private Integer carrierCode;

    @JsonProperty("carrier_name")
    private String carrierName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private String status;

    @JsonProperty("substatus")
    private String substatus;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("last_synced_at")
    private LocalDateTime lastSyncedAt;

    @JsonProperty("track_info")
    private Map<String, Object> trackInfo;

    @JsonProperty("events")
    private List<TrackingEventResponse> events;
}
