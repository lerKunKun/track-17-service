package com.tracking.controller;

import com.tracking.dto.WebhookPayload;
import com.tracking.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController{

    private final WebhookService webhookService;

    @PostMapping("/17track")
    public ResponseEntity<String> handleWebhook(
            @RequestBody WebhookPayload payload,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        try {
            log.info("Webhook received: event={}", payload.getEvent());

            // Validate signature if configured
            if (signature != null && !webhookService.validateSignature(payload, signature)) {
                log.warn("Invalid webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid signature");
            }

            webhookService.processWebhook(payload);
            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> webhookHealth() {
        return ResponseEntity.ok("Webhook endpoint is healthy");
    }
}