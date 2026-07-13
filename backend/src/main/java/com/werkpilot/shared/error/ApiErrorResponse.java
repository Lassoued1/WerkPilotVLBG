package com.werkpilot.shared.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        ErrorCode errorCode,
        String message,
        String path,
        List<ErrorDetail> details) {

    public ApiErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
