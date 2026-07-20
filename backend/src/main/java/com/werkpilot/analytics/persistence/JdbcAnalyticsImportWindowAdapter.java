package com.werkpilot.analytics.persistence;

import com.werkpilot.analytics.application.AnalyticsImportWindow;
import com.werkpilot.analytics.application.AnalyticsImportWindowPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAnalyticsImportWindowAdapter implements AnalyticsImportWindowPort {

    private final JdbcTemplate jdbcTemplate;

    JdbcAnalyticsImportWindowAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AnalyticsImportWindow> findWindow(UUID importJobId, String importType) {
        String tableName = switch (importType) {
            case "PRODUCTION_RECORDS" -> "production_record";
            case "ENERGY_MEASUREMENTS" -> "energy_measurement";
            case "DOWNTIME_RECORDS" -> "downtime_record";
            case "SCRAP_RECORDS" -> "scrap_record";
            default -> throw new IllegalArgumentException("Unsupported import type: " + importType);
        };
        List<AnalyticsImportWindow> windows = jdbcTemplate.query(
                "select min(period_start) as from_ts, max(period_end) as to_ts from " + tableName + " where import_job_id = ?",
                this::map,
                importJobId);
        return windows.stream().filter(window -> window.from() != null && window.to() != null).findFirst();
    }

    private AnalyticsImportWindow map(ResultSet rs, int rowNum) throws SQLException {
        var from = rs.getTimestamp("from_ts");
        var to = rs.getTimestamp("to_ts");
        return new AnalyticsImportWindow(from == null ? null : from.toInstant(), to == null ? null : to.toInstant());
    }
}
