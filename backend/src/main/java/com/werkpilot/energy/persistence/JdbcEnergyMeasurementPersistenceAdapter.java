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
        jdbcTemplate.batchUpdate(
                """
                        insert into energy_measurement
                        (id, import_job_id, period_start, period_end, factory_id, line_id, machine_id, shift_id, energy_kwh)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                measurements,
                1000,
                (statement, measurement) -> {
                    statement.setObject(1, measurement.id());
                    statement.setObject(2, measurement.importJobId());
                    statement.setTimestamp(3, Timestamp.from(measurement.periodStart()));
                    statement.setTimestamp(4, Timestamp.from(measurement.periodEnd()));
                    statement.setObject(5, measurement.factoryId());
                    statement.setObject(6, measurement.lineId());
                    statement.setObject(7, measurement.machineId());
                    statement.setObject(8, measurement.shiftId());
                    statement.setBigDecimal(9, measurement.energyKwh());
                });
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
