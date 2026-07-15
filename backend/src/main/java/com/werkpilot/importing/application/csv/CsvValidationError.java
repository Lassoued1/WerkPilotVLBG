package com.werkpilot.importing.application.csv;

import com.werkpilot.shared.error.ErrorCode;

public record CsvValidationError(
        ErrorCode errorCode,
        int rowNumber,
        String columnName,
        String rejectedValue,
        String message) {
}