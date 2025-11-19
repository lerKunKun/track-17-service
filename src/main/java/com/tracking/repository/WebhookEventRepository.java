package com.tracking.repository;

import com.tracking.entity.WebhookEvent;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    Optional<WebhookEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    List<WebhookEvent> findByProcessed(Boolean processed);

    List<WebhookEvent> findByTrackingNumber(String trackingNumber);

    @Query("SELECT w FROM WebhookEvent w WHERE w.processed = false ORDER BY w.createdAt ASC")
    List<WebhookEvent> findUnprocessedEvents();

    @Query("SELECT w FROM WebhookEvent w WHERE w.createdAt < :threshold")
    List<WebhookEvent> findOlderThan(@Param("threshold") LocalDateTime threshold);

    void deleteByCreatedAtBefore(LocalDateTime threshold);
}
