package com.werkpilot.production.application.port;

import java.util.List;

public interface ProductionRecordPort {

    void insertAll(List<ProductionRecordDraft> records);
}