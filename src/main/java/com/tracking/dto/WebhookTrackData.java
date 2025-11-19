package com.tracking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookTrackData {

    @JsonProperty("number")
    private String number;

    @JsonProperty("carrier")
    private Integer carrier;

    @JsonProperty("track")
    private TrackInfo track;
}
