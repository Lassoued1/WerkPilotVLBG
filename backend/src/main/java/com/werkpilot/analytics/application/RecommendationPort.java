package com.werkpilot.analytics.application;

import java.util.List;
import java.util.UUID;

public interface RecommendationPort {

    void replaceForAnomaly(UUID anomalyId, List<RecommendationRecord> recommendations);

    List<RecommendationRecord> findByAnomalyId(UUID anomalyId);
}
