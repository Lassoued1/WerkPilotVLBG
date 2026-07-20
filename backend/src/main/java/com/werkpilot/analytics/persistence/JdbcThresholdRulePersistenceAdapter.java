package com.werkpilot.analytics.persistence;

import com.werkpilot.analytics.application.ThresholdRule;
import com.werkpilot.analytics.application.ThresholdRuleDraft;
import com.werkpilot.analytics.application.ThresholdRulePort;
import com.werkpilot.analytics.domain.ThresholdMetricKey;
import com.werkpilot.analytics.domain.ThresholdScopeType;
import com.werkpilot.analytics.domain.ThresholdSeverity;
import java.sql.ResultSet;
import java.sql.SQLException;
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
class JdbcThresholdRulePersistenceAdapter implements ThresholdRulePort {

    private final JdbcTemplate jdbcTemplate;

    JdbcThresholdRulePersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Page<ThresholdRule> list(
            ThresholdMetricKey metricKey,
            ThresholdScopeType scopeType,
            boolean includeInactive,
            Pageable pageable) {
        QueryParts queryParts = filters(metricKey, scopeType, includeInactive);
        List<Object> itemArgs = new ArrayList<>(queryParts.args());
        itemArgs.add(pageable.getPageSize());
        itemArgs.add(pageable.getOffset());
        List<ThresholdRule> items = jdbcTemplate.query(
                select() + queryParts.where() + " order by updated_at desc, created_at desc limit ? offset ?",
                this::map,
                itemArgs.toArray());
        Long total = jdbcTemplate.queryForObject(
                "select count(*) from threshold_rule" + queryParts.where(),
                Long.class,
                queryParts.args().toArray());
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }

    @Override
    public Optional<ThresholdRule> findById(UUID id) {
        List<ThresholdRule> rows = jdbcTemplate.query(select() + " where id = ?", this::map, id);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<ThresholdRule> findActiveByDefinition(ThresholdRuleDraft draft, UUID excludedId) {
        List<Object> args = new ArrayList<>();
        args.add(draft.metricKey().name());
        args.add(draft.scopeType().name());
        args.add(draft.scopeId());
        args.add(draft.severity().name());
        String excludedFilter = "";
        if (excludedId != null) {
            excludedFilter = " and id <> ?";
            args.add(excludedId);
        }
        List<ThresholdRule> rows = jdbcTemplate.query(
                select() + """
                         where active = true
                           and metric_key = ?
                           and scope_type = ?
                           and scope_id is not distinct from ?
                           and severity = ?
                        """ + excludedFilter,
                this::map,
                args.toArray());
        return rows.stream().findFirst();
    }

    @Override
    public List<ThresholdRule> findActiveFor(
            ThresholdMetricKey metricKey,
            UUID factoryId,
            UUID lineId,
            UUID machineId,
            UUID productId,
            UUID shiftId) {
        return jdbcTemplate.query(
                select() + """
                         where active = true
                           and metric_key = ?
                           and (
                               (scope_type = 'GLOBAL' and scope_id is null)
                               or (scope_type = 'FACTORY' and scope_id is not distinct from ?)
                               or (scope_type = 'LINE' and scope_id is not distinct from ?)
                               or (scope_type = 'MACHINE' and scope_id is not distinct from ?)
                               or (scope_type = 'PRODUCT' and scope_id is not distinct from ?)
                               or (scope_type = 'SHIFT' and scope_id is not distinct from ?)
                           )
                        """,
                this::map,
                metricKey.name(),
                factoryId,
                lineId,
                machineId,
                productId,
                shiftId);
    }

    @Override
    public ThresholdRule create(ThresholdRuleDraft draft) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into threshold_rule (
                            id, metric_key, scope_type, scope_id, min_value, max_value,
                            severity, active, created_by_user_id, updated_by_user_id, created_at, updated_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                        """,
                id,
                draft.metricKey().name(),
                draft.scopeType().name(),
                draft.scopeId(),
                draft.minValue(),
                draft.maxValue(),
                draft.severity().name(),
                draft.active(),
                draft.actorUserId(),
                draft.actorUserId());
        return findById(id).orElseThrow();
    }

    @Override
    public ThresholdRule update(UUID id, ThresholdRuleDraft draft) {
        jdbcTemplate.update(
                """
                        update threshold_rule
                        set metric_key = ?,
                            scope_type = ?,
                            scope_id = ?,
                            min_value = ?,
                            max_value = ?,
                            severity = ?,
                            active = ?,
                            updated_by_user_id = ?,
                            updated_at = now()
                        where id = ?
                        """,
                draft.metricKey().name(),
                draft.scopeType().name(),
                draft.scopeId(),
                draft.minValue(),
                draft.maxValue(),
                draft.severity().name(),
                draft.active(),
                draft.actorUserId(),
                id);
        return findById(id).orElseThrow();
    }

    @Override
    public void setActive(UUID id, boolean active, UUID actorUserId) {
        jdbcTemplate.update(
                "update threshold_rule set active = ?, updated_by_user_id = ?, updated_at = now() where id = ?",
                active,
                actorUserId,
                id);
    }

    private QueryParts filters(ThresholdMetricKey metricKey, ThresholdScopeType scopeType, boolean includeInactive) {
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (!includeInactive) {
            clauses.add("active = true");
        }
        if (metricKey != null) {
            clauses.add("metric_key = ?");
            args.add(metricKey.name());
        }
        if (scopeType != null) {
            clauses.add("scope_type = ?");
            args.add(scopeType.name());
        }
        String where = clauses.isEmpty() ? "" : " where " + String.join(" and ", clauses);
        return new QueryParts(where, args);
    }

    private ThresholdRule map(ResultSet resultSet, int rowNum) throws SQLException {
        return new ThresholdRule(
                resultSet.getObject("id", UUID.class),
                ThresholdMetricKey.valueOf(resultSet.getString("metric_key")),
                ThresholdScopeType.valueOf(resultSet.getString("scope_type")),
                resultSet.getObject("scope_id", UUID.class),
                resultSet.getBigDecimal("min_value"),
                resultSet.getBigDecimal("max_value"),
                ThresholdSeverity.valueOf(resultSet.getString("severity")),
                resultSet.getBoolean("active"),
                resultSet.getObject("created_by_user_id", UUID.class),
                resultSet.getObject("updated_by_user_id", UUID.class),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }

    private static String select() {
        return """
                select id, metric_key, scope_type, scope_id, min_value, max_value, severity, active,
                       created_by_user_id, updated_by_user_id, created_at, updated_at
                from threshold_rule
                """;
    }

    private record QueryParts(String where, List<Object> args) {
    }
}
