package com.werkpilot.analytics.persistence;

import com.werkpilot.analytics.application.AnomalyDetectionCandidate;
import com.werkpilot.analytics.application.AnomalyPort;
import com.werkpilot.analytics.application.AnomalyRecord;
import com.werkpilot.analytics.application.AnomalySearchCriteria;
import com.werkpilot.analytics.domain.AnomalyStatus;
import com.werkpilot.analytics.domain.AnomalyType;
import com.werkpilot.analytics.domain.BaselineQuality;
import com.werkpilot.analytics.domain.DetectionMethod;
import com.werkpilot.analytics.domain.ThresholdMetricKey;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAnomalyPersistenceAdapter implements AnomalyPort {

    private final JdbcTemplate jdbcTemplate;

    JdbcAnomalyPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Page<AnomalyRecord> search(AnomalySearchCriteria criteria, Pageable pageable) {
        QueryParts filters = filters(criteria);
        List<Object> itemArgs = new ArrayList<>(filters.args());
        itemArgs.add(pageable.getPageSize());
        itemArgs.add(pageable.getOffset());
        List<AnomalyRecord> items = jdbcTemplate.query(
                select() + filters.where() + " order by period_start desc, created_at desc limit ? offset ?",
                this::map,
                itemArgs.toArray());
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from anomaly" + filters.where(),
                Long.class,
                filters.args().toArray());
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }

    @Override
    public Optional<AnomalyRecord> findById(UUID id) {
        List<AnomalyRecord> rows = jdbcTemplate.query(select() + " where id = ?", this::map, id);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<AnomalyRecord> findActiveByIdentityKey(String identityKey) {
        List<AnomalyRecord> rows = jdbcTemplate.query(
                select() + " where identity_key = ? and status <> 'SUPERSEDED' order by updated_at desc limit 1",
                this::map,
                identityKey);
        return rows.stream().findFirst();
    }

    @Override
    public List<AnomalyRecord> activeInWindow(java.time.Instant from, java.time.Instant to) {
        return jdbcTemplate.query(
                select() + """
                         where status <> 'SUPERSEDED'
                           and period_start >= ?
                           and period_end <= ?
                        """,
                this::map,
                Timestamp.from(from),
                Timestamp.from(to));
    }

    @Override
    public AnomalyRecord create(AnomalyDetectionCandidate candidate, UUID previousAnomalyId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into anomaly (
                            id, identity_key, detector_version, metric_key, anomaly_type, severity, status,
                            detection_method, factory_id, line_id, machine_id, product_id, shift_id,
                            period_start, period_end, observed_value, baseline_average, baseline_stddev,
                            baseline_count, baseline_quality, z_score, threshold_rule_id, explanation,
                            fingerprint, previous_anomaly_id, created_at, updated_at
                        )
                        values (?, ?, ?, ?, ?, ?, 'NEW', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                        """,
                id,
                candidate.identityKey(),
                candidate.detectorVersion(),
                candidate.metricKey().name(),
                candidate.anomalyType().name(),
                candidate.severity().name(),
                candidate.detectionMethod().name(),
                candidate.factoryId(),
                candidate.lineId(),
                candidate.machineId(),
                candidate.productId(),
                candidate.shiftId(),
                Timestamp.from(candidate.periodStart()),
                Timestamp.from(candidate.periodEnd()),
                candidate.observedValue(),
                candidate.baselineAverage(),
                candidate.baselineStddev(),
                candidate.baselineCount(),
                candidate.baselineQuality().name(),
                candidate.zScore(),
                candidate.thresholdRuleId(),
                candidate.explanation(),
                candidate.fingerprint(),
                previousAnomalyId);
        return findById(id).orElseThrow();
    }

    @Override
    public void supersede(UUID anomalyId, UUID supersededByAnomalyId) {
        jdbcTemplate.update(
                """
                        update anomaly
                        set status = 'SUPERSEDED',
                            superseded_by_anomaly_id = ?,
                            updated_at = now()
                        where id = ?
                        """,
                supersededByAnomalyId,
                anomalyId);
    }

    @Override
    public AnomalyRecord updateStatus(UUID anomalyId, AnomalyStatus status) {
        jdbcTemplate.update("update anomaly set status = ?, updated_at = now() where id = ?", status.name(), anomalyId);
        return findById(anomalyId).orElseThrow();
    }

    private QueryParts filters(AnomalySearchCriteria criteria) {
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (!criteria.includeSuperseded()) {
            clauses.add("status <> 'SUPERSEDED'");
        }
        if (criteria.from() != null) {
            clauses.add("period_start >= ?");
            args.add(Timestamp.from(criteria.from()));
        }
        if (criteria.to() != null) {
            clauses.add("period_end <= ?");
            args.add(Timestamp.from(criteria.to()));
        }
        addUuidFilter(clauses, args, "factory_id", criteria.factoryId());
        addUuidFilter(clauses, args, "line_id", criteria.lineId());
        addUuidFilter(clauses, args, "machine_id", criteria.machineId());
        addUuidFilter(clauses, args, "product_id", criteria.productId());
        addUuidFilter(clauses, args, "shift_id", criteria.shiftId());
        if (criteria.anomalyType() != null) {
            clauses.add("anomaly_type = ?");
            args.add(criteria.anomalyType().name());
        }
        if (criteria.severity() != null) {
            clauses.add("severity = ?");
            args.add(criteria.severity().name());
        }
        if (criteria.anomalyStatus() != null) {
            clauses.add("status = ?");
            args.add(criteria.anomalyStatus().name());
        }
        return new QueryParts(clauses.isEmpty() ? "" : " where " + String.join(" and ", clauses), args);
    }

    private static void addUuidFilter(List<String> clauses, List<Object> args, String column, UUID value) {
        if (value != null) {
            clauses.add(column + " = ?");
            args.add(value);
        }
    }

    private AnomalyRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new AnomalyRecord(
                rs.getObject("id", UUID.class),
                rs.getString("identity_key"),
                rs.getString("detector_version"),
                ThresholdMetricKey.valueOf(rs.getString("metric_key")),
                AnomalyType.valueOf(rs.getString("anomaly_type")),
                ThresholdSeverity.valueOf(rs.getString("severity")),
                AnomalyStatus.valueOf(rs.getString("status")),
                DetectionMethod.valueOf(rs.getString("detection_method")),
                rs.getObject("factory_id", UUID.class),
                rs.getObject("line_id", UUID.class),
                rs.getObject("machine_id", UUID.class),
                rs.getObject("product_id", UUID.class),
                rs.getObject("shift_id", UUID.class),
                rs.getTimestamp("period_start").toInstant(),
                rs.getTimestamp("period_end").toInstant(),
                rs.getBigDecimal("observed_value"),
                rs.getBigDecimal("baseline_average"),
                rs.getBigDecimal("baseline_stddev"),
                rs.getInt("baseline_count"),
                BaselineQuality.valueOf(rs.getString("baseline_quality")),
                rs.getBigDecimal("z_score"),
                rs.getObject("threshold_rule_id", UUID.class),
                rs.getString("explanation"),
                rs.getString("fingerprint"),
                rs.getObject("previous_anomaly_id", UUID.class),
                rs.getObject("superseded_by_anomaly_id", UUID.class),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private static String select() {
        return """
                select id, identity_key, detector_version, metric_key, anomaly_type, severity, status,
                       detection_method, factory_id, line_id, machine_id, product_id, shift_id,
                       period_start, period_end, observed_value, baseline_average, baseline_stddev,
                       baseline_count, baseline_quality, z_score, threshold_rule_id, explanation,
                       fingerprint, previous_anomaly_id, superseded_by_anomaly_id, created_at, updated_at
                from anomaly
                """;
    }

    private record QueryParts(String where, List<Object> args) {
    }
}
