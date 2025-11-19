package com.tracking.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackRegisterRequestDto {

    @JsonProperty("number")
    private String number;

    @JsonProperty("carrier")
    private Integer carrier;

    @JsonProperty("auto_detection")
    private Boolean autoDetection;

    @JsonProperty("tag")
    private String tag;
}
