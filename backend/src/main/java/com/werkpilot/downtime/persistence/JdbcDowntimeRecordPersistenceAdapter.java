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
}
