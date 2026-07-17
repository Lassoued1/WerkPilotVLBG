package com.werkpilot.energy.persistence;

import com.werkpilot.analytics.application.EnergyKpiReadPort;
import com.werkpilot.analytics.application.EnergyTopConsumer;
import com.werkpilot.analytics.application.EnergyTotals;
import com.werkpilot.analytics.application.KpiQuery;
import com.werkpilot.energy.application.port.EnergyMeasurementDraft;
import com.werkpilot.energy.application.port.EnergyMeasurementPort;
import java.util.ArrayList;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcEnergyMeasurementPersistenceAdapter implements EnergyMeasurementPort, EnergyKpiReadPort {

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

    @Override
    public EnergyTotals totals(KpiQuery query) {
        return jdbcTemplate.queryForObject(
                """
                        select coalesce(sum(em.energy_kwh), 0) as energy_kwh
                        from energy_measurement em
                        join import_job ij on ij.id = em.import_job_id
                        %s
                        """.formatted(whereClause("em")),
                (resultSet, rowNum) -> new EnergyTotals(resultSet.getBigDecimal("energy_kwh")),
                parameters(query).toArray());
    }

    @Override
    public List<EnergyTopConsumer> topConsumers(KpiQuery query, int limit) {
        List<Object> queryParameters = parameters(query);
        queryParameters.add(limit);
        return jdbcTemplate.query(
                """
                        select em.line_id, em.machine_id, coalesce(sum(em.energy_kwh), 0) as energy_kwh
                        from energy_measurement em
                        join import_job ij on ij.id = em.import_job_id
                        %s
                        group by em.line_id, em.machine_id
                        order by energy_kwh desc, em.line_id asc, em.machine_id asc
                        limit ?
                        """.formatted(whereClause("em")),
                (resultSet, rowNum) -> new EnergyTopConsumer(
                        resultSet.getObject("line_id", UUID.class),
                        resultSet.getObject("machine_id", UUID.class),
                        resultSet.getBigDecimal("energy_kwh")),
                queryParameters.toArray());
    }

    private static String whereClause(String alias) {
        return """
                where ij.status = 'COMMITTED'
                  and %1$s.period_start >= ?
                  and %1$s.period_end <= ?
                  and (cast(? as uuid) is null or %1$s.factory_id = ?)
                  and (cast(? as uuid) is null or %1$s.line_id = ?)
                  and (cast(? as uuid) is null or %1$s.machine_id = ?)
                  and (cast(? as uuid) is null or %1$s.shift_id = ?)
                """.formatted(alias);
    }

    private static List<Object> parameters(KpiQuery query) {
        List<Object> parameters = new ArrayList<>();
        parameters.add(Timestamp.from(query.from()));
        parameters.add(Timestamp.from(query.to()));
        addOptionalUuid(parameters, query.factoryId());
        addOptionalUuid(parameters, query.lineId());
        addOptionalUuid(parameters, query.machineId());
        addOptionalUuid(parameters, query.shiftId());
        return parameters;
    }

    private static void addOptionalUuid(List<Object> parameters, UUID value) {
        parameters.add(value);
        parameters.add(value);
    }
}
