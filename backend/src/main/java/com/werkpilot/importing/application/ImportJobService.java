package com.werkpilot.importing.application;

import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.domain.AuditEventType;
import com.werkpilot.importing.application.port.ImportJobErrorRecord;
import com.werkpilot.importing.application.port.ImportJobPort;
import com.werkpilot.importing.application.port.ImportJobRecord;
import com.werkpilot.importing.domain.ImportJobStatus;
import com.werkpilot.importing.domain.ImportType;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import com.werkpilot.shared.security.AuthenticatedPrincipal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportJobService {

    public static final int MAX_RECORDED_ERRORS = 500;
    public static final long MAX_FILE_SIZE_BYTES = 25L * 1024L * 1024L;

    private final ImportJobPort importJobPort;
    private final AuditEventPort auditEventPort;
    private final Clock clock;

    public ImportJobService(ImportJobPort importJobPort, AuditEventPort auditEventPort, Clock clock) {
        this.importJobPort = importJobPort;
        this.auditEventPort = auditEventPort;
        this.clock = clock;
    }


    public ImportJobRecord startProductionImportJob(MultipartFile file, AuthenticatedPrincipal principal) {
        return startImport(ImportType.PRODUCTION_RECORDS, file, principal);
    }
    public ImportJobStartResponse startProductionImport(MultipartFile file, AuthenticatedPrincipal principal) {
        return job(startImport(ImportType.PRODUCTION_RECORDS, file, principal));
    }

    public ImportJobRecord startEnergyImportJob(MultipartFile file, AuthenticatedPrincipal principal) {
        return startImport(ImportType.ENERGY_MEASUREMENTS, file, principal);
    }

    public ImportJobStartResponse startEnergyImport(MultipartFile file, AuthenticatedPrincipal principal) {
        return job(startImport(ImportType.ENERGY_MEASUREMENTS, file, principal));
    }

    public ImportJobRecord startDowntimeImportJob(MultipartFile file, AuthenticatedPrincipal principal) {
        return startImport(ImportType.DOWNTIME_RECORDS, file, principal);
    }

    public ImportJobStartResponse startDowntimeImport(MultipartFile file, AuthenticatedPrincipal principal) {
        return job(startImport(ImportType.DOWNTIME_RECORDS, file, principal));
    }

    public ImportJobRecord startScrapImportJob(MultipartFile file, AuthenticatedPrincipal principal) {
        return startImport(ImportType.SCRAP_RECORDS, file, principal);
    }

    public ImportJobStartResponse startScrapImport(MultipartFile file, AuthenticatedPrincipal principal) {
        return job(startImport(ImportType.SCRAP_RECORDS, file, principal));
    }

    public ImportJobRecord startImport(ImportType importType, MultipartFile file, AuthenticatedPrincipal principal) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "CSV file is required.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.CSV_VALIDATION_FAILED, "CSV file exceeds the configured size limit.");
        }

        String hash = sha256(file);
        if (importJobPort.existsNormalImportByTypeAndHash(importType, hash)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.IMPORT_DUPLICATE_FILE, "A file with the same hash was already imported for this type.");
        }

        ImportJobRecord created = importJobPort.create(new ImportJobRecord(
                UUID.randomUUID(),
                importType,
                ImportJobStatus.PROCESSING,
                originalFilename(file),
                safeFilename(file),
                hash,
                file.getSize(),
                0,
                0,
                0,
                false,
                null,
                principal.userId(),
                clock.instant(),
                null,
                null));

        auditEventPort.append(
                AuditEventType.CSV_IMPORT_STARTED,
                principal.userId(),
                null,
                "importJobId=%s; importType=%s; safeFilename=%s".formatted(created.id(), created.importType(), created.safeFilename()));
        return created;
    }

    public ImportJobPage<ImportJobListItemResponse> listJobs(int page, int size) {
        Page<ImportJobRecord> jobs = list(PageRequest.of(page, size));
        return new ImportJobPage<>(jobs.getContent().stream().map(this::listItem).toList(), page, size, jobs.getTotalElements(), jobs.getTotalPages());
    }

    public ImportJobPage<ImportJobErrorResponse> listErrors(UUID importJobId, int page, int size) {
        Page<ImportJobErrorRecord> errors = listErrors(importJobId, PageRequest.of(page, size));
        return new ImportJobPage<>(errors.getContent().stream().map(this::error).toList(), page, size, errors.getTotalElements(), errors.getTotalPages());
    }

    public Page<ImportJobRecord> list(Pageable pageable) {
        return importJobPort.list(pageable);
    }

    public Page<ImportJobErrorRecord> listErrors(UUID importJobId, Pageable pageable) {
        if (importJobPort.findById(importJobId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "Import job was not found.");
        }
        return importJobPort.listErrors(importJobId, pageable);
    }

    public void replaceErrorsForFailedJob(UUID importJobId, List<ImportJobErrorRecord> errors, int totalErrorCount) {
        importJobPort.replaceErrors(importJobId, errors.stream().limit(MAX_RECORDED_ERRORS).toList(), totalErrorCount);
    }

    private static ImportJobStartResponse job(ImportJobRecord job) {
        return new ImportJobStartResponse(job.id(), job.status().name(), job.createdAt(), job.completedAt());
    }

    private ImportJobListItemResponse listItem(ImportJobRecord job) {
        return new ImportJobListItemResponse(
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
                job.createdAt(),
                job.completedAt(),
                job.failureReason());
    }

    private ImportJobErrorResponse error(ImportJobErrorRecord error) {
        return new ImportJobErrorResponse(
                error.id(),
                error.importJobId(),
                error.rowNumber(),
                error.columnName(),
                error.rejectedValue(),
                error.message(),
                error.createdAt());
    }

    private static String sha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(file.getBytes()));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JVM", exception);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "CSV file could not be read.");
        }
    }

    private static String originalFilename(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "upload.csv";
        }
        return original.strip();
    }

    private static String safeFilename(MultipartFile file) {
        String basename = originalFilename(file).replace('\\', '/');
        int slash = basename.lastIndexOf('/');
        if (slash >= 0) {
            basename = basename.substring(slash + 1);
        }
        String safe = basename.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "_")
                .replaceAll("_+", "_");
        if (safe.isBlank() || ".".equals(safe) || "..".equals(safe)) {
            return "upload.csv";
        }
        return safe.length() > 255 ? safe.substring(safe.length() - 255) : safe;
    }

    public record ImportJobStartResponse(
            UUID jobId,
            String status,
            Instant createdAt,
            Instant completedAt) {
    }

    public record ImportJobPage<T>(
            List<T> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    public record ImportJobListItemResponse(
            UUID id,
            String importType,
            String status,
            String originalFilename,
            String safeFilename,
            String fileHashSha256,
            long fileSizeBytes,
            int totalRows,
            int validRows,
            int errorCount,
            boolean errorOverflow,
            UUID correctsImportJobId,
            UUID createdByUserId,
            Instant createdAt,
            Instant completedAt,
            String failureReason) {
    }

    public record ImportJobErrorResponse(
            UUID id,
            UUID importJobId,
            int rowNumber,
            String columnName,
            String rejectedValue,
            String message,
            Instant createdAt) {
    }
}


