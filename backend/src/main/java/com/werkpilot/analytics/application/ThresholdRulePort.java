package com.werkpilot.analytics.application;

import com.werkpilot.analytics.domain.ThresholdMetricKey;
import com.werkpilot.analytics.domain.ThresholdScopeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ThresholdRulePort {

    Page<ThresholdRule> list(
            ThresholdMetricKey metricKey,
            ThresholdScopeType scopeType,
            boolean includeInactive,
            Pageable pageable);

    Optional<ThresholdRule> findById(UUID id);

    Optional<ThresholdRule> findActiveByDefinition(ThresholdRuleDraft draft, UUID excludedId);

    List<ThresholdRule> findActiveFor(
            ThresholdMetricKey metricKey,
            UUID factoryId,
            UUID lineId,
            UUID machineId,
            UUID productId,
            UUID shiftId);

    ThresholdRule create(ThresholdRuleDraft draft);

    ThresholdRule update(UUID id, ThresholdRuleDraft draft);

    void setActive(UUID id, boolean active, UUID actorUserId);
}
