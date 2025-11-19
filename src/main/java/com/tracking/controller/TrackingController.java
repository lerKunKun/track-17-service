package com.tracking.controller;

import com.tracking.dto.*;
import com.tracking.service.TrackingService;
import com.tracking.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// ============= TrackingController =============

@Slf4j
@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TrackingResponse>> registerTracking(
            @Valid @RequestBody RegisterTrackingRequest request) {
        try {
            log.info("REST API: Register tracking - {}", request.getTrackingNumber());
            TrackingResponse response = trackingService.registerTracking(request);
            return ResponseEntity.ok(ApiResponse.success("Tracking registered successfully", response));
        } catch (Exception e) {
            log.error("Error registering tracking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<BatchRegisterResponse>> registerBatch(
            @Valid @RequestBody BatchRegisterRequest request) {
        try {
            log.info("REST API: Register batch - {} items", request.getTrackings().size());
            BatchRegisterResponse response = trackingService.registerBatch(request);
            return ResponseEntity.ok(ApiResponse.success("Batch registration completed", response));
        } catch (Exception e) {
            log.error("Error in batch registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/import-csv")
    public ResponseEntity<ApiResponse<BatchRegisterResponse>> importCsv(
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("REST API: Import CSV - {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File is empty"));
            }

            if (!file.getOriginalFilename().endsWith(".csv")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Only CSV files are supported"));
            }

            BatchRegisterResponse response = trackingService.importFromCsv(file);
            return ResponseEntity.ok(ApiResponse.success("CSV imported successfully", response));
        } catch (Exception e) {
            log.error("Error importing CSV", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrackingResponse>> getTracking(@PathVariable Long id) {
        try {
            log.info("REST API: Get tracking by ID - {}", id);
            TrackingResponse response = trackingService.getTracking(id);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting tracking by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/number/{trackingNumber}")
    public ResponseEntity<ApiResponse<TrackingResponse>> getTrackingByNumber(
            @PathVariable String trackingNumber) {
        try {
            log.info("REST API: Get tracking by number - {}", trackingNumber);
            TrackingResponse response = trackingService.getTrackingByNumber(trackingNumber);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting tracking by number: {}", trackingNumber, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<TrackingResponse>>> getAllTrackings() {
        try {
            log.info("REST API: Get all trackings");
            List<TrackingResponse> response = trackingService.getAllTrackings();
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting all trackings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<ApiResponse<String>> syncTracking(@PathVariable Long id) {
        try {
            log.info("REST API: Sync tracking - {}", id);
            TrackingResponse tracking = trackingService.getTracking(id);
            trackingService.syncTracking(tracking.getTrackingNumber());
            return ResponseEntity.ok(ApiResponse.success("Sync initiated", null));
        } catch (Exception e) {
            log.error("Error syncing tracking: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}