package com.werkpilot.masterdata.persistence;

import com.werkpilot.masterdata.application.port.SystemSettings;
import com.werkpilot.masterdata.application.port.SystemSettingsPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSystemSettingsPersistenceAdapter implements SystemSettingsPort {

    private static final int SETTINGS_ID = 1;

    private final JdbcTemplate jdbcTemplate;

    JdbcSystemSettingsPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SystemSettings get() {
        List<SystemSettings> settings = jdbcTemplate.query(
                """
                        select energy_threshold_delegation_enabled, updated_by_user_id, created_at, updated_at
                        from system_settings
                        where id = ?
                        """,
                this::mapSettings,
                SETTINGS_ID);
        return settings.getFirst();
    }

    @Override
    public SystemSettings setEnergyThresholdDelegationEnabled(boolean enabled, UUID actorUserId) {
        jdbcTemplate.update(
                """
                        update system_settings
                        set energy_threshold_delegation_enabled = ?,
                            updated_by_user_id = ?,
                            updated_at = now()
                        where id = ?
                        """,
                enabled,
                actorUserId,
                SETTINGS_ID);
        return get();
    }

    private SystemSettings mapSettings(ResultSet resultSet, int rowNum) throws SQLException {
        return new SystemSettings(
                resultSet.getBoolean("energy_threshold_delegation_enabled"),
                resultSet.getObject("updated_by_user_id", UUID.class),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }
}
