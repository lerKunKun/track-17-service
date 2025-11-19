package com.tracking.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRegisterRequest {

    @NotNull(message = "Tracking list cannot be null")
    @JsonProperty("trackings")
    private List<RegisterTrackingRequest> trackings;
}
