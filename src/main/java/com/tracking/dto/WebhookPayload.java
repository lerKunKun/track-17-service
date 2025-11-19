package com.tracking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {

    @JsonProperty("event")
    private String event;

    @JsonProperty("accepted")
    private List<WebhookTrackData> accepted;
}
