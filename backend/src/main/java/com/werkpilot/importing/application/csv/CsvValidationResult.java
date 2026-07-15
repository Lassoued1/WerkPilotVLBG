package com.werkpilot.importing.application.csv;

import java.util.List;

public record CsvValidationResult(
        List<CsvRow> rows,
        List<CsvValidationError> errors,
        int totalErrorCount,
        boolean errorOverflow) {

    public CsvValidationResult {
        rows = List.copyOf(rows);
        errors = List.copyOf(errors);
    }

    public boolean valid() {
        return totalErrorCount == 0;
    }
}