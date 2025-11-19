package com.tracking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// ============= Request DTOs =============

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterTrackingRequest {

    @NotBlank(message = "Tracking number is required")
    @JsonProperty("tracking_number")
    private String trackingNumber;

    @JsonProperty("carrier")
    private Integer carrier;

    @JsonProperty("description")
    private String description;
}