package com.tracking.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRegisterResponse {

    @JsonProperty("success_count")
    private Integer successCount;

    @JsonProperty("error_count")
    private Integer errorCount;

    @JsonProperty("results")
    private List<RegisterResult> results;
}
