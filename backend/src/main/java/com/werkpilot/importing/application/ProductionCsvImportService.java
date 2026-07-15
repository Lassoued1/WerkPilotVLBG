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
import com.werkpilot.production.application.port.ProductionRecordDraft;
import com.werkpilot.production.application.port.ProductionRecordPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductionCsvImportService {

    public static final CsvTemplate TEMPLATE = new CsvTemplate("production-records", List.of(
            CsvColumn.utcInstant("period_start"),
            CsvColumn.utcInstant("period_end"),
            CsvColumn.requiredString("factory_code", 40),
            CsvColumn.requiredString("line_code", 40),
            CsvColumn.optionalString("machine_code", 40),
            CsvColumn.optionalString("product_code", 40),
            CsvColumn.requiredString("shift_code", 40),
            CsvColumn.nonNegativeInteger("units_produced"),
            CsvColumn.optionalString("batch_code", 80)), 100_000);

    private final StrictCsvParserService csvParser;
    private final MasterDataPort masterDataPort;
    private final ProductionRecordPort productionRecordPort;
    private final ImportJobPort importJobPort;
    private final ImportJobService importJobService;
    private final AuditEventPort auditEventPort;

    public ProductionCsvImportService(
            StrictCsvParserService csvParser,
            MasterDataPort masterDataPort,
            ProductionRecordPort productionRecordPort,
            ImportJobPort importJobPort,
            ImportJobService importJobService,
            AuditEventPort auditEventPort) {
        this.csvParser = csvParser;
        this.masterDataPort = masterDataPort;
        this.productionRecordPort = productionRecordPort;
        this.importJobPort = importJobPort;
        this.importJobService = importJobService;
        this.auditEventPort = auditEventPort;
    }

    @Transactional
    public void process(ImportJobRecord job, MultipartFile file) {
        CsvValidationResult result = csvParser.validate(bytes(file), TEMPLATE, ImportJobService.MAX_RECORDED_ERRORS);
        List<CsvValidationError> semanticErrors = new ArrayList<>(result.errors());
        List<ProductionRecordDraft> records = new ArrayList<>();

        if (result.valid()) {
            for (CsvRow row : result.rows()) {
                Optional<ProductionRecordDraft> draft = toDraft(job.id(), row, semanticErrors);
                draft.ifPresent(records::add);
            }
        }

        int totalRows = countDataRows(bytes(file));
        int totalErrors = result.totalErrorCount() + semanticErrors.size() - result.errors().size();
        if (totalErrors > 0 || !semanticErrors.isEmpty()) {
            List<ImportJobErrorRecord> errors = semanticErrors.stream()
                    .limit(ImportJobService.MAX_RECORDED_ERRORS)
                    .map(error -> new ImportJobErrorRecord(UUID.randomUUID(), job.id(), error.rowNumber(), error.columnName(), error.rejectedValue(), error.message(), null))
                    .toList();
            importJobService.replaceErrorsForFailedJob(job.id(), errors, Math.max(totalErrors, semanticErrors.size()));
            auditEventPort.append(AuditEventType.CSV_IMPORT_FAILED, job.createdByUserId(), null,
                    "importJobId=%s; importType=%s; errors=%d".formatted(job.id(), job.importType(), Math.max(totalErrors, semanticErrors.size())));
            return;
        }

        productionRecordPort.insertAll(records);
        importJobPort.markCommitted(job.id(), totalRows, records.size());
        auditEventPort.append(AuditEventType.CSV_IMPORT_COMMITTED, job.createdByUserId(), null,
                "importJobId=%s; importType=%s; rows=%d".formatted(job.id(), job.importType(), records.size()));
    }

    private Optional<ProductionRecordDraft> toDraft(UUID jobId, CsvRow row, List<CsvValidationError> errors) {
        UUID factoryId = resolve(MasterDataKind.FACTORY, row, "factory_code", errors).orElse(null);
        UUID shiftId = resolve(MasterDataKind.SHIFT, row, "shift_code", errors).orElse(null);
        UUID productId = optionalResolve(MasterDataKind.PRODUCT, row, "product_code", errors).orElse(null);
        UUID lineId = null;
        UUID machineId = null;

        String lineCode = row.values().get("line_code");
        if (factoryId != null && lineCode != null && !lineCode.isBlank()) {
            Optional<MasterDataRecord> line = masterDataPort.findLineByFactoryAndCode(factoryId, lineCode).filter(MasterDataRecord::active);
            if (line.isEmpty()) {
                errors.add(error(row, "line_code", lineCode, "Der Stammdatencode ist unbekannt oder inaktiv."));
            } else {
                lineId = line.get().id();
            }
        }

        String machineCode = row.values().get("machine_code");
        if (factoryId != null && machineCode != null && !machineCode.isBlank()) {
            Optional<MasterDataRecord> machine = masterDataPort.findMachineByFactoryAndCode(factoryId, machineCode).filter(MasterDataRecord::active);
            if (machine.isEmpty()) {
                errors.add(error(row, "machine_code", machineCode, "Der Stammdatencode ist unbekannt oder inaktiv."));
            } else {
                machineId = machine.get().id();
                if (lineId != null && !lineId.equals(machine.get().lineId())) {
                    errors.add(error(row, "machine_code", machineCode, "Die Maschine gehört nicht zur angegebenen Linie."));
                }
            }
        }

        Instant start = Instant.parse(row.values().get("period_start"));
        Instant end = Instant.parse(row.values().get("period_end"));
        if (!end.isAfter(start)) {
            errors.add(error(row, "period_end", row.values().get("period_end"), "Das Ende muss nach dem Start liegen."));
        }

        if (factoryId == null || lineId == null || shiftId == null || errors.stream().anyMatch(error -> error.rowNumber() == row.rowNumber())) {
            return Optional.empty();
        }

        return Optional.of(new ProductionRecordDraft(
                UUID.randomUUID(),
                jobId,
                start,
                end,
                factoryId,
                lineId,
                machineId,
                productId,
                shiftId,
                Integer.parseInt(row.values().get("units_produced")),
                blankToNull(row.values().get("batch_code"))));
    }

    private Optional<UUID> resolve(MasterDataKind kind, CsvRow row, String column, List<CsvValidationError> errors) {
        String code = row.values().get(column);
        Optional<UUID> id = active(kind, code);
        if (id.isEmpty()) {
            errors.add(error(row, column, code, "Der Stammdatencode ist unbekannt oder inaktiv."));
        }
        return id;
    }

    private Optional<UUID> optionalResolve(MasterDataKind kind, CsvRow row, String column, List<CsvValidationError> errors) {
        String code = row.values().get(column);
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return resolve(kind, row, column, errors);
    }

    private Optional<UUID> active(MasterDataKind kind, String code) {
        return masterDataPort.findByCode(kind, code).filter(MasterDataRecord::active).map(MasterDataRecord::id);
    }

    private static CsvValidationError error(CsvRow row, String column, String value, String message) {
        return new CsvValidationError(com.werkpilot.shared.error.ErrorCode.CSV_VALIDATION_FAILED, row.rowNumber(), column, value, message);
    }

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception exception) {
            return new byte[0];
        }
    }

    private static int countDataRows(byte[] content) {
        String text = new String(content, java.nio.charset.StandardCharsets.UTF_8).replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = text.split("\n", -1);
        int count = 0;
        for (int index = 1; index < lines.length; index++) {
            if (!lines[index].isBlank()) {
                count++;
            }
        }
        return count;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}