package com.tracking.dto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackInfo {

    @JsonProperty("provider_name")
    private String providerName;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("substatus")
    private Integer substatus;

    @JsonProperty("latest_event")
    private TrackEvent latestEvent;

    @JsonProperty("origin")
    private TrackingLocation origin;

    @JsonProperty("destination")
    private TrackingLocation destination;

    @JsonProperty("events")
    private List<TrackEvent> events;
}
