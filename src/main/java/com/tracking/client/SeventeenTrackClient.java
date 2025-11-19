package com.tracking.client;


import com.tracking.dto.*;
import com.tracking.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeventeenTrackClient {

    private final WebClient.Builder webClientBuilder;
    private final RedisCacheService cacheService;

    @Value("${tracking.api.base-url}")
    private String baseUrl;

    @Value("${tracking.api.key}")
    private String apiKey;

    @Value("${tracking.api.timeout:30000}")
    private int timeout;

    @Value("${tracking.api.max-retries:3}")
    private int maxRetries;

    // ============= Token Management =============

    public String getToken() {
        // Check cache first
        return cacheService.getToken().orElseGet(() -> {
            log.info("Fetching new API token from 17-Track");
            String token = fetchTokenFromApi();
            cacheService.cacheToken(token, 3600); // Cache for 1 hour
            return token;
        });
    }

    private String fetchTokenFromApi() {
        WebClient client = createWebClient();

        try {
            Map<String, String> response = client.post()
                    .uri("/token/new")
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of("api_key", apiKey))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .retryWhen(Retry.fixedDelay(maxRetries, Duration.ofSeconds(2)))
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response != null && response.containsKey("token")) {
                return response.get("token");
            }
            throw new RuntimeException("Failed to get token from API response");
        } catch (Exception e) {
            log.error("Error fetching token from 17-Track API", e);
            throw new RuntimeException("Failed to fetch API token", e);
        }
    }

    // ============= Register Tracking =============

    public TrackApiResponse<List<Map<String, Object>>> registerTracking(
            String trackingNumber, Integer carrier) {

        WebClient client = createAuthenticatedClient();

        TrackRegisterRequestDto requestDto = TrackRegisterRequestDto.builder()
                .number(trackingNumber)
                .carrier(carrier)
                .autoDetection(carrier == null || carrier == 0)
                .build();

        try {
            log.info("Registering tracking number: {}", trackingNumber);

            TrackApiResponse<List<Map<String, Object>>> response = client.post()
                    .uri("/track/v2/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(List.of(requestDto))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<TrackApiResponse<List<Map<String, Object>>>>() {})
                    .retryWhen(Retry.fixedDelay(maxRetries, Duration.ofSeconds(2)))
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.info("Registration response code: {}", response != null ? response.getCode() : null);
            return response;
        } catch (Exception e) {
            log.error("Error registering tracking number: {}", trackingNumber, e);
            throw new RuntimeException("Failed to register tracking number", e);
        }
    }

    public TrackApiResponse<List<Map<String, Object>>> registerBatch(
            List<TrackRegisterRequestDto> trackings) {

        WebClient client = createAuthenticatedClient();

        try {
            log.info("Registering batch of {} tracking numbers", trackings.size());

            TrackApiResponse<List<Map<String, Object>>> response = client.post()
                    .uri("/track/v2/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(trackings)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<TrackApiResponse<List<Map<String, Object>>>>() {})
                    .retryWhen(Retry.fixedDelay(maxRetries, Duration.ofSeconds(2)))
                    .timeout(Duration.ofMillis(timeout * 2)) // Longer timeout for batch
                    .block();

            log.info("Batch registration response code: {}", response != null ? response.getCode() : null);
            return response;
        } catch (Exception e) {
            log.error("Error registering batch tracking numbers", e);
            throw new RuntimeException("Failed to register batch", e);
        }
    }

    // ============= Query Tracking Status =============

    public TrackApiResponse<Map<String, Object>> queryTracking(List<String> trackingNumbers) {
        WebClient client = createAuthenticatedClient();

        try {
            log.info("Querying {} tracking numbers", trackingNumbers.size());

            QueryTrackingRequest request = QueryTrackingRequest.builder()
                    .numbers(trackingNumbers)
                    .build();

            TrackApiResponse<Map<String, Object>> response = client.post()
                    .uri("/track/v2/gettracklist")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<TrackApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.fixedDelay(maxRetries, Duration.ofSeconds(2)))
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.info("Query response code: {}", response != null ? response.getCode() : null);
            return response;
        } catch (Exception e) {
            log.error("Error querying tracking numbers", e);
            throw new RuntimeException("Failed to query tracking status", e);
        }
    }

    // ============= Get Carrier Info =============

    public TrackApiResponse<List<Map<String, Object>>> getCarriers() {
        WebClient client = createAuthenticatedClient();

        try {
            log.info("Fetching carrier list");

            TrackApiResponse<List<Map<String, Object>>> response = client.get()
                    .uri("/track/v2/getcarrierlist")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<TrackApiResponse<List<Map<String, Object>>>>() {})
                    .retryWhen(Retry.fixedDelay(maxRetries, Duration.ofSeconds(2)))
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.info("Fetched carriers, response code: {}", response != null ? response.getCode() : null);
            return response;
        } catch (Exception e) {
            log.error("Error fetching carriers", e);
            throw new RuntimeException("Failed to fetch carriers", e);
        }
    }

    // ============= Auto-detect Carrier =============

    public TrackApiResponse<Map<String, Object>> detectCarrier(String trackingNumber) {
        WebClient client = createAuthenticatedClient();

        try {
            log.info("Auto-detecting carrier for: {}", trackingNumber);

            TrackApiResponse<Map<String, Object>> response = client.post()
                    .uri("/track/v2/getcarrierbytracknum")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("number", trackingNumber))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<TrackApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.fixedDelay(maxRetries, Duration.ofSeconds(2)))
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.info("Carrier detection response code: {}", response != null ? response.getCode() : null);
            return response;
        } catch (Exception e) {
            log.error("Error detecting carrier for: {}", trackingNumber, e);
            throw new RuntimeException("Failed to detect carrier", e);
        }
    }

    // ============= Helper Methods =============

    private WebClient createWebClient() {
        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private WebClient createAuthenticatedClient() {
        String token = getToken();
        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("17token", token)
                .build();
    }
}