package com.werkpilot.quality.application.port;

import java.util.List;

public interface ScrapRecordPort {

    void insertAll(List<ScrapRecordDraft> records);
}
