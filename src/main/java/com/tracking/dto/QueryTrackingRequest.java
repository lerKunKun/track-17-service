package com.tracking.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryTrackingRequest {

    @NotNull(message = "Tracking numbers are required")
    @JsonProperty("numbers")
    private List<String> numbers;
}
