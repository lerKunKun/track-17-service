package com.tracking.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResult {

    @JsonProperty("tracking_number")
    private String trackingNumber;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("tracking_id")
    private Long trackingId;
}
