package com.tracking.dto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackingLocation {

    @JsonProperty("country")
    private String country;

    @JsonProperty("city")
    private String city;
}
