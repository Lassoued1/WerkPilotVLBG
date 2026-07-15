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
        for (ScrapRecordDraft record : records) {
            jdbcTemplate.update(
                    """
                            insert into scrap_record
                            (id, import_job_id, period_start, period_end, machine_id, product_id, shift_id, scrap_count, scrap_category_id)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    record.id(),
                    record.importJobId(),
                    Timestamp.from(record.periodStart()),
                    Timestamp.from(record.periodEnd()),
                    record.machineId(),
                    record.productId(),
                    record.shiftId(),
                    record.scrapCount(),
                    record.scrapCategoryId());
        }
    }
}
