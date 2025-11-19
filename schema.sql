-- Database Creation
CREATE DATABASE IF NOT EXISTS tracking_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tracking_db;

-- Tracking Numbers Table
CREATE TABLE tracking_numbers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tracking_number VARCHAR(100) NOT NULL,
    carrier_code INT,
    carrier_name VARCHAR(100),
    description VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    substatus VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_synced_at TIMESTAMP NULL,
    retry_count INT DEFAULT 0,
    next_retry_at TIMESTAMP NULL,
    track_info JSON,
    UNIQUE KEY uk_tracking_number (tracking_number),
    INDEX idx_status (status),
    INDEX idx_carrier (carrier_code),
    INDEX idx_created_at (created_at),
    INDEX idx_next_retry (next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tracking Events Table (History)
CREATE TABLE tracking_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tracking_id BIGINT NOT NULL,
    tracking_number VARCHAR(100) NOT NULL,
    event_time TIMESTAMP,
    event_description TEXT,
    event_location VARCHAR(255),
    event_code VARCHAR(50),
    stage VARCHAR(50),
    substatus VARCHAR(50),
    event_data JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tracking_id) REFERENCES tracking_numbers(id) ON DELETE CASCADE,
    INDEX idx_tracking_id (tracking_id),
    INDEX idx_tracking_number (tracking_number),
    INDEX idx_event_time (event_time),
    INDEX idx_stage (stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Carrier Information Cache Table
CREATE TABLE carriers (
    carrier_code INT PRIMARY KEY,
    carrier_name VARCHAR(100) NOT NULL,
    carrier_key VARCHAR(50),
    country VARCHAR(5),
    phone VARCHAR(50),
    website VARCHAR(255),
    logo_url VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_carrier_key (carrier_key),
    INDEX idx_country (country)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Webhook Events Table (for deduplication and audit)
CREATE TABLE webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) UNIQUE,
    event_type VARCHAR(50),
    tracking_number VARCHAR(100),
    payload JSON,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_event_id (event_id),
    INDEX idx_tracking_number (tracking_number),
    INDEX idx_processed (processed),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- API Rate Limit Tracking
CREATE TABLE rate_limit_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_endpoint VARCHAR(100) NOT NULL,
    request_count INT DEFAULT 0,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_endpoint_window (api_endpoint, window_start),
    INDEX idx_window_end (window_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sample Carriers Data (Common ones from 17-Track)
INSERT INTO carriers (carrier_code, carrier_name, carrier_key, country) VALUES
(101, 'USPS', 'usps', 'US'),
(102, 'FedEx', 'fedex', 'US'),
(103, 'UPS', 'ups', 'US'),
(104, 'DHL', 'dhl', 'DE'),
(105, 'China Post', 'china-post', 'CN'),
(106, 'DHL eCommerce', 'dhl-global-mail', 'US'),
(107, 'SF Express', 'sf-express', 'CN'),
(108, 'YunExpress', 'yun-express', 'CN'),
(2001, 'Canada Post', 'canada-post', 'CA'),
(2002, 'Royal Mail', 'royal-mail', 'GB'),
(2003, 'Australia Post', 'australia-post', 'AU')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;