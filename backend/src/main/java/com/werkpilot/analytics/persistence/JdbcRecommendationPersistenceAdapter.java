package com.werkpilot.analytics.persistence;

import com.werkpilot.analytics.application.RecommendationPort;
import com.werkpilot.analytics.application.RecommendationRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcRecommendationPersistenceAdapter implements RecommendationPort {

    private final JdbcTemplate jdbcTemplate;

    JdbcRecommendationPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void replaceForAnomaly(UUID anomalyId, List<RecommendationRecord> recommendations) {
        jdbcTemplate.update("delete from anomaly_recommendation where anomaly_id = ?", anomalyId);
        jdbcTemplate.batchUpdate(
                """
                        insert into anomaly_recommendation
                        (id, anomaly_id, template_code, template_version, message_de, disclaimer_de, created_at)
                        values (?, ?, ?, ?, ?, ?, now())
                        """,
                recommendations,
                100,
                (statement, recommendation) -> {
                    statement.setObject(1, recommendation.id());
                    statement.setObject(2, anomalyId);
                    statement.setString(3, recommendation.templateCode());
                    statement.setString(4, recommendation.templateVersion());
                    statement.setString(5, recommendation.messageDe());
                    statement.setString(6, recommendation.disclaimerDe());
                });
    }

    @Override
    public List<RecommendationRecord> findByAnomalyId(UUID anomalyId) {
        return jdbcTemplate.query(
                """
                        select id, anomaly_id, template_code, template_version, message_de, disclaimer_de, created_at
                        from anomaly_recommendation
                        where anomaly_id = ?
                        order by created_at asc, id asc
                        """,
                this::map,
                anomalyId);
    }

    private RecommendationRecord map(ResultSet rs, int rowNum) throws SQLException {
        return new RecommendationRecord(
                rs.getObject("id", UUID.class),
                rs.getObject("anomaly_id", UUID.class),
                rs.getString("template_code"),
                rs.getString("template_version"),
                rs.getString("message_de"),
                rs.getString("disclaimer_de"),
                rs.getTimestamp("created_at").toInstant());
    }
}
