package com.werkpilot.importing.application.csv;

import com.werkpilot.shared.error.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class StrictCsvParserService {

    public static final int DEFAULT_ERROR_LIMIT = 500;
    private static final Pattern NON_NEGATIVE_INTEGER = Pattern.compile("\\d+");
    private static final Pattern NON_NEGATIVE_DECIMAL = Pattern.compile("\\d+(\\.\\d+)?");

    private final CsvMasterDataResolver masterDataResolver;

    public StrictCsvParserService(CsvMasterDataResolver masterDataResolver) {
        this.masterDataResolver = masterDataResolver;
    }

    public CsvValidationResult validate(byte[] content, CsvTemplate template) {
        return validate(content, template, DEFAULT_ERROR_LIMIT);
    }

    public CsvValidationResult validate(byte[] content, CsvTemplate template, int errorLimit) {
        if (content == null || content.length == 0) {
            return result(List.of(), List.of(error(1, "file", "", "Die CSV-Datei darf nicht leer sein.")), 1, errorLimit);
        }
        if (errorLimit < 1) {
            throw new IllegalArgumentException("errorLimit must be positive.");
        }

        String text;
        try {
            text = decodeUtf8Strict(content);
        } catch (CharacterCodingException exception) {
            return result(List.of(), List.of(error(1, "file", "", "Die Datei muss UTF-8 kodiert sein.")), 1, errorLimit);
        }

        List<String> lines = splitLines(text);
        if (lines.isEmpty() || lines.getFirst().isBlank()) {
            return result(List.of(), List.of(error(1, "header", "", "Die CSV-Kopfzeile fehlt.")), 1, errorLimit);
        }

        List<String> header;
        try {
            header = parseLine(lines.getFirst());
        } catch (IllegalArgumentException exception) {
            return result(List.of(), List.of(error(1, "header", lines.getFirst(), exception.getMessage())), 1, errorLimit);
        }

        List<CsvValidationError> errors = new ArrayList<>();
        validateHeader(header, template, errors);
        if (!errors.isEmpty()) {
            return result(List.of(), errors, errors.size(), errorLimit);
        }

        List<CsvRow> rows = new ArrayList<>();
        int totalErrorCount = 0;
        int dataRows = 0;
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            dataRows++;
            int rowNumber = index + 1;
            if (dataRows > template.maxRows()) {
                totalErrorCount++;
                addCapped(errors, error(rowNumber, "file", "", "Die CSV-Datei enthält zu viele Datenzeilen."), errorLimit);
                continue;
            }

            List<String> values;
            try {
                values = parseLine(line);
            } catch (IllegalArgumentException exception) {
                totalErrorCount++;
                addCapped(errors, error(rowNumber, "row", line, exception.getMessage()), errorLimit);
                continue;
            }
            if (values.size() != template.columns().size()) {
                totalErrorCount++;
                addCapped(errors, error(rowNumber, "row", line, "Die Anzahl der Spalten stimmt nicht mit der Kopfzeile überein."), errorLimit);
                continue;
            }

            Map<String, String> rowValues = new HashMap<>();
            Map<String, UUID> resolvedIds = new HashMap<>();
            int rowErrors = validateRow(rowNumber, template, values, rowValues, resolvedIds, errors, errorLimit);
            totalErrorCount += rowErrors;
            if (rowErrors == 0) {
                rows.add(new CsvRow(rowNumber, rowValues, resolvedIds));
            }
        }
        return result(rows, errors, totalErrorCount, errorLimit);
    }

    private int validateRow(
            int rowNumber,
            CsvTemplate template,
            List<String> values,
            Map<String, String> rowValues,
            Map<String, UUID> resolvedIds,
            List<CsvValidationError> errors,
            int errorLimit) {
        int rowErrors = 0;
        for (int columnIndex = 0; columnIndex < template.columns().size(); columnIndex++) {
            CsvColumn column = template.columns().get(columnIndex);
            String value = values.get(columnIndex).strip();
            rowValues.put(column.name(), value);
            if (value.isEmpty()) {
                if (column.required()) {
                    rowErrors++;
                    addCapped(errors, error(rowNumber, column.name(), value, "Der Wert ist erforderlich."), errorLimit);
                }
                continue;
            }
            if (column.maxLength() > 0 && value.length() > column.maxLength()) {
                rowErrors++;
                addCapped(errors, error(rowNumber, column.name(), value, "Der Wert ist zu lang."), errorLimit);
            }
            switch (column.type()) {
                case STRING -> { }
                case INTEGER_NON_NEGATIVE -> {
                    if (!NON_NEGATIVE_INTEGER.matcher(value).matches()) {
                        rowErrors++;
                        addCapped(errors, error(rowNumber, column.name(), value, "Der Wert muss eine ganze Zahl größer oder gleich null sein."), errorLimit);
                    }
                }
                case DECIMAL_NON_NEGATIVE -> {
                    if (!NON_NEGATIVE_DECIMAL.matcher(value).matches()) {
                        rowErrors++;
                        addCapped(errors, error(rowNumber, column.name(), value, "Der Wert muss eine Dezimalzahl mit Punkt und größer oder gleich null sein."), errorLimit);
                    } else {
                        new BigDecimal(value);
                    }
                }
                case UTC_INSTANT -> {
                    if (!value.endsWith("Z")) {
                        rowErrors++;
                        addCapped(errors, error(rowNumber, column.name(), value, "Der Zeitstempel muss in UTC mit Suffix Z angegeben werden."), errorLimit);
                    } else {
                        try {
                            Instant.parse(value);
                        } catch (DateTimeParseException exception) {
                            rowErrors++;
                            addCapped(errors, error(rowNumber, column.name(), value, "Der Zeitstempel ist ungültig."), errorLimit);
                        }
                    }
                }
                case MASTER_DATA_CODE -> {
                    var resolved = masterDataResolver.resolveActiveId(column.masterDataKind(), value);
                    if (resolved.isEmpty()) {
                        rowErrors++;
                        addCapped(errors, error(rowNumber, column.name(), value, "Der Stammdatencode ist unbekannt oder inaktiv."), errorLimit);
                    } else {
                        resolvedIds.put(column.name(), resolved.get());
                    }
                }
            }
        }
        return rowErrors;
    }

    private void validateHeader(List<String> actual, CsvTemplate template, List<CsvValidationError> errors) {
        if (actual.size() == 1 && actual.getFirst().contains(";") && template.columns().size() > 1) {
            errors.add(error(1, "header", actual.getFirst(), "Die CSV-Datei muss Kommas als Trennzeichen verwenden."));
            return;
        }
        List<String> expected = template.headerNames();
        for (String expectedColumn : expected) {
            if (!actual.contains(expectedColumn)) {
                errors.add(new CsvValidationError(ErrorCode.CSV_MISSING_COLUMN, 1, expectedColumn, "", "Die erforderliche Spalte fehlt."));
            }
        }
        for (String actualColumn : actual) {
            if (!expected.contains(actualColumn)) {
                errors.add(new CsvValidationError(ErrorCode.CSV_UNKNOWN_COLUMN, 1, actualColumn, actualColumn, "Die Spalte ist nicht erlaubt."));
            }
        }
        if (errors.isEmpty() && !actual.equals(expected)) {
            errors.add(error(1, "header", String.join(",", actual), "Die Reihenfolge der Spalten entspricht nicht der Vorlage."));
        }
    }

    private static CsvValidationResult result(List<CsvRow> rows, List<CsvValidationError> errors, int totalErrorCount, int errorLimit) {
        return new CsvValidationResult(rows, errors.stream().limit(errorLimit).toList(), totalErrorCount, totalErrorCount > errorLimit);
    }

    private static CsvValidationError error(int rowNumber, String columnName, String rejectedValue, String message) {
        return new CsvValidationError(ErrorCode.CSV_VALIDATION_FAILED, rowNumber, columnName, rejectedValue, message);
    }

    private static void addCapped(List<CsvValidationError> errors, CsvValidationError error, int errorLimit) {
        if (errors.size() < errorLimit) {
            errors.add(error);
        }
    }

    private static String decodeUtf8Strict(byte[] content) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(content))
                .toString();
    }

    private static List<String> splitLines(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.endsWith("\n")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            return List.of();
        }
        return List.of(normalized.split("\n", -1));
    }

    private static List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);
            if (quoted) {
                if (currentChar == '"') {
                    if (index + 1 < line.length() && line.charAt(index + 1) == '"') {
                        current.append('"');
                        index++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(currentChar);
                }
            } else if (currentChar == ',') {
                values.add(current.toString());
                current.setLength(0);
            } else if (currentChar == '"') {
                if (!current.isEmpty()) {
                    throw new IllegalArgumentException("Anführungszeichen sind an dieser Position nicht erlaubt.");
                }
                quoted = true;
            } else {
                current.append(currentChar);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Ein Anführungszeichen wurde nicht geschlossen.");
        }
        values.add(current.toString());
        return values;
    }
}