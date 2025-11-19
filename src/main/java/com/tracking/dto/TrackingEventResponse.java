package com.tracking.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEventResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("event_time")
    private LocalDateTime eventTime;

    @JsonProperty("description")
    private String eventDescription;

    @JsonProperty("location")
    private String eventLocation;

    @JsonProperty("event_code")
    private String eventCode;

    @JsonProperty("stage")
    private String stage;

    @JsonProperty("substatus")
    private String substatus;
}
