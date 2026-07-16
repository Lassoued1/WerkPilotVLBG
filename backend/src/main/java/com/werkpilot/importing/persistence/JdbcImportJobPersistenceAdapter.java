package com.werkpilot.importing.persistence;

import com.werkpilot.importing.application.port.ImportJobErrorRecord;
import com.werkpilot.importing.application.port.ImportJobPort;
import com.werkpilot.importing.application.port.ImportJobRecord;
import com.werkpilot.importing.domain.ImportJobStatus;
import com.werkpilot.importing.domain.ImportType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcImportJobPersistenceAdapter implements ImportJobPort {

    private final JdbcTemplate jdbcTemplate;

    JdbcImportJobPersistenceAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsNormalImportByTypeAndHash(ImportType importType, String fileHashSha256) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from import_job where import_type = ? and file_hash_sha256 = ? and corrects_import_job_id is null",
                Long.class,
                importType.name(),
                fileHashSha256);
        return count != null && count > 0;
    }

    @Override
    public ImportJobRecord create(ImportJobRecord job) {
        jdbcTemplate.update(
                """
                        insert into import_job
                        (id, import_type, status, original_filename, safe_filename, file_hash_sha256, file_size_bytes,
                         total_rows, valid_rows, error_count, error_overflow, corrects_import_job_id, created_by_user_id,
                         created_at, completed_at, failure_reason, superseded_reason)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                job.id(),
                job.importType().name(),
                job.status().name(),
                job.originalFilename(),
                job.safeFilename(),
                job.fileHashSha256(),
                job.fileSizeBytes(),
                job.totalRows(),
                job.validRows(),
                job.errorCount(),
                job.errorOverflow(),
                job.correctsImportJobId(),
                job.createdByUserId(),
                Timestamp.from(job.createdAt()),
                job.completedAt() == null ? null : Timestamp.from(job.completedAt()),
                job.failureReason(),
                job.supersededReason());
        return findById(job.id()).orElseThrow();
    }

    @Override
    public Optional<ImportJobRecord> findById(UUID id) {
        List<ImportJobRecord> records = jdbcTemplate.query(selectJobs() + " where id = ?", this::mapJob, id);
        return records.stream().findFirst();
    }

    @Override
    public Page<ImportJobRecord> list(Pageable pageable) {
        List<ImportJobRecord> items = jdbcTemplate.query(
                selectJobs() + " order by created_at desc limit ? offset ?",
                this::mapJob,
                pageable.getPageSize(),
                pageable.getOffset());
        Long total = jdbcTemplate.queryForObject("select count(*) from import_job", Long.class);
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }

    @Override
    public Page<ImportJobErrorRecord> listErrors(UUID importJobId, Pageable pageable) {
        List<ImportJobErrorRecord> items = jdbcTemplate.query(
                """
                        select id, import_job_id, row_number, column_name, rejected_value, message, created_at
                        from import_job_error
                        where import_job_id = ?
                        order by row_number asc, column_name asc
                        limit ? offset ?
                        """,
                this::mapError,
                importJobId,
                pageable.getPageSize(),
                pageable.getOffset());
        Long total = jdbcTemplate.queryForObject("select count(*) from import_job_error where import_job_id = ?", Long.class, importJobId);
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }

    @Override
    @Transactional
    public void replaceErrors(UUID importJobId, List<ImportJobErrorRecord> errors, int totalErrorCount) {
        jdbcTemplate.update("delete from import_job_error where import_job_id = ?", importJobId);
        for (ImportJobErrorRecord error : errors) {
            jdbcTemplate.update(
                    """
                            insert into import_job_error (id, import_job_id, row_number, column_name, rejected_value, message, created_at)
                            values (?, ?, ?, ?, ?, ?, now())
                            """,
                    error.id(),
                    importJobId,
                    error.rowNumber(),
                    error.columnName(),
                    error.rejectedValue(),
                    error.message());
        }
        jdbcTemplate.update(
                "update import_job set error_count = ?, error_overflow = ?, status = 'FAILED', completed_at = now() where id = ?",
                totalErrorCount,
                totalErrorCount > errors.size(),
                importJobId);
    }


    @Override
    public void markCommitted(UUID importJobId, int totalRows, int validRows) {
        jdbcTemplate.update(
                "update import_job set total_rows = ?, valid_rows = ?, error_count = 0, error_overflow = false, status = 'COMMITTED', completed_at = now(), failure_reason = null where id = ?",
                totalRows,
                validRows,
                importJobId);
    }

    @Override
    public boolean supersede(UUID importJobId, String reason) {
        int updated = jdbcTemplate.update(
                "update import_job set status = 'SUPERSEDED', superseded_reason = ? where id = ? and status = 'COMMITTED'",
                reason,
                importJobId);
        return updated == 1;
    }

    private ImportJobRecord mapJob(ResultSet resultSet, int rowNum) throws SQLException {
        Timestamp completedAt = resultSet.getTimestamp("completed_at");
        return new ImportJobRecord(
                resultSet.getObject("id", UUID.class),
                ImportType.valueOf(resultSet.getString("import_type")),
                ImportJobStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("original_filename"),
                resultSet.getString("safe_filename"),
                resultSet.getString("file_hash_sha256"),
                resultSet.getLong("file_size_bytes"),
                resultSet.getInt("total_rows"),
                resultSet.getInt("valid_rows"),
                resultSet.getInt("error_count"),
                resultSet.getBoolean("error_overflow"),
                resultSet.getObject("corrects_import_job_id", UUID.class),
                resultSet.getObject("created_by_user_id", UUID.class),
                resultSet.getTimestamp("created_at").toInstant(),
                completedAt == null ? null : completedAt.toInstant(),
                resultSet.getString("failure_reason"),
                resultSet.getString("superseded_reason"));
    }

    private ImportJobErrorRecord mapError(ResultSet resultSet, int rowNum) throws SQLException {
        return new ImportJobErrorRecord(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("import_job_id", UUID.class),
                resultSet.getInt("row_number"),
                resultSet.getString("column_name"),
                resultSet.getString("rejected_value"),
                resultSet.getString("message"),
                resultSet.getTimestamp("created_at").toInstant());
    }

    private static String selectJobs() {
        return """
                select id, import_type, status, original_filename, safe_filename, file_hash_sha256, file_size_bytes,
                       total_rows, valid_rows, error_count, error_overflow, corrects_import_job_id, created_by_user_id,
                       created_at, completed_at, failure_reason, superseded_reason
                from import_job
                """;
    }
}
