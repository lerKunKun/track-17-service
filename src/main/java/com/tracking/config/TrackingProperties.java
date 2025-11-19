package com.tracking.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "tracking")
public class TrackingProperties {

    private Api api = new Api();
    private RateLimit rateLimit = new RateLimit();
    private Cache cache = new Cache();
    private Retry retry = new Retry();
    private Webhook webhook = new Webhook();

    @Data
    public static class Api {
        private String baseUrl;
        private String version;
        private String key;
        private int timeout;
        private int maxRetries;
    }

    @Data
    public static class RateLimit {
        private int registerPerDay;
        private int queryPerDay;
        private int windowSeconds;
    }

    @Data
    public static class Cache {
        private long tokenTtl;
        private long statusTtl;
        private long carrierTtl;
    }

    @Data
    public static class Retry {
        private int maxAttempts;
        private long initialDelay;
        private long maxDelay;
        private double multiplier;
    }

    @Data
    public static class Webhook {
        private String secret;
        private boolean enabled;
    }
}
