package com.werkpilot.production.persistence;

import com.werkpilot.analytics.application.KpiQuery;
import com.werkpilot.analytics.application.ProductionKpiReadPort;
import com.werkpilot.analytics.application.ProductionRecordView;
import com.werkpilot.analytics.application.ProductionTotals;
import com.werkpilot.analytics.application.ProductionTrendPoint;
import com.werkpilot.production.application.port.ProductionRecordDraft;
import com.werkpilot.production.application.port.ProductionRecordPort;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcProductionRecordPersistenceAdapter implements ProductionRecordPort, ProductionKpiReadPort {

    private final JdbcTemplate jdbcTemplate;

    JdbcProductionRecordPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertAll(List<ProductionRecordDraft> records) {
        jdbcTemplate.batchUpdate(
                """
                        insert into production_record
                        (id, import_job_id, period_start, period_end, factory_id, line_id, machine_id, product_id,
                         shift_id, units_produced, batch_code)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                records,
                1000,
                (statement, record) -> {
                    statement.setObject(1, record.id());
                    statement.setObject(2, record.importJobId());
                    statement.setTimestamp(3, Timestamp.from(record.periodStart()));
                    statement.setTimestamp(4, Timestamp.from(record.periodEnd()));
                    statement.setObject(5, record.factoryId());
                    statement.setObject(6, record.lineId());
                    statement.setObject(7, record.machineId());
                    statement.setObject(8, record.productId());
                    statement.setObject(9, record.shiftId());
                    statement.setInt(10, record.unitsProduced());
                    statement.setString(11, record.batchCode());
                });
    }

    @Override
    public ProductionTotals totals(KpiQuery query) {
        return jdbcTemplate.queryForObject(
                """
                        select coalesce(sum(pr.units_produced), 0) as units_produced,
                               coalesce(sum(extract(epoch from (pr.period_end - pr.period_start)) / 3600.0), 0) as production_hours
                        from production_record pr
                        join import_job ij on ij.id = pr.import_job_id
                        %s
                        """.formatted(whereClause("pr")),
                (resultSet, rowNum) -> new ProductionTotals(
                        resultSet.getLong("units_produced"),
                        resultSet.getBigDecimal("production_hours")),
                parameters(query).toArray());
    }

    @Override
    public Page<ProductionRecordView> listRecords(KpiQuery query, Pageable pageable) {
        List<Object> listParameters = parameters(query);
        listParameters.add(pageable.getPageSize());
        listParameters.add(pageable.getOffset());
        List<ProductionRecordView> items = jdbcTemplate.query(
                """
                        select pr.id, pr.period_start, pr.period_end, pr.factory_id, pr.line_id, pr.machine_id,
                               pr.product_id, pr.shift_id, pr.units_produced, pr.batch_code, pr.import_job_id
                        from production_record pr
                        join import_job ij on ij.id = pr.import_job_id
                        %s
                        order by pr.period_start asc, pr.id asc
                        limit ? offset ?
                        """.formatted(whereClause("pr")),
                this::mapRecord,
                listParameters.toArray());

        Long total = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from production_record pr
                        join import_job ij on ij.id = pr.import_job_id
                        %s
                        """.formatted(whereClause("pr")),
                Long.class,
                parameters(query).toArray());
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }

    @Override
    public List<ProductionTrendPoint> hourlyTrend(KpiQuery query) {
        return jdbcTemplate.query(
                """
                        select date_trunc('hour', pr.period_start) as bucket_start,
                               coalesce(sum(pr.units_produced), 0) as units_produced
                        from production_record pr
                        join import_job ij on ij.id = pr.import_job_id
                        %s
                        group by bucket_start
                        order by bucket_start asc
                        """.formatted(whereClause("pr")),
                (resultSet, rowNum) -> new ProductionTrendPoint(
                        resultSet.getTimestamp("bucket_start").toInstant(),
                        resultSet.getLong("units_produced")),
                parameters(query).toArray());
    }

    @Override
    public List<ProductionRecordView> evidenceRows(KpiQuery query) {
        return jdbcTemplate.query(
                """
                        select pr.id, pr.period_start, pr.period_end, pr.factory_id, pr.line_id, pr.machine_id,
                               pr.product_id, pr.shift_id, pr.units_produced, pr.batch_code, pr.import_job_id
                        from production_record pr
                        join import_job ij on ij.id = pr.import_job_id
                        %s
                        order by pr.period_start asc, pr.id asc
                        """.formatted(whereClause("pr")),
                this::mapRecord,
                parameters(query).toArray());
    }

    private ProductionRecordView mapRecord(ResultSet resultSet, int rowNum) throws SQLException {
        return new ProductionRecordView(
                resultSet.getObject("id", UUID.class),
                timestampToInstant(resultSet, "period_start"),
                timestampToInstant(resultSet, "period_end"),
                resultSet.getObject("factory_id", UUID.class),
                resultSet.getObject("line_id", UUID.class),
                resultSet.getObject("machine_id", UUID.class),
                resultSet.getObject("product_id", UUID.class),
                resultSet.getObject("shift_id", UUID.class),
                resultSet.getInt("units_produced"),
                resultSet.getString("batch_code"),
                resultSet.getObject("import_job_id", UUID.class));
    }

    private static Instant timestampToInstant(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getTimestamp(column).toInstant();
    }

    private static String whereClause(String alias) {
        return """
                where ij.status = 'COMMITTED'
                  and %1$s.period_start >= ?
                  and %1$s.period_end <= ?
                  and (cast(? as uuid) is null or %1$s.factory_id = ?)
                  and (cast(? as uuid) is null or %1$s.line_id = ?)
                  and (cast(? as uuid) is null or %1$s.machine_id = ?)
                  and (cast(? as uuid) is null or %1$s.product_id = ?)
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
        addOptionalUuid(parameters, query.productId());
        addOptionalUuid(parameters, query.shiftId());
        return parameters;
    }

    private static void addOptionalUuid(List<Object> parameters, UUID value) {
        parameters.add(value);
        parameters.add(value);
    }
}
