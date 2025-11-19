package com.tracking.service;


import com.opencsv.CSVReader;
import com.tracking.client.SeventeenTrackClient;
import com.tracking.dto.*;
import com.tracking.entity.TrackingEvent;
import com.tracking.entity.TrackingNumber;
import com.tracking.entity.TrackingStatus;
import com.tracking.repository.CarrierRepository;
import com.tracking.repository.TrackingEventRepository;
import com.tracking.repository.TrackingNumberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final TrackingNumberRepository trackingRepository;
    private final TrackingEventRepository eventRepository;
    private final CarrierRepository carrierRepository;
    private final SeventeenTrackClient apiClient;
    private final RedisCacheService cacheService;

    @Value("${tracking.rate-limit.register-per-day}")
    private int registerDailyLimit;

    @Value("${tracking.rate-limit.query-per-day}")
    private int queryDailyLimit;

    @Value("${tracking.cache.status-ttl}")
    private long statusCacheTtl;

    @Value("${tracking.retry.max-attempts}")
    private int maxRetryAttempts;

    // ============= Register Single Tracking =============

    @Transactional
    public TrackingResponse registerTracking(RegisterTrackingRequest request) {
        log.info("Registering tracking number: {}", request.getTrackingNumber());

        // Check rate limit
        if (!cacheService.checkRateLimit("register", registerDailyLimit, 86400)) {
            throw new RuntimeException("Daily registration limit exceeded");
        }

        // Check if already exists
        Optional<TrackingNumber> existing = trackingRepository
                .findByTrackingNumber(request.getTrackingNumber());

        if (existing.isPresent()) {
            log.info("Tracking number already exists: {}", request.getTrackingNumber());
            return convertToResponse(existing.get());
        }

        // Call 17-Track API
        TrackApiResponse<List<Map<String, Object>>> apiResponse =
                apiClient.registerTracking(request.getTrackingNumber(), request.getCarrier());

        if (apiResponse.getCode() != 0) {
            throw new RuntimeException("API error: " + apiResponse.getMessage());
        }

        // Extract carrier info from response
        Integer carrierCode = request.getCarrier();
        String carrierName = null;

        if (apiResponse.getData() != null && !apiResponse.getData().isEmpty()) {
            Map<String, Object> trackData = apiResponse.getData().get(0);
            if (trackData.containsKey("carrier")) {
                carrierCode = (Integer) trackData.get("carrier");
            }
            if (trackData.containsKey("carrier_name")) {
                carrierName = (String) trackData.get("carrier_name");
            }
        }

        // Save to database
        TrackingNumber tracking = TrackingNumber.builder()
                .trackingNumber(request.getTrackingNumber())
                .carrierCode(carrierCode)
                .carrierName(carrierName)
                .description(request.getDescription())
                .status(TrackingStatus.PENDING)
                .retryCount(0)
                .build();

        tracking = trackingRepository.save(tracking);
        log.info("Saved tracking number with ID: {}", tracking.getId());

        return convertToResponse(tracking);
    }

    // ============= Register Batch =============

    @Transactional
    public BatchRegisterResponse registerBatch(BatchRegisterRequest request) {
        log.info("Registering batch of {} tracking numbers", request.getTrackings().size());

        List<RegisterResult> results = new ArrayList<>();
        int successCount = 0;
        int errorCount = 0;

        for (RegisterTrackingRequest trackingRequest : request.getTrackings()) {
            try {
                TrackingResponse response = registerTracking(trackingRequest);
                results.add(RegisterResult.builder()
                        .trackingNumber(trackingRequest.getTrackingNumber())
                        .success(true)
                        .trackingId(response.getId())
                        .message("Successfully registered")
                        .build());
                successCount++;
            } catch (Exception e) {
                log.error("Error registering tracking number: {}",
                        trackingRequest.getTrackingNumber(), e);
                results.add(RegisterResult.builder()
                        .trackingNumber(trackingRequest.getTrackingNumber())
                        .success(false)
                        .message(e.getMessage())
                        .build());
                errorCount++;
            }
        }

        return BatchRegisterResponse.builder()
                .successCount(successCount)
                .errorCount(errorCount)
                .results(results)
                .build();
    }

    // ============= Import from CSV =============

    @Transactional
    public BatchRegisterResponse importFromCsv(MultipartFile file) {
        log.info("Importing tracking numbers from CSV: {}", file.getOriginalFilename());

        List<RegisterTrackingRequest> trackings = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            boolean isHeader = true;

            while ((line = reader.readNext()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // Skip header
                }

                if (line.length >= 1 && !line[0].trim().isEmpty()) {
                    RegisterTrackingRequest request = RegisterTrackingRequest.builder()
                            .trackingNumber(line[0].trim())
                            .carrier(line.length >= 2 && !line[1].trim().isEmpty()
                                    ? Integer.parseInt(line[1].trim()) : null)
                            .description(line.length >= 3 ? line[2].trim() : null)
                            .build();
                    trackings.add(request);
                }
            }

            log.info("Parsed {} tracking numbers from CSV", trackings.size());

        } catch (Exception e) {
            log.error("Error reading CSV file", e);
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }

        return registerBatch(BatchRegisterRequest.builder().trackings(trackings).build());
    }

    // ============= Query Tracking Status =============

    @Transactional(readOnly = true)
    public TrackingResponse getTracking(Long id) {
        TrackingNumber tracking = trackingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tracking not found: " + id));

        return convertToResponse(tracking);
    }

    @Transactional(readOnly = true)
    public TrackingResponse getTrackingByNumber(String trackingNumber) {
        TrackingNumber tracking = trackingRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Tracking not found: " + trackingNumber));

        return convertToResponse(tracking);
    }

    @Transactional(readOnly = true)
    public List<TrackingResponse> getAllTrackings() {
        return trackingRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ============= Sync with 17-Track API =============

    @Transactional
    public void syncTracking(String trackingNumber) {
        log.info("Syncing tracking number: {}", trackingNumber);

        TrackingNumber tracking = trackingRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new RuntimeException("Tracking not found: " + trackingNumber));

        // Check rate limit
        if (!cacheService.checkRateLimit("query", queryDailyLimit, 86400)) {
            log.warn("Daily query limit exceeded, scheduling retry");
            scheduleRetry(tracking);
            return;
        }

        try {
            // Query API
            TrackApiResponse<Map<String, Object>> response =
                    apiClient.queryTracking(List.of(trackingNumber));

            if (response.getCode() != 0) {
                log.error("API error: {}", response.getMessage());
                scheduleRetry(tracking);
                return;
            }

            // Update tracking info
            updateTrackingFromApi(tracking, response.getData());
            tracking.setLastSyncedAt(LocalDateTime.now());
            tracking.setRetryCount(0);
            tracking.setNextRetryAt(null);

            trackingRepository.save(tracking);

            // Cache the status
            cacheService.cacheTrackingStatus(trackingNumber, tracking, statusCacheTtl);

            log.info("Successfully synced tracking: {}", trackingNumber);

        } catch (Exception e) {
            log.error("Error syncing tracking: {}", trackingNumber, e);
            scheduleRetry(tracking);
        }
    }

    // ============= Update from Webhook =============

    @Transactional
    public void updateFromWebhook(WebhookTrackData webhookData) {
        String trackingNumber = webhookData.getNumber();
        log.info("Updating tracking from webhook: {}", trackingNumber);

        TrackingNumber tracking = trackingRepository.findByTrackingNumber(trackingNumber)
                .orElse(null);

        if (tracking == null) {
            log.warn("Tracking number not found in database: {}", trackingNumber);
            return;
        }

        // Update tracking info
        TrackInfo trackInfo = webhookData.getTrack();
        if (trackInfo != null) {
            updateTrackingStatus(tracking, trackInfo);
            tracking.setLastSyncedAt(LocalDateTime.now());
            trackingRepository.save(tracking);

            // Save events
            if (trackInfo.getEvents() != null) {
                saveTrackingEvents(tracking, trackInfo.getEvents());
            }

            // Clear cache
            cacheService.clearTrackingStatus(trackingNumber);
        }
    }

    // ============= Helper Methods =============

    private void scheduleRetry(TrackingNumber tracking) {
        tracking.setRetryCount(tracking.getRetryCount() + 1);

        if (tracking.getRetryCount() >= maxRetryAttempts) {
            tracking.setStatus(TrackingStatus.ERROR);
            log.warn("Max retry attempts reached for: {}", tracking.getTrackingNumber());
        } else {
            // Exponential backoff: 1min, 2min, 4min, 8min, 16min
            long delayMinutes = (long) Math.pow(2, tracking.getRetryCount() - 1);
            tracking.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
            log.info("Scheduled retry {} for {} in {} minutes",
                    tracking.getRetryCount(), tracking.getTrackingNumber(), delayMinutes);
        }

        trackingRepository.save(tracking);
    }

    private void updateTrackingFromApi(TrackingNumber tracking, Map<String, Object> apiData) {
        if (apiData == null) return;

        // Extract accepted tracking data
        List<Map<String, Object>> accepted =
                (List<Map<String, Object>>) apiData.get("accepted");

        if (accepted != null && !accepted.isEmpty()) {
            Map<String, Object> trackData = accepted.get(0);
            Map<String, Object> track = (Map<String, Object>) trackData.get("track");

            if (track != null) {
                Integer status = (Integer) track.get("status");
                Integer substatus = (Integer) track.get("substatus");

                tracking.setStatus(mapStatusCode(status));
                tracking.setSubstatus(String.valueOf(substatus));
                tracking.setTrackInfo(track);

                // Save events
                List<Map<String, Object>> events =
                        (List<Map<String, Object>>) track.get("events");
                if (events != null && !events.isEmpty()) {
                    saveTrackingEventsFromMap(tracking, events);
                }
            }
        }
    }

    private void updateTrackingStatus(TrackingNumber tracking, TrackInfo trackInfo) {
        tracking.setStatus(mapStatusCode(trackInfo.getStatus()));
        tracking.setSubstatus(String.valueOf(trackInfo.getSubstatus()));
        tracking.setCarrierName(trackInfo.getProviderName());
    }

    private void saveTrackingEvents(TrackingNumber tracking, List<TrackEvent> events) {
        for (TrackEvent event : events) {
            TrackingEvent trackingEvent = TrackingEvent.builder()
                    .trackingId(tracking.getId())
                    .trackingNumber(tracking.getTrackingNumber())
                    .eventTime(parseEventTime(event.getTimeIso()))
                    .eventDescription(event.getDescription())
                    .eventLocation(event.getLocation())
                    .stage(event.getStage())
                    .substatus(String.valueOf(event.getSubstatus()))
                    .build();

            eventRepository.save(trackingEvent);
        }
    }

    private void saveTrackingEventsFromMap(TrackingNumber tracking,
                                           List<Map<String, Object>> events) {
        for (Map<String, Object> event : events) {
            TrackingEvent trackingEvent = TrackingEvent.builder()
                    .trackingId(tracking.getId())
                    .trackingNumber(tracking.getTrackingNumber())
                    .eventTime(parseEventTime((String) event.get("time_iso")))
                    .eventDescription((String) event.get("description"))
                    .eventLocation((String) event.get("location"))
                    .stage((String) event.get("stage"))
                    .eventData(event)
                    .build();

            eventRepository.save(trackingEvent);
        }
    }

    private TrackingResponse convertToResponse(TrackingNumber tracking) {
        List<TrackingEvent> events = eventRepository
                .findByTrackingIdOrderByEventTimeDesc(tracking.getId());

        return TrackingResponse.builder()
                .id(tracking.getId())
                .trackingNumber(tracking.getTrackingNumber())
                .carrierCode(tracking.getCarrierCode())
                .carrierName(tracking.getCarrierName())
                .description(tracking.getDescription())
                .status(tracking.getStatus().name())
                .substatus(tracking.getSubstatus())
                .createdAt(tracking.getCreatedAt())
                .updatedAt(tracking.getUpdatedAt())
                .lastSyncedAt(tracking.getLastSyncedAt())
                .trackInfo(tracking.getTrackInfo())
                .events(events.stream().map(this::convertEventToResponse).collect(Collectors.toList()))
                .build();
    }

    private TrackingEventResponse convertEventToResponse(TrackingEvent event) {
        return TrackingEventResponse.builder()
                .id(event.getId())
                .eventTime(event.getEventTime())
                .eventDescription(event.getEventDescription())
                .eventLocation(event.getEventLocation())
                .eventCode(event.getEventCode())
                .stage(event.getStage())
                .substatus(event.getSubstatus())
                .build();
    }

    private TrackingStatus mapStatusCode(Integer statusCode) {
        if (statusCode == null) return TrackingStatus.PENDING;

        return switch (statusCode) {
            case 0 -> TrackingStatus.PENDING;
            case 10 -> TrackingStatus.NOT_FOUND;
            case 20 -> TrackingStatus.IN_TRANSIT;
            case 30 -> TrackingStatus.PICK_UP;
            case 35 -> TrackingStatus.UNDELIVERED;
            case 40 -> TrackingStatus.DELIVERED;
            case 50 -> TrackingStatus.ALERT;
            case 60 -> TrackingStatus.EXPIRED;
            default -> TrackingStatus.PENDING;
        };
    }

    private LocalDateTime parseEventTime(String timeIso) {
        try {
            return LocalDateTime.parse(timeIso, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
