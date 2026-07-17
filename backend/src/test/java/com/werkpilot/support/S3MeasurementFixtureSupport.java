package com.werkpilot.support;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class S3MeasurementFixtureSupport {

    private final JdbcTemplate jdbcTemplate;

    public S3MeasurementFixtureSupport(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Fixture createFixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        UUID factoryId = insertReturningId("insert into factory (code, name) values (?, ?) returning id", "F3" + suffix, "Factory " + suffix);
        UUID lineId = insertReturningId(
                "insert into production_line (factory_id, code, name) values (?, ?, ?) returning id",
                factoryId,
                "L3" + suffix,
                "Line " + suffix);
        UUID machineId = insertReturningId(
                "insert into machine (factory_id, line_id, code, name) values (?, ?, ?, ?) returning id",
                factoryId,
                lineId,
                "M3" + suffix,
                "Machine " + suffix);
        UUID secondMachineId = insertReturningId(
                "insert into machine (factory_id, line_id, code, name) values (?, ?, ?, ?) returning id",
                factoryId,
                lineId,
                "M3B" + suffix,
                "Machine B " + suffix);
        UUID productId = insertReturningId(
                "insert into product (code, name, family) values (?, ?, ?) returning id",
                "P3" + suffix,
                "Product " + suffix,
                "Pilot");
        UUID secondProductId = insertReturningId(
                "insert into product (code, name, family) values (?, ?, ?) returning id",
                "P3B" + suffix,
                "Product B " + suffix,
                "Pilot");
        UUID shiftId = insertReturningId(
                "insert into shift (code, name, start_time, end_time, planned_minutes) values (?, ?, '06:00:00', '14:00:00', 480) returning id",
                "S3" + suffix,
                "Shift " + suffix);
        UUID secondShiftId = insertReturningId(
                "insert into shift (code, name, start_time, end_time, planned_minutes) values (?, ?, '14:00:00', '22:00:00', 480) returning id",
                "S3B" + suffix,
                "Shift B " + suffix);
        return new Fixture(factoryId, lineId, machineId, secondMachineId, productId, secondProductId, shiftId, secondShiftId);
    }

    public UUID importJob(String type, String status) {
        UUID adminUserId = jdbcTemplate.queryForObject(
                "select id from app_user where email = 'admin@werkpilot.local'",
                UUID.class);
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        insert into import_job
                        (id, import_type, status, original_filename, safe_filename, file_hash_sha256, file_size_bytes,
                         total_rows, valid_rows, error_count, error_overflow, created_by_user_id, created_at, completed_at)
                        values (?, ?, ?, ?, ?, ?, 10, 1, 1, 0, false, ?, now(), now())
                        """,
                id,
                type,
                status,
                id + ".csv",
                id + ".csv",
                hex64(id),
                adminUserId);
        return id;
    }

    public void production(UUID jobId, Fixture fixture, Instant start, Instant end, int units) {
        production(jobId, fixture, fixture.machineId(), fixture.productId(), fixture.shiftId(), start, end, units);
    }

    public void production(
            UUID jobId,
            Fixture fixture,
            UUID machineId,
            UUID productId,
            UUID shiftId,
            Instant start,
            Instant end,
            int units) {
        jdbcTemplate.update(
                """
                        insert into production_record
                        (id, import_job_id, period_start, period_end, factory_id, line_id, machine_id, product_id, shift_id, units_produced, batch_code)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                jobId,
                Timestamp.from(start),
                Timestamp.from(end),
                fixture.factoryId(),
                fixture.lineId(),
                machineId,
                productId,
                shiftId,
                units,
                "BATCH-S3");
    }

    public void energy(UUID jobId, Fixture fixture, Instant start, Instant end, BigDecimal energyKwh) {
        jdbcTemplate.update(
                """
                        insert into energy_measurement
                        (id, import_job_id, period_start, period_end, factory_id, line_id, machine_id, shift_id, energy_kwh)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                jobId,
                Timestamp.from(start),
                Timestamp.from(end),
                fixture.factoryId(),
                fixture.lineId(),
                fixture.machineId(),
                fixture.shiftId(),
                energyKwh);
    }

    public void downtime(UUID jobId, Fixture fixture, Instant start, Instant end, int minutes) {
        UUID reasonId = insertReturningId(
                "insert into downtime_reason (code, name) values (?, ?) returning id",
                "R3" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "Reason S3");
        jdbcTemplate.update(
                """
                        insert into downtime_record
                        (id, import_job_id, period_start, period_end, machine_id, shift_id, downtime_min, reason_id)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                jobId,
                Timestamp.from(start),
                Timestamp.from(end),
                fixture.machineId(),
                fixture.shiftId(),
                minutes,
                reasonId);
    }

    public void scrap(UUID jobId, Fixture fixture, Instant start, Instant end, int count) {
        UUID categoryId = insertReturningId(
                "insert into scrap_category (code, name) values (?, ?) returning id",
                "C3" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "Category S3");
        jdbcTemplate.update(
                """
                        insert into scrap_record
                        (id, import_job_id, period_start, period_end, machine_id, product_id, shift_id, scrap_count, scrap_category_id)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                jobId,
                Timestamp.from(start),
                Timestamp.from(end),
                fixture.machineId(),
                fixture.productId(),
                fixture.shiftId(),
                count,
                categoryId);
    }

    private UUID insertReturningId(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, UUID.class, args);
    }

    private static String hex64(UUID id) {
        return id.toString().replace("-", "") + id.toString().replace("-", "");
    }

    public record Fixture(
            UUID factoryId,
            UUID lineId,
            UUID machineId,
            UUID secondMachineId,
            UUID productId,
            UUID secondProductId,
            UUID shiftId,
            UUID secondShiftId) {
    }
}
