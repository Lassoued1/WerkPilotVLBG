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
        for (ProductionRecordDraft record : records) {
            jdbcTemplate.update(
                    """
                            insert into production_record
                            (id, import_job_id, period_start, period_end, factory_id, line_id, machine_id, product_id,
                             shift_id, units_produced, batch_code)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    record.id(),
                    record.importJobId(),
                    Timestamp.from(record.periodStart()),
                    Timestamp.from(record.periodEnd()),
                    record.factoryId(),
                    record.lineId(),
                    record.machineId(),
                    record.productId(),
                    record.shiftId(),
                    record.unitsProduced(),
                    record.batchCode());
        }
    }
}