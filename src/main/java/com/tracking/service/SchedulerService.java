package com.tracking.service;

import com.tracking.entity.TrackingNumber;
import com.tracking.entity.TrackingStatus;
import com.tracking.repository.TrackingNumberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final TrackingNumberRepository trackingRepository;
    private final TrackingService trackingService;
    private final RedisCacheService cacheService;
    private final WebhookService webhookService;

    @Value("${tracking.retry.max-attempts}")
    private int maxRetryAttempts;

    @Value("${scheduling.enabled}")
    private boolean schedulingEnabled;

    // ============= Retry Failed Trackings =============
    // Runs every 5 minutes
    @Scheduled(fixedDelayString = "${tracking.retry.initial-delay:300000}")
    @Transactional
    public void retryFailedTrackings() {
        if (!schedulingEnabled) {
            return;
        }

        log.info("Running retry job for failed trackings");

        try {
            List<TrackingNumber> pendingRetries = trackingRepository
                    .findPendingRetries(LocalDateTime.now(), maxRetryAttempts);

            log.info("Found {} trackings pending retry", pendingRetries.size());

            for (TrackingNumber tracking : pendingRetries) {
                // Acquire lock to prevent concurrent retries
                if (!cacheService.acquireRetryLock(tracking.getTrackingNumber(), 300)) {
                    log.debug("Retry already in progress for: {}", tracking.getTrackingNumber());
                    continue;
                }

                try {
                    log.info("Retrying tracking: {} (attempt {}/{})",
                            tracking.getTrackingNumber(),
                            tracking.getRetryCount() + 1,
                            maxRetryAttempts);

                    trackingService.syncTracking(tracking.getTrackingNumber());

                } catch (Exception e) {
                    log.error("Error retrying tracking: {}", tracking.getTrackingNumber(), e);
                } finally {
                    cacheService.releaseRetryLock(tracking.getTrackingNumber());
                }

                // Add delay between retries to avoid rate limiting
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            log.info("Retry job completed");

        } catch (Exception e) {
            log.error("Error in retry job", e);
        }
    }

    // ============= Sync Active Trackings =============
    // Runs every 5 minutes
    @Scheduled(fixedDelayString = "${scheduling.sync-interval:300000}")
    @Transactional
    public void syncActiveTrackings() {
        if (!schedulingEnabled) {
            return;
        }

        log.info("Running sync job for active trackings");

        try {
            // Sync trackings that haven't been updated in the last 5 minutes
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
            List<TrackingNumber> needingSync = trackingRepository.findNeedingSync(threshold);

            log.info("Found {} trackings needing sync", needingSync.size());

            // Limit to avoid rate limits
            int maxSyncPerRun = 50;
            int synced = 0;

            for (TrackingNumber tracking : needingSync) {
                if (synced >= maxSyncPerRun) {
                    log.info("Reached max sync limit, will continue in next run");
                    break;
                }

                try {
                    trackingService.syncTracking(tracking.getTrackingNumber());
                    synced++;

                    // Add delay to respect rate limits
                    Thread.sleep(500);

                } catch (Exception e) {
                    log.error("Error syncing tracking: {}", tracking.getTrackingNumber(), e);
                }
            }

            log.info("Sync job completed, synced {} trackings", synced);

        } catch (Exception e) {
            log.error("Error in sync job", e);
        }
    }

    // ============= Cleanup Old Data =============
    // Runs daily at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldData() {
        if (!schedulingEnabled) {
            return;
        }

        log.info("Running cleanup job");

        try {
            // Cleanup webhook events older than 30 days
            webhookService.cleanupOldWebhookEvents(30);

            // Mark very old trackings as expired
            LocalDateTime expiredThreshold = LocalDateTime.now().minusDays(90);
            List<TrackingNumber> veryOldTrackings = trackingRepository
                    .findNeedingSync(expiredThreshold);

            int expiredCount = 0;
            for (TrackingNumber tracking : veryOldTrackings) {
                if (tracking.getStatus() != TrackingStatus.DELIVERED &&
                        tracking.getStatus() != TrackingStatus.EXPIRED) {
                    tracking.setStatus(TrackingStatus.EXPIRED);
                    trackingRepository.save(tracking);
                    expiredCount++;
                }
            }

            log.info("Cleanup completed, expired {} old trackings", expiredCount);

        } catch (Exception e) {
            log.error("Error in cleanup job", e);
        }
    }

    // ============= Rate Limit Statistics =============
    // Runs every hour
    @Scheduled(fixedDelay = 3600000) // 1 hour
    public void logRateLimitStatistics() {
        if (!schedulingEnabled) {
            return;
        }

        try {
            long registerCount = cacheService.getRateLimitCount("register");
            long queryCount = cacheService.getRateLimitCount("query");

            log.info("=== Rate Limit Statistics ===");
            log.info("Register operations today: {}", registerCount);
            log.info("Query operations today: {}", queryCount);
            log.info("============================");

        } catch (Exception e) {
            log.error("Error logging rate limit statistics", e);
        }
    }

    // ============= Health Check =============
    // Runs every 10 minutes
    @Scheduled(fixedDelay = 600000) // 10 minutes
    public void healthCheck() {
        if (!schedulingEnabled) {
            return;
        }

        try {
            // Check database connectivity
            long totalTrackings = trackingRepository.count();

            // Check active trackings
            List<TrackingStatus> activeStatuses = Arrays.asList(
                    TrackingStatus.PENDING,
                    TrackingStatus.IN_TRANSIT,
                    TrackingStatus.PICK_UP
            );
            List<TrackingNumber> activeTrackings =
                    trackingRepository.findByStatusIn(activeStatuses);

            // Check recent registrations
            LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
            long recentRegistrations = trackingRepository.countCreatedSince(last24Hours);

            log.debug("=== Health Check ===");
            log.debug("Total trackings: {}", totalTrackings);
            log.debug("Active trackings: {}", activeTrackings.size());
            log.debug("Registrations (24h): {}", recentRegistrations);
            log.debug("==================");

        } catch (Exception e) {
            log.error("Health check failed", e);
        }
    }
}