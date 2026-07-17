package com.werkpilot.downtime.persistence;

import com.werkpilot.analytics.application.DowntimeKpiReadPort;
import com.werkpilot.analytics.application.DowntimeParetoPoint;
import com.werkpilot.analytics.application.DowntimeTotals;
import com.werkpilot.analytics.application.KpiQuery;
import com.werkpilot.downtime.application.port.DowntimeRecordDraft;
import com.werkpilot.downtime.application.port.DowntimeRecordPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcDowntimeRecordPersistenceAdapter implements DowntimeRecordPort, DowntimeKpiReadPort {

    private final JdbcTemplate jdbcTemplate;

    JdbcDowntimeRecordPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertAll(List<DowntimeRecordDraft> records) {
        jdbcTemplate.batchUpdate(
                """
                        insert into downtime_record
                        (id, import_job_id, period_start, period_end, machine_id, shift_id, downtime_min, reason_id, comment)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                records,
                1000,
                (statement, record) -> {
                    statement.setObject(1, record.id());
                    statement.setObject(2, record.importJobId());
                    statement.setTimestamp(3, Timestamp.from(record.periodStart()));
                    statement.setTimestamp(4, Timestamp.from(record.periodEnd()));
                    statement.setObject(5, record.machineId());
                    statement.setObject(6, record.shiftId());
                    statement.setInt(7, record.downtimeMin());
                    statement.setObject(8, record.reasonId());
                    statement.setString(9, record.comment());
                });
    }

    @Override
    public DowntimeTotals totals(KpiQuery query) {
        return jdbcTemplate.queryForObject(
                """
                        with matching as (
                            select dr.id, dr.machine_id, dr.shift_id, dr.period_start, dr.downtime_min, s.planned_minutes
                            from downtime_record dr
                            join import_job ij on ij.id = dr.import_job_id
                            join machine m on m.id = dr.machine_id
                            join shift s on s.id = dr.shift_id
                            %s
                        ),
                        planned_scope as (
                            select distinct machine_id, shift_id, period_start::date as production_day, planned_minutes
                            from matching
                        )
                        select coalesce((select sum(downtime_min) from matching), 0) as downtime_minutes,
                               coalesce((select sum(planned_minutes) from planned_scope), 0) as planned_minutes
                        """.formatted(whereClause("dr", "m")),
                (resultSet, rowNum) -> new DowntimeTotals(
                        resultSet.getLong("downtime_minutes"),
                        resultSet.getLong("planned_minutes")),
                parameters(query).toArray());
    }

    @Override
    public List<DowntimeParetoPoint> pareto(KpiQuery query) {
        return jdbcTemplate.query(
                """
                        with reason_totals as (
                            select dr.reason_id,
                                   r.name as reason_name,
                                   coalesce(sum(dr.downtime_min), 0) as downtime_minutes
                            from downtime_record dr
                            join import_job ij on ij.id = dr.import_job_id
                            join machine m on m.id = dr.machine_id
                            join downtime_reason r on r.id = dr.reason_id
                            %s
                            group by dr.reason_id, r.name
                        ),
                        ranked as (
                            select reason_id,
                                   reason_name,
                                   downtime_minutes,
                                   sum(downtime_minutes) over (
                                       order by downtime_minutes desc, reason_name asc, reason_id asc
                                       rows unbounded preceding
                                   ) as cumulative_minutes,
                                   sum(downtime_minutes) over () as total_minutes
                            from reason_totals
                        )
                        select reason_id,
                               reason_name,
                               downtime_minutes,
                               case
                                   when total_minutes = 0 then 0
                                   else round((cumulative_minutes * 100.0 / total_minutes)::numeric, 3)
                               end as cumulative_percentage
                        from ranked
                        order by downtime_minutes desc, reason_name asc, reason_id asc
                        """.formatted(whereClause("dr", "m")),
                this::mapParetoPoint,
                parameters(query).toArray());
    }

    private DowntimeParetoPoint mapParetoPoint(ResultSet resultSet, int rowNum) throws SQLException {
        return new DowntimeParetoPoint(
                resultSet.getObject("reason_id", UUID.class),
                resultSet.getString("reason_name"),
                resultSet.getLong("downtime_minutes"),
                resultSet.getBigDecimal("cumulative_percentage"));
    }

    private static String whereClause(String recordAlias, String machineAlias) {
        return """
                where ij.status = 'COMMITTED'
                  and %1$s.period_start >= ?
                  and %1$s.period_end <= ?
                  and (cast(? as uuid) is null or %2$s.factory_id = ?)
                  and (cast(? as uuid) is null or %2$s.line_id = ?)
                  and (cast(? as uuid) is null or %1$s.machine_id = ?)
                  and (cast(? as uuid) is null or %1$s.shift_id = ?)
                """.formatted(recordAlias, machineAlias);
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
