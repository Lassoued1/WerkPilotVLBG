package com.werkpilot.shared.error;

import java.util.List;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode errorCode;
    private final List<ErrorDetail> details;

    public ApiException(HttpStatus status, ErrorCode errorCode, String message) {
        this(status, errorCode, message, List.of());
    }

    public ApiException(HttpStatus status, ErrorCode errorCode, String message, List<ErrorDetail> details) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = List.copyOf(details);
    }

    public HttpStatus status() {
        return status;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<ErrorDetail> details() {
        return details;
    }
}
