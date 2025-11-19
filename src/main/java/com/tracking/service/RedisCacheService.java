package com.tracking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Cache Keys
    private static final String TOKEN_KEY = "track17:token";
    private static final String TRACKING_STATUS_PREFIX = "track17:status:";
    private static final String CARRIER_INFO_PREFIX = "track17:carrier:";
    private static final String RATE_LIMIT_PREFIX = "track17:ratelimit:";
    private static final String WEBHOOK_DEDUP_PREFIX = "track17:webhook:";
    private static final String RETRY_LOCK_PREFIX = "track17:retry:";

    // ============= Token Management =============

    public void cacheToken(String token, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(TOKEN_KEY, token, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached API token with TTL: {} seconds", ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to cache token", e);
        }
    }

    public Optional<String> getToken() {
        try {
            String token = redisTemplate.opsForValue().get(TOKEN_KEY);
            return Optional.ofNullable(token);
        } catch (Exception e) {
            log.error("Failed to get token from cache", e);
            return Optional.empty();
        }
    }

    public void clearToken() {
        try {
            redisTemplate.delete(TOKEN_KEY);
            log.debug("Cleared cached token");
        } catch (Exception e) {
            log.error("Failed to clear token", e);
        }
    }

    // ============= Tracking Status Cache =============

    public void cacheTrackingStatus(String trackingNumber, Object status, long ttlSeconds) {
        try {
            String key = TRACKING_STATUS_PREFIX + trackingNumber;
            String json = objectMapper.writeValueAsString(status);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached tracking status for: {}", trackingNumber);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize tracking status", e);
        }
    }

    public <T> Optional<T> getTrackingStatus(String trackingNumber, Class<T> clazz) {
        try {
            String key = TRACKING_STATUS_PREFIX + trackingNumber;
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, clazz));
            }
        } catch (Exception e) {
            log.error("Failed to get tracking status from cache", e);
        }
        return Optional.empty();
    }

    public void clearTrackingStatus(String trackingNumber) {
        String key = TRACKING_STATUS_PREFIX + trackingNumber;
        redisTemplate.delete(key);
    }

    // ============= Carrier Info Cache =============

    public void cacheCarrierInfo(Integer carrierCode, Object carrierInfo, long ttlSeconds) {
        try {
            String key = CARRIER_INFO_PREFIX + carrierCode;
            String json = objectMapper.writeValueAsString(carrierInfo);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            log.error("Failed to cache carrier info", e);
        }
    }

    public <T> Optional<T> getCarrierInfo(Integer carrierCode, Class<T> clazz) {
        try {
            String key = CARRIER_INFO_PREFIX + carrierCode;
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, clazz));
            }
        } catch (Exception e) {
            log.error("Failed to get carrier info from cache", e);
        }
        return Optional.empty();
    }

    // ============= Rate Limiting =============

    public boolean checkRateLimit(String operation, int maxRequests, long windowSeconds) {
        String key = RATE_LIMIT_PREFIX + operation + ":" + LocalDate.now();
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return false;
            }

            if (count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }

            if (count > maxRequests) {
                log.warn("Rate limit exceeded for {}: {}/{}", operation, count, maxRequests);
                return false;
            }

            log.debug("Rate limit check for {}: {}/{}", operation, count, maxRequests);
            return true;
        } catch (Exception e) {
            log.error("Rate limit check failed for {}", operation, e);
            return true; // Fail open
        }
    }

    public long getRateLimitCount(String operation) {
        String key = RATE_LIMIT_PREFIX + operation + ":" + LocalDate.now();
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0;
        } catch (Exception e) {
            log.error("Failed to get rate limit count", e);
            return 0;
        }
    }

    // ============= Webhook Deduplication =============

    public boolean isWebhookProcessed(String eventId) {
        String key = WEBHOOK_DEDUP_PREFIX + eventId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void markWebhookProcessed(String eventId, long ttlSeconds) {
        String key = WEBHOOK_DEDUP_PREFIX + eventId;
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(ttlSeconds));
    }

    // ============= Retry Lock =============

    public boolean acquireRetryLock(String trackingNumber, long lockTimeSeconds) {
        String key = RETRY_LOCK_PREFIX + trackingNumber;
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, LocalDateTime.now().toString(), Duration.ofSeconds(lockTimeSeconds));
        return Boolean.TRUE.equals(success);
    }

    public void releaseRetryLock(String trackingNumber) {
        String key = RETRY_LOCK_PREFIX + trackingNumber;
        redisTemplate.delete(key);
    }

    // ============= Utility Methods =============

    public void set(String key, String value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void expire(String key, long seconds) {
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }
}