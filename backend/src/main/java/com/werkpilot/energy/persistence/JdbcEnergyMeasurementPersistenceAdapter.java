package com.werkpilot.energy.persistence;

import com.werkpilot.energy.application.port.EnergyMeasurementDraft;
import com.werkpilot.energy.application.port.EnergyMeasurementPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcEnergyMeasurementPersistenceAdapter implements EnergyMeasurementPort {

    private final JdbcTemplate jdbcTemplate;

    JdbcEnergyMeasurementPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertAll(List<EnergyMeasurementDraft> measurements) {
        for (EnergyMeasurementDraft measurement : measurements) {
            jdbcTemplate.update(
                    """
                            insert into energy_measurement
                            (id, import_job_id, period_start, period_end, factory_id, line_id, machine_id, shift_id, energy_kwh)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    measurement.id(),
                    measurement.importJobId(),
                    Timestamp.from(measurement.periodStart()),
                    Timestamp.from(measurement.periodEnd()),
                    measurement.factoryId(),
                    measurement.lineId(),
                    measurement.machineId(),
                    measurement.shiftId(),
                    measurement.energyKwh());
        }
    }

    @Override
    public boolean existsCommittedOppositeGranularityOverlap(UUID lineId, Instant periodStart, Instant periodEnd, boolean machineLevel) {
        Long count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from energy_measurement em
                        join import_job ij on ij.id = em.import_job_id
                        where ij.status = 'COMMITTED'
                          and em.line_id = ?
                          and em.period_start < ?
                          and em.period_end > ?
                          and ((? = true and em.machine_id is null) or (? = false and em.machine_id is not null))
                        """,
                Long.class,
                lineId,
                Timestamp.from(periodEnd),
                Timestamp.from(periodStart),
                machineLevel,
                machineLevel);
        return count != null && count > 0;
    }
}
