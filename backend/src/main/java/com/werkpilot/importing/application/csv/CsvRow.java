package com.werkpilot.importing.application.csv;

import java.util.Map;
import java.util.UUID;

public record CsvRow(
        int rowNumber,
        Map<String, String> values,
        Map<String, UUID> resolvedMasterDataIds) {

    public CsvRow {
        values = Map.copyOf(values);
        resolvedMasterDataIds = Map.copyOf(resolvedMasterDataIds);
    }
}