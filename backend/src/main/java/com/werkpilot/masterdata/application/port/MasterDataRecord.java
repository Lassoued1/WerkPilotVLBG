package com.werkpilot.masterdata.application.port;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record MasterDataRecord(
        UUID id,
        String code,
        String name,
        boolean active,
        UUID factoryId,
        UUID lineId,
        String family,
        LocalTime startTime,
        LocalTime endTime,
        Integer plannedMinutes,
        Instant createdAt,
        Instant updatedAt) {
}
