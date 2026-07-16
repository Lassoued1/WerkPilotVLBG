package com.werkpilot.quality.persistence;

import com.werkpilot.quality.application.port.ScrapRecordDraft;
import com.werkpilot.quality.application.port.ScrapRecordPort;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcScrapRecordPersistenceAdapter implements ScrapRecordPort {

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
}
