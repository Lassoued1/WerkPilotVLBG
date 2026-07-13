package com.werkpilot.masterdata.persistence;

import com.werkpilot.masterdata.application.MasterDataKind;
import com.werkpilot.masterdata.application.port.MasterDataPort;
import com.werkpilot.masterdata.application.port.MasterDataRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcMasterDataPersistenceAdapter implements MasterDataPort {

    private final JdbcTemplate jdbcTemplate;

    JdbcMasterDataPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Page<MasterDataRecord> list(MasterDataKind kind, boolean includeInactive, Pageable pageable) {
        String activeFilter = includeInactive ? "" : " where active = true";
        List<MasterDataRecord> items = jdbcTemplate.query(
                select(kind) + activeFilter + " order by code asc limit ? offset ?",
                this::mapRecord,
                pageable.getPageSize(),
                pageable.getOffset());
        Long total = jdbcTemplate.queryForObject("select count(*) from " + table(kind) + activeFilter, Long.class);
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }

    @Override
    public Optional<MasterDataRecord> findById(MasterDataKind kind, UUID id) {
        return optional(select(kind) + " where id = ?", id);
    }

    @Override
    public Optional<MasterDataRecord> findByCode(MasterDataKind kind, String code) {
        return optional(select(kind) + " where code = ?", code);
    }

    @Override
    public Optional<MasterDataRecord> findLineByFactoryAndCode(UUID factoryId, String code) {
        return optional(select(MasterDataKind.PRODUCTION_LINE) + " where factory_id = ? and code = ?", factoryId, code);
    }

    @Override
    public Optional<MasterDataRecord> findMachineByFactoryAndCode(UUID factoryId, String code) {
        return optional(select(MasterDataKind.MACHINE) + " where factory_id = ? and code = ?", factoryId, code);
    }

    @Override
    public List<MasterDataRecord> findMachinesByFactoryAndCode(UUID factoryId, String code) {
        return jdbcTemplate.query(select(MasterDataKind.MACHINE) + " where factory_id = ? and code = ?", this::mapRecord, factoryId, code);
    }

    @Override
    public MasterDataRecord createFactory(String code, String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into factory (id, code, name, active, created_at, updated_at) values (?, ?, ?, true, now(), now())",
                id,
                code,
                name);
        return findById(MasterDataKind.FACTORY, id).orElseThrow();
    }

    @Override
    public MasterDataRecord createLine(UUID factoryId, String code, String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into production_line (id, factory_id, code, name, active, created_at, updated_at) values (?, ?, ?, ?, true, now(), now())",
                id,
                factoryId,
                code,
                name);
        return findById(MasterDataKind.PRODUCTION_LINE, id).orElseThrow();
    }

    @Override
    public MasterDataRecord createMachine(UUID factoryId, UUID lineId, String code, String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into machine (id, factory_id, line_id, code, name, active, created_at, updated_at) values (?, ?, ?, ?, ?, true, now(), now())",
                id,
                factoryId,
                lineId,
                code,
                name);
        return findById(MasterDataKind.MACHINE, id).orElseThrow();
    }

    @Override
    public MasterDataRecord createProduct(String code, String name, String family) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into product (id, code, name, family, active, created_at, updated_at) values (?, ?, ?, ?, true, now(), now())",
                id,
                code,
                name,
                family);
        return findById(MasterDataKind.PRODUCT, id).orElseThrow();
    }

    @Override
    public MasterDataRecord createShift(String code, String name, LocalTime startTime, LocalTime endTime, int plannedMinutes) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into shift (id, code, name, start_time, end_time, planned_minutes, active, created_at, updated_at) values (?, ?, ?, ?, ?, ?, true, now(), now())",
                id,
                code,
                name,
                startTime,
                endTime,
                plannedMinutes);
        return findById(MasterDataKind.SHIFT, id).orElseThrow();
    }

    @Override
    public MasterDataRecord createSimple(MasterDataKind kind, String code, String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into " + table(kind) + " (id, code, name, active, created_at, updated_at) values (?, ?, ?, true, now(), now())",
                id,
                code,
                name);
        return findById(kind, id).orElseThrow();
    }

    @Override
    public MasterDataRecord updateFactory(UUID id, String code, String name, boolean active) {
        jdbcTemplate.update("update factory set code = ?, name = ?, active = ?, updated_at = now() where id = ?", code, name, active, id);
        return findById(MasterDataKind.FACTORY, id).orElseThrow();
    }

    @Override
    public MasterDataRecord updateLine(UUID id, UUID factoryId, String code, String name, boolean active) {
        jdbcTemplate.update(
                "update production_line set factory_id = ?, code = ?, name = ?, active = ?, updated_at = now() where id = ?",
                factoryId,
                code,
                name,
                active,
                id);
        return findById(MasterDataKind.PRODUCTION_LINE, id).orElseThrow();
    }

    @Override
    public MasterDataRecord updateMachine(UUID id, UUID factoryId, UUID lineId, String code, String name, boolean active) {
        jdbcTemplate.update(
                "update machine set factory_id = ?, line_id = ?, code = ?, name = ?, active = ?, updated_at = now() where id = ?",
                factoryId,
                lineId,
                code,
                name,
                active,
                id);
        return findById(MasterDataKind.MACHINE, id).orElseThrow();
    }

    @Override
    public MasterDataRecord updateProduct(UUID id, String code, String name, String family, boolean active) {
        jdbcTemplate.update(
                "update product set code = ?, name = ?, family = ?, active = ?, updated_at = now() where id = ?",
                code,
                name,
                family,
                active,
                id);
        return findById(MasterDataKind.PRODUCT, id).orElseThrow();
    }

    @Override
    public MasterDataRecord updateShift(
            UUID id,
            String code,
            String name,
            LocalTime startTime,
            LocalTime endTime,
            int plannedMinutes,
            boolean active) {
        jdbcTemplate.update(
                "update shift set code = ?, name = ?, start_time = ?, end_time = ?, planned_minutes = ?, active = ?, updated_at = now() where id = ?",
                code,
                name,
                startTime,
                endTime,
                plannedMinutes,
                active,
                id);
        return findById(MasterDataKind.SHIFT, id).orElseThrow();
    }

    @Override
    public MasterDataRecord updateSimple(MasterDataKind kind, UUID id, String code, String name, boolean active) {
        jdbcTemplate.update(
                "update " + table(kind) + " set code = ?, name = ?, active = ?, updated_at = now() where id = ?",
                code,
                name,
                active,
                id);
        return findById(kind, id).orElseThrow();
    }

    @Override
    public void setActive(MasterDataKind kind, UUID id, boolean active) {
        jdbcTemplate.update("update " + table(kind) + " set active = ?, updated_at = now() where id = ?", active, id);
    }

    private Optional<MasterDataRecord> optional(String sql, Object... args) {
        List<MasterDataRecord> records = jdbcTemplate.query(sql, this::mapRecord, args);
        return records.stream().findFirst();
    }

    private MasterDataRecord mapRecord(ResultSet resultSet, int rowNum) throws SQLException {
        return new MasterDataRecord(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getBoolean("active"),
                resultSet.getObject("factory_id", UUID.class),
                resultSet.getObject("line_id", UUID.class),
                resultSet.getString("family"),
                resultSet.getObject("start_time", LocalTime.class),
                resultSet.getObject("end_time", LocalTime.class),
                (Integer) resultSet.getObject("planned_minutes"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }

    private static String select(MasterDataKind kind) {
        return switch (kind) {
            case FACTORY -> """
                    select id, code, name, active, null::uuid as factory_id, null::uuid as line_id,
                           null::varchar as family, null::time as start_time, null::time as end_time,
                           null::integer as planned_minutes, created_at, updated_at
                    from factory""";
            case PRODUCTION_LINE -> """
                    select id, code, name, active, factory_id, null::uuid as line_id,
                           null::varchar as family, null::time as start_time, null::time as end_time,
                           null::integer as planned_minutes, created_at, updated_at
                    from production_line""";
            case MACHINE -> """
                    select id, code, name, active, factory_id, line_id,
                           null::varchar as family, null::time as start_time, null::time as end_time,
                           null::integer as planned_minutes, created_at, updated_at
                    from machine""";
            case PRODUCT -> """
                    select id, code, name, active, null::uuid as factory_id, null::uuid as line_id,
                           family, null::time as start_time, null::time as end_time,
                           null::integer as planned_minutes, created_at, updated_at
                    from product""";
            case SHIFT -> """
                    select id, code, name, active, null::uuid as factory_id, null::uuid as line_id,
                           null::varchar as family, start_time, end_time, planned_minutes, created_at, updated_at
                    from shift""";
            case DOWNTIME_REASON -> """
                    select id, code, name, active, null::uuid as factory_id, null::uuid as line_id,
                           null::varchar as family, null::time as start_time, null::time as end_time,
                           null::integer as planned_minutes, created_at, updated_at
                    from downtime_reason""";
            case SCRAP_CATEGORY -> """
                    select id, code, name, active, null::uuid as factory_id, null::uuid as line_id,
                           null::varchar as family, null::time as start_time, null::time as end_time,
                           null::integer as planned_minutes, created_at, updated_at
                    from scrap_category""";
        };
    }

    private static String table(MasterDataKind kind) {
        return switch (kind) {
            case FACTORY -> "factory";
            case PRODUCTION_LINE -> "production_line";
            case MACHINE -> "machine";
            case PRODUCT -> "product";
            case SHIFT -> "shift";
            case DOWNTIME_REASON -> "downtime_reason";
            case SCRAP_CATEGORY -> "scrap_category";
        };
    }
}
