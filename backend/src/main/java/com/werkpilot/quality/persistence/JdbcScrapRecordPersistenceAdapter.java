package com.werkpilot.quality.persistence;

import com.werkpilot.analytics.application.KpiQuery;
import com.werkpilot.analytics.application.ScrapKpiReadPort;
import com.werkpilot.analytics.application.ScrapTotals;
import com.werkpilot.quality.application.port.ScrapRecordDraft;
import com.werkpilot.quality.application.port.ScrapRecordPort;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcScrapRecordPersistenceAdapter implements ScrapRecordPort, ScrapKpiReadPort {

    private final JdbcTemplate jdbcTemplate;

    JdbcScrapRecordPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertAll(List<ScrapRecordDraft> records) {
        jdbcTemplate.batchUpdate(
                """
                        insert into scrap_record
                        (id, import_job_id, period_start, period_end, machine_id, product_id, shift_id, scrap_count, scrap_category_id)
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
                    statement.setObject(6, record.productId());
                    statement.setObject(7, record.shiftId());
                    statement.setInt(8, record.scrapCount());
                    statement.setObject(9, record.scrapCategoryId());
                });
    }

    @Override
    public ScrapTotals totals(KpiQuery query) {
        return jdbcTemplate.queryForObject(
                """
                        select coalesce(sum(sr.scrap_count), 0) as scrap_count
                        from scrap_record sr
                        join import_job ij on ij.id = sr.import_job_id
                        join machine m on m.id = sr.machine_id
                        %s
                        """.formatted(whereClause("sr", "m")),
                (resultSet, rowNum) -> new ScrapTotals(resultSet.getLong("scrap_count")),
                parameters(query).toArray());
    }

    private static String whereClause(String recordAlias, String machineAlias) {
        return """
                where ij.status = 'COMMITTED'
                  and %1$s.period_start >= ?
                  and %1$s.period_end <= ?
                  and (cast(? as uuid) is null or %2$s.factory_id = ?)
                  and (cast(? as uuid) is null or %2$s.line_id = ?)
                  and (cast(? as uuid) is null or %1$s.machine_id = ?)
                  and (cast(? as uuid) is null or %1$s.product_id = ?)
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
        addOptionalUuid(parameters, query.productId());
        addOptionalUuid(parameters, query.shiftId());
        return parameters;
    }

    private static void addOptionalUuid(List<Object> parameters, UUID value) {
        parameters.add(value);
        parameters.add(value);
    }
}
