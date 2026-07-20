package com.werkpilot.shared.events;

import java.util.UUID;

public record ImportAnalyticsChangedEvent(UUID importJobId, String importType) {
}
