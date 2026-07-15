package com.werkpilot.importing.application;

import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.domain.AuditEventType;
import com.werkpilot.importing.application.csv.CsvColumn;
import com.werkpilot.importing.application.csv.CsvRow;
import com.werkpilot.importing.application.csv.CsvTemplate;
import com.werkpilot.importing.application.csv.CsvValidationError;
import com.werkpilot.importing.application.csv.CsvValidationResult;
import com.werkpilot.importing.application.csv.StrictCsvParserService;
import com.werkpilot.importing.application.port.ImportJobErrorRecord;
import com.werkpilot.importing.application.port.ImportJobPort;
import com.werkpilot.importing.application.port.ImportJobRecord;
import com.werkpilot.masterdata.application.MasterDataKind;
import com.werkpilot.masterdata.application.port.MasterDataPort;
import com.werkpilot.masterdata.application.port.MasterDataRecord;
import com.werkpilot.quality.application.port.ScrapRecordDraft;
import com.werkpilot.quality.application.port.ScrapRecordPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ScrapCsvImportService {

    public static final CsvTemplate TEMPLATE = new CsvTemplate("scrap-records", List.of(
            CsvColumn.utcInstant("period_start"),
            CsvColumn.utcInstant("period_end"),
            CsvColumn.requiredString("machine_code", 40),
            CsvColumn.requiredString("product_code", 40),
            CsvColumn.requiredString("shift_code", 40),
            CsvColumn.nonNegativeInteger("scrap_count"),
            CsvColumn.requiredString("scrap_category_code", 40)), 100_000);

    private final StrictCsvParserService csvParser;
    private final MasterDataPort masterDataPort;
    private final ScrapRecordPort scrapRecordPort;
    private final ImportJobPort importJobPort;
    private final ImportJobService importJobService;
    private final AuditEventPort auditEventPort;

    public ScrapCsvImportService(
            StrictCsvParserService csvParser,
            MasterDataPort masterDataPort,
            ScrapRecordPort scrapRecordPort,
            ImportJobPort importJobPort,
            ImportJobService importJobService,
            AuditEventPort auditEventPort) {
        this.csvParser = csvParser;
        this.masterDataPort = masterDataPort;
        this.scrapRecordPort = scrapRecordPort;
        this.importJobPort = importJobPort;
        this.importJobService = importJobService;
        this.auditEventPort = auditEventPort;
    }

    @Transactional
    public void process(ImportJobRecord job, MultipartFile file) {
        CsvValidationResult result = csvParser.validate(bytes(file), TEMPLATE, ImportJobService.MAX_RECORDED_ERRORS);
        List<CsvValidationError> semanticErrors = new ArrayList<>(result.errors());
        List<ScrapRecordDraft> records = new ArrayList<>();
        if (result.valid()) {
            for (CsvRow row : result.rows()) {
                toDraft(job.id(), row, semanticErrors).ifPresent(records::add);
            }
        }
        finish(job, file, result, semanticErrors, records);
    }

    private Optional<ScrapRecordDraft> toDraft(UUID jobId, CsvRow row, List<CsvValidationError> errors) {
        UUID machineId = resolveUniqueMachine(row, errors).orElse(null);
        UUID productId = resolve(MasterDataKind.PRODUCT, row, "product_code", errors).orElse(null);
        UUID shiftId = resolve(MasterDataKind.SHIFT, row, "shift_code", errors).orElse(null);
        UUID categoryId = resolve(MasterDataKind.SCRAP_CATEGORY, row, "scrap_category_code", errors).orElse(null);
        Instant start = Instant.parse(row.values().get("period_start"));
        Instant end = Instant.parse(row.values().get("period_end"));
        if (!end.isAfter(start)) {
            errors.add(error(row, "period_end", row.values().get("period_end"), "Das Ende muss nach dem Start liegen."));
        }
        if (machineId == null || productId == null || shiftId == null || categoryId == null || errors.stream().anyMatch(error -> error.rowNumber() == row.rowNumber())) {
            return Optional.empty();
        }
        return Optional.of(new ScrapRecordDraft(
                UUID.randomUUID(), jobId, start, end, machineId, productId, shiftId,
                Integer.parseInt(row.values().get("scrap_count")), categoryId));
    }

    private void finish(ImportJobRecord job, MultipartFile file, CsvValidationResult result, List<CsvValidationError> semanticErrors, List<ScrapRecordDraft> records) {
        int totalRows = countDataRows(bytes(file));
        int totalErrors = result.totalErrorCount() + semanticErrors.size() - result.errors().size();
        if (totalErrors > 0 || !semanticErrors.isEmpty()) {
            importJobService.replaceErrorsForFailedJob(job.id(), semanticErrors.stream()
                    .limit(ImportJobService.MAX_RECORDED_ERRORS)
                    .map(error -> new ImportJobErrorRecord(UUID.randomUUID(), job.id(), error.rowNumber(), error.columnName(), error.rejectedValue(), error.message(), null))
                    .toList(), Math.max(totalErrors, semanticErrors.size()));
            auditEventPort.append(AuditEventType.CSV_IMPORT_FAILED, job.createdByUserId(), null,
                    "importJobId=%s; importType=%s; errors=%d".formatted(job.id(), job.importType(), Math.max(totalErrors, semanticErrors.size())));
            return;
        }
        scrapRecordPort.insertAll(records);
        importJobPort.markCommitted(job.id(), totalRows, records.size());
        auditEventPort.append(AuditEventType.CSV_IMPORT_COMMITTED, job.createdByUserId(), null,
                "importJobId=%s; importType=%s; rows=%d".formatted(job.id(), job.importType(), records.size()));
    }

    private Optional<UUID> resolve(MasterDataKind kind, CsvRow row, String column, List<CsvValidationError> errors) {
        String code = row.values().get(column);
        Optional<UUID> id = masterDataPort.findByCode(kind, code).filter(MasterDataRecord::active).map(MasterDataRecord::id);
        if (id.isEmpty()) {
            errors.add(error(row, column, code, "Der Stammdatencode ist unbekannt oder inaktiv."));
        }
        return id;
    }

    private Optional<UUID> resolveUniqueMachine(CsvRow row, List<CsvValidationError> errors) {
        String code = row.values().get("machine_code");
        List<MasterDataRecord> machines = masterDataPort.findMachinesByCode(code).stream().filter(MasterDataRecord::active).toList();
        if (machines.size() != 1) {
            errors.add(error(row, "machine_code", code, machines.isEmpty() ? "Der Stammdatencode ist unbekannt oder inaktiv." : "Der Maschinencode ist nicht eindeutig."));
            return Optional.empty();
        }
        return Optional.of(machines.getFirst().id());
    }

    private static CsvValidationError error(CsvRow row, String column, String value, String message) {
        return new CsvValidationError(com.werkpilot.shared.error.ErrorCode.CSV_VALIDATION_FAILED, row.rowNumber(), column, value, message);
    }

    private static byte[] bytes(MultipartFile file) {
        try { return file.getBytes(); } catch (Exception exception) { return new byte[0]; }
    }

    private static int countDataRows(byte[] content) {
        String[] lines = new String(content, java.nio.charset.StandardCharsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        int count = 0;
        for (int index = 1; index < lines.length; index++) {
            if (!lines[index].isBlank()) count++;
        }
        return count;
    }
}
