package com.werkpilot.analytics.application;

import java.util.List;

public record ProductionRecordPage(
        List<ProductionRecordView> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public ProductionRecordPage {
        items = List.copyOf(items);
    }
}
