package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.AnomalyStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AnomalyPort {

    Page<AnomalyRecord> search(AnomalySearchCriteria criteria, Pageable pageable);

    Optional<AnomalyRecord> findById(UUID id);

    Optional<AnomalyRecord> findActiveByIdentityKey(String identityKey);

    List<AnomalyRecord> activeInWindow(Instant from, Instant to);

    AnomalyRecord create(AnomalyDetectionCandidate candidate, UUID previousAnomalyId);

    void supersede(UUID anomalyId, UUID supersededByAnomalyId);

    AnomalyRecord updateStatus(UUID anomalyId, AnomalyStatus status);
}
