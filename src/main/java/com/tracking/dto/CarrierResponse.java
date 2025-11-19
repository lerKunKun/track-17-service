package com.tracking.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrierResponse {

    @JsonProperty("carrier_code")
    private Integer carrierCode;

    @JsonProperty("carrier_name")
    private String carrierName;

    @JsonProperty("carrier_key")
    private String carrierKey;

    @JsonProperty("country")
    private String country;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("website")
    private String website;

    @JsonProperty("logo_url")
    private String logoUrl;
}
