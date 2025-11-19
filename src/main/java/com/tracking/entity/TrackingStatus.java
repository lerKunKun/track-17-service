package com.tracking.entity;

public enum TrackingStatus {
    PENDING,           // Registered, not yet tracked
    NOT_FOUND,         // Not found by carrier
    IN_TRANSIT,        // In transit
    EXPIRED,           // Tracking expired
    PICK_UP,           // Ready for pickup
    UNDELIVERED,       // Delivery attempt failed
    DELIVERED,         // Successfully delivered
    ALERT,             // Exception/Alert
    ERROR              // System error
}
