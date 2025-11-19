package com.tracking.repository;

import com.tracking.entity.TrackingEvent;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {

    List<TrackingEvent> findByTrackingId(Long trackingId);

    List<TrackingEvent> findByTrackingNumber(String trackingNumber);

    @Query("SELECT e FROM TrackingEvent e WHERE e.trackingId = :trackingId ORDER BY e.eventTime DESC")
    List<TrackingEvent> findByTrackingIdOrderByEventTimeDesc(@Param("trackingId") Long trackingId);

    @Query("SELECT e FROM TrackingEvent e WHERE e.trackingNumber = :trackingNumber ORDER BY e.eventTime DESC")
    List<TrackingEvent> findByTrackingNumberOrderByEventTimeDesc(@Param("trackingNumber") String trackingNumber);

    @Query("SELECT COUNT(e) FROM TrackingEvent e WHERE e.trackingId = :trackingId")
    Long countByTrackingId(@Param("trackingId") Long trackingId);

    void deleteByTrackingId(Long trackingId);
}
