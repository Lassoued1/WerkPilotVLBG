package com.werkpilot.downtime.application.port;

import java.util.List;

public interface DowntimeRecordPort {

    void insertAll(List<DowntimeRecordDraft> records);
}
