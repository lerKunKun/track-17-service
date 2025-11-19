package com.tracking.repository;


import com.tracking.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// ============= TrackingNumberRepository =============
@Repository
public interface TrackingNumberRepository extends JpaRepository<TrackingNumber, Long> {

    Optional<TrackingNumber> findByTrackingNumber(String trackingNumber);

    boolean existsByTrackingNumber(String trackingNumber);

    List<TrackingNumber> findByStatus(TrackingStatus status);

    List<TrackingNumber> findByCarrierCode(Integer carrierCode);

    @Query("SELECT t FROM TrackingNumber t WHERE t.status IN :statuses")
    List<TrackingNumber> findByStatusIn(@Param("statuses") List<TrackingStatus> statuses);

    @Query("SELECT t FROM TrackingNumber t WHERE t.nextRetryAt <= :now AND t.retryCount < :maxRetries")
    List<TrackingNumber> findPendingRetries(
            @Param("now") LocalDateTime now,
            @Param("maxRetries") Integer maxRetries
    );

    @Query("SELECT t FROM TrackingNumber t WHERE t.status NOT IN ('DELIVERED', 'EXPIRED') " +
            "AND (t.lastSyncedAt IS NULL OR t.lastSyncedAt < :threshold)")
    List<TrackingNumber> findNeedingSync(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(t) FROM TrackingNumber t WHERE t.createdAt > :since")
    Long countCreatedSince(@Param("since") LocalDateTime since);

    @Query("SELECT t FROM TrackingNumber t WHERE t.trackingNumber IN :trackingNumbers")
    List<TrackingNumber> findByTrackingNumberIn(@Param("trackingNumbers") List<String> trackingNumbers);
}