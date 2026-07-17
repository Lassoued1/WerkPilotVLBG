package com.werkpilot.analytics.application;

import java.time.Instant;

public record ProductionTrendPoint(Instant bucketStart, long unitsProduced) {
}
