package com.werkpilot.shared.error;

public record ErrorDetail(
        Integer row,
        String column,
        String value,
        String message) {
}
