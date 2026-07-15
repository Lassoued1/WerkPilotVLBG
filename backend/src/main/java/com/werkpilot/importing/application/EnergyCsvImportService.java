package com.werkpilot.importing.application;

import com.werkpilot.audit.application.port.AuditEventPort;
import com.werkpilot.audit.domain.AuditEventType;
import com.werkpilot.energy.application.port.EnergyMeasurementDraft;
import com.werkpilot.energy.application.port.EnergyMeasurementPort;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EnergyCsvImportService {

    public static final CsvTemplate TEMPLATE = new CsvTemplate("energy-measurements", List.of(
            CsvColumn.utcInstant("period_start"),
            CsvColumn.utcInstant("period_end"),
            CsvColumn.requiredString("factory_code", 40),
            CsvColumn.requiredString("line_code", 40),
            CsvColumn.optionalString("machine_code", 40),
            CsvColumn.requiredString("shift_code", 40),
            CsvColumn.nonNegativeDecimal("energy_kwh")), 100_000);

    private final StrictCsvParserService csvParser;
    private final MasterDataPort masterDataPort;
    private final EnergyMeasurementPort energyMeasurementPort;
    private final ImportJobPort importJobPort;
    private final ImportJobService importJobService;
    private final AuditEventPort auditEventPort;

    public EnergyCsvImportService(
            StrictCsvParserService csvParser,
            MasterDataPort masterDataPort,
            EnergyMeasurementPort energyMeasurementPort,
            ImportJobPort importJobPort,
            ImportJobService importJobService,
            AuditEventPort auditEventPort) {
        this.csvParser = csvParser;
        this.masterDataPort = masterDataPort;
        this.energyMeasurementPort = energyMeasurementPort;
        this.importJobPort = importJobPort;
        this.importJobService = importJobService;
        this.auditEventPort = auditEventPort;
    }

    @Transactional
    public void process(ImportJobRecord job, MultipartFile file) {
        CsvValidationResult result = csvParser.validate(bytes(file), TEMPLATE, ImportJobService.MAX_RECORDED_ERRORS);
        List<CsvValidationError> semanticErrors = new ArrayList<>(result.errors());
        List<EnergyMeasurementDraft> measurements = new ArrayList<>();

        if (result.valid()) {
            List<GranularityWindow> windows = new ArrayList<>();
            for (CsvRow row : result.rows()) {
                Optional<EnergyMeasurementDraft> draft = toDraft(job.id(), row, semanticErrors, windows);
                draft.ifPresent(measurements::add);
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

        energyMeasurementPort.insertAll(measurements);
        importJobPort.markCommitted(job.id(), totalRows, measurements.size());
        auditEventPort.append(AuditEventType.CSV_IMPORT_COMMITTED, job.createdByUserId(), null,
                "importJobId=%s; importType=%s; rows=%d".formatted(job.id(), job.importType(), measurements.size()));
    }

    private Optional<EnergyMeasurementDraft> toDraft(UUID jobId, CsvRow row, List<CsvValidationError> errors, List<GranularityWindow> windows) {
        UUID factoryId = resolve(MasterDataKind.FACTORY, row, "factory_code", errors).orElse(null);
        UUID shiftId = resolve(MasterDataKind.SHIFT, row, "shift_code", errors).orElse(null);
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
                    errors.add(error(row, "machine_code", machineCode, "Die Maschine gehoert nicht zur angegebenen Linie."));
                }
            }
        }

        Instant start = Instant.parse(row.values().get("period_start"));
        Instant end = Instant.parse(row.values().get("period_end"));
        if (!end.isAfter(start)) {
            errors.add(error(row, "period_end", row.values().get("period_end"), "Das Ende muss nach dem Start liegen."));
        }

        boolean machineLevel = machineId != null;
        if (lineId != null && end.isAfter(start)) {
            if (energyMeasurementPort.existsCommittedOppositeGranularityOverlap(lineId, start, end, machineLevel)) {
                errors.add(error(row, "machine_code", machineCode == null ? "" : machineCode,
                        "Fuer diese Linie existiert im ueberlappenden Zeitraum bereits Energie mit anderer Granularitaet."));
            }
            for (GranularityWindow window : windows) {
                if (window.lineId().equals(lineId) && window.machineLevel() != machineLevel && overlaps(window.start(), window.end(), start, end)) {
                    errors.add(error(row, "machine_code", machineCode == null ? "" : machineCode,
                            "Fuer diese Linie darf im ueberlappenden Zeitraum nur eine Energie-Granularitaet verwendet werden."));
                    break;
                }
            }
        }

        if (factoryId == null || lineId == null || shiftId == null || errors.stream().anyMatch(error -> error.rowNumber() == row.rowNumber())) {
            return Optional.empty();
        }

        windows.add(new GranularityWindow(lineId, start, end, machineLevel));
        return Optional.of(new EnergyMeasurementDraft(
                UUID.randomUUID(),
                jobId,
                start,
                end,
                factoryId,
                lineId,
                machineId,
                shiftId,
                new BigDecimal(row.values().get("energy_kwh")).setScale(3)));
    }

    private Optional<UUID> resolve(MasterDataKind kind, CsvRow row, String column, List<CsvValidationError> errors) {
        String code = row.values().get(column);
        Optional<UUID> id = masterDataPort.findByCode(kind, code).filter(MasterDataRecord::active).map(MasterDataRecord::id);
        if (id.isEmpty()) {
            errors.add(error(row, column, code, "Der Stammdatencode ist unbekannt oder inaktiv."));
        }
        return id;
    }

    private static boolean overlaps(Instant leftStart, Instant leftEnd, Instant rightStart, Instant rightEnd) {
        return leftStart.isBefore(rightEnd) && leftEnd.isAfter(rightStart);
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

    private record GranularityWindow(UUID lineId, Instant start, Instant end, boolean machineLevel) {
    }
}

