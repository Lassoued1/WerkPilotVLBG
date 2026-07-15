package com.werkpilot.importing.application.csv;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record CsvTemplate(String name, List<CsvColumn> columns, int maxRows) {

    public CsvTemplate {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("CSV template name is required.");
        }
        columns = List.copyOf(columns);
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("CSV template columns are required.");
        }
        if (maxRows < 1) {
            throw new IllegalArgumentException("CSV template maxRows must be positive.");
        }
        Set<String> seen = new HashSet<>();
        for (CsvColumn column : columns) {
            if (!seen.add(column.name())) {
                throw new IllegalArgumentException("Duplicate CSV column: " + column.name());
            }
        }
    }

    public List<String> headerNames() {
        return columns.stream().map(CsvColumn::name).toList();
    }
}