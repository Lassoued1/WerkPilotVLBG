package com.werkpilot.analytics.application;

import java.time.Instant;

public record AnalyticsImportWindow(Instant from, Instant to) {
}
