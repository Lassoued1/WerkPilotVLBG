package com.werkpilot.downtime.persistence;

import com.werkpilot.downtime.application.port.DowntimeRecordDraft;
import com.werkpilot.downtime.application.port.DowntimeRecordPort;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcDowntimeRecordPersistenceAdapter implements DowntimeRecordPort {

    private final JdbcTemplate jdbcTemplate;

    JdbcDowntimeRecordPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertAll(List<DowntimeRecordDraft> records) {
        for (DowntimeRecordDraft record : records) {
            jdbcTemplate.update(
                    """
                            insert into downtime_record
                            (id, import_job_id, period_start, period_end, machine_id, shift_id, downtime_min, reason_id, comment)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    record.id(),
                    record.importJobId(),
                    Timestamp.from(record.periodStart()),
                    Timestamp.from(record.periodEnd()),
                    record.machineId(),
                    record.shiftId(),
                    record.downtimeMin(),
                    record.reasonId(),
                    record.comment());
        }
    }
}
