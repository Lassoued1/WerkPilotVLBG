package com.werkpilot.energy.application.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EnergyMeasurementPort {

    void insertAll(List<EnergyMeasurementDraft> measurements);

    boolean existsCommittedOppositeGranularityOverlap(UUID lineId, Instant periodStart, Instant periodEnd, boolean machineLevel);
}
