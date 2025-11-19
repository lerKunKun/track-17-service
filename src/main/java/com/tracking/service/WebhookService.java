package com.tracking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracking.client.SeventeenTrackClient;
import com.tracking.dto.*;
import com.tracking.entity.Carrier;
import com.tracking.entity.WebhookEvent;
import com.tracking.repository.CarrierRepository;
import com.tracking.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// ============= WebhookService =============

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final WebhookEventRepository webhookRepository;
    private final TrackingService trackingService;
    private final RedisCacheService cacheService;
    private final ObjectMapper objectMapper;

    @Value("${tracking.webhook.secret}")
    private String webhookSecret;

    @Value("${tracking.webhook.enabled}")
    private boolean webhookEnabled;

    @Transactional
    public void processWebhook(WebhookPayload payload) {
        if (!webhookEnabled) {
            log.warn("Webhook processing is disabled");
            return;
        }

        log.info("Processing webhook event: {}", payload.getEvent());

        if (payload.getAccepted() == null || payload.getAccepted().isEmpty()) {
            log.warn("No tracking data in webhook payload");
            return;
        }

        for (WebhookTrackData trackData : payload.getAccepted()) {
            String trackingNumber = trackData.getNumber();
            String eventId = generateEventId(trackingNumber, payload.getEvent());

            // Check for duplicate
            if (webhookRepository.existsByEventId(eventId)) {
                log.info("Webhook event already processed: {}", eventId);
                continue;
            }

            // Check Redis for deduplication
            if (cacheService.isWebhookProcessed(eventId)) {
                log.info("Webhook event cached as processed: {}", eventId);
                continue;
            }

            try {
                // Save webhook event
                WebhookEvent webhookEvent = WebhookEvent.builder()
                        .eventId(eventId)
                        .eventType(payload.getEvent())
                        .trackingNumber(trackingNumber)
                        .payload(convertToMap(trackData))
                        .processed(false)
                        .build();

                webhookRepository.save(webhookEvent);

                // Process the tracking update
                trackingService.updateFromWebhook(trackData);

                // Mark as processed
                webhookEvent.setProcessed(true);
                webhookEvent.setProcessedAt(LocalDateTime.now());
                webhookRepository.save(webhookEvent);

                // Cache in Redis for deduplication
                cacheService.markWebhookProcessed(eventId, 86400); // 24 hours

                log.info("Successfully processed webhook for: {}", trackingNumber);

            } catch (Exception e) {
                log.error("Error processing webhook for: {}", trackingNumber, e);
            }
        }
    }

    public boolean validateSignature(WebhookPayload payload, String signature) {
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            log.warn("Webhook secret not configured, skipping signature validation");
            return true;
        }

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String computedSignature = computeHmacSha256(payloadJson, webhookSecret);
            return signature.equals(computedSignature);
        } catch (Exception e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    @Transactional
    public void cleanupOldWebhookEvents(int daysToKeep) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysToKeep);
        webhookRepository.deleteByCreatedAtBefore(threshold);
        log.info("Cleaned up webhook events older than {} days", daysToKeep);
    }

    private String generateEventId(String trackingNumber, String eventType) {
        return String.format("%s_%s_%d", trackingNumber, eventType, System.currentTimeMillis());
    }

    private String computeHmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    private Map<String, Object> convertToMap(Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Error converting object to map", e);
            return new HashMap<>();
        }
    }
}
