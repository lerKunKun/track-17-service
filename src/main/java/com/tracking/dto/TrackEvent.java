package com.tracking.dto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackEvent {

    @JsonProperty("time_iso")
    private String timeIso;

    @JsonProperty("description")
    private String description;

    @JsonProperty("location")
    private String location;

    @JsonProperty("stage")
    private String stage;

    @JsonProperty("substatus")
    private Integer substatus;
}
