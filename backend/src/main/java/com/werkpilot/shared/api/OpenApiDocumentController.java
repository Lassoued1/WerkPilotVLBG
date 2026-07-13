package com.werkpilot.shared.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OpenApiDocumentController {

    @GetMapping("/v3/api-docs")
    Map<String, Object> openApiDocument() {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("openapi", "3.1.0");
        document.put("info", Map.of(
                "title", "WerkPilot VLBG API",
                "version", "0.0.1-SNAPSHOT"));
        document.put("servers", List.of(Map.of("url", "/api/v1")));
        document.put("paths", new LinkedHashMap<>());
        document.put("components", Map.of("schemas", schemas()));
        return document;
    }

    private Map<String, Object> schemas() {
        Map<String, Object> schemas = new LinkedHashMap<>();
        schemas.put("ApiErrorResponse", objectSchema(Map.of(
                "timestamp", stringSchema("date-time"),
                "status", integerSchema(),
                "errorCode", enumSchema(List.of(
                        "AUTH_INVALID_CREDENTIALS",
                        "AUTH_TOKEN_EXPIRED",
                        "ACCESS_DENIED",
                        "RESOURCE_NOT_FOUND",
                        "VALIDATION_FAILED",
                        "CSV_MISSING_COLUMN",
                        "CSV_UNKNOWN_COLUMN",
                        "CSV_VALIDATION_FAILED",
                        "IMPORT_DUPLICATE_FILE",
                        "BUSINESS_RULE_VIOLATION",
                        "INTERNAL_ERROR")),
                "message", stringSchema(null),
                "path", stringSchema(null),
                "details", arraySchema(refSchema("ErrorDetail")))));
        schemas.put("ErrorDetail", objectSchema(Map.of(
                "row", integerSchema(),
                "column", stringSchema(null),
                "value", stringSchema(null),
                "message", stringSchema(null))));
        schemas.put("PageResponse", objectSchema(Map.of(
                "items", arraySchema(Map.of()),
                "page", integerSchema(),
                "size", integerSchema(),
                "totalElements", integerSchema(),
                "totalPages", integerSchema())));
        schemas.put("AggregateValue", objectSchema(Map.of(
                "value", Map.of("type", List.of("number", "null")),
                "unit", stringSchema(null),
                "available", Map.of("type", "boolean"),
                "reason", Map.of("type", List.of("string", "null")))));
        schemas.put("FilterCriteria", objectSchema(properties(
                "from", nullableStringSchema("date-time"),
                "to", nullableStringSchema("date-time"),
                "factoryId", nullableStringSchema("uuid"),
                "lineId", nullableStringSchema("uuid"),
                "machineId", nullableStringSchema("uuid"),
                "productId", nullableStringSchema("uuid"),
                "shiftId", nullableStringSchema("uuid"),
                "anomalyType", nullableStringSchema(null),
                "severity", nullableStringSchema(null),
                "anomalyStatus", nullableStringSchema(null),
                "ticketStatus", nullableStringSchema(null),
                "priority", nullableStringSchema(null),
                "assigneeId", nullableStringSchema("uuid"),
                "reasonId", nullableStringSchema("uuid"),
                "scrapCategoryId", nullableStringSchema("uuid"))));
        schemas.put("JobResponse", objectSchema(Map.of(
                "jobId", stringSchema("uuid"),
                "status", enumSchema(List.of("PROCESSING", "COMMITTED", "FAILED", "SUPERSEDED")),
                "createdAt", stringSchema("date-time"),
                "completedAt", nullableStringSchema("date-time"))));
        return schemas;
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties) {
        return Map.of("type", "object", "properties", properties);
    }

    private Map<String, Object> properties(Object... entries) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            properties.put((String) entries[index], entries[index + 1]);
        }
        return properties;
    }

    private Map<String, Object> stringSchema(String format) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        if (format != null) {
            schema.put("format", format);
        }
        return schema;
    }

    private Map<String, Object> nullableStringSchema(String format) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", List.of("string", "null"));
        if (format != null) {
            schema.put("format", format);
        }
        return schema;
    }

    private Map<String, Object> integerSchema() {
        return Map.of("type", "integer", "format", "int32");
    }

    private Map<String, Object> arraySchema(Map<String, Object> items) {
        return Map.of("type", "array", "items", items);
    }

    private Map<String, Object> enumSchema(List<String> values) {
        return Map.of("type", "string", "enum", values);
    }

    private Map<String, Object> refSchema(String schemaName) {
        return Map.of("$ref", "#/components/schemas/" + schemaName);
    }
}
