package com.werkpilot.production.persistence;

import com.werkpilot.production.application.port.ProductionRecordDraft;
import com.werkpilot.production.application.port.ProductionRecordPort;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcProductionRecordPersistenceAdapter implements ProductionRecordPort {

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
}