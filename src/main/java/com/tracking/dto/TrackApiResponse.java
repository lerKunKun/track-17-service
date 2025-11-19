package com.tracking.dto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackApiResponse<T> {

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private T data;
}
