package com.werkpilot.importing.application.csv;

import com.werkpilot.masterdata.application.MasterDataKind;
import com.werkpilot.masterdata.application.port.MasterDataPort;
import com.werkpilot.masterdata.application.port.MasterDataRecord;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-import memoization of master-data lookups. A 100k-row file repeats the
 * same handful of codes, so resolving each code once (including misses) keeps
 * the import inside the NFR-03 budget without changing resolution semantics.
 */
public final class ImportMasterDataLookup {

    private final MasterDataPort masterDataPort;
    private final Map<String, Optional<MasterDataRecord>> byCode = new HashMap<>();
    private final Map<String, Optional<MasterDataRecord>> linesByFactory = new HashMap<>();
    private final Map<String, Optional<MasterDataRecord>> machinesByFactory = new HashMap<>();
    private final Map<String, List<MasterDataRecord>> machinesByCode = new HashMap<>();

    public ImportMasterDataLookup(MasterDataPort masterDataPort) {
        this.masterDataPort = masterDataPort;
    }

    public Optional<MasterDataRecord> byCode(MasterDataKind kind, String code) {
        return byCode.computeIfAbsent(kind.name() + "|" + code, key -> masterDataPort.findByCode(kind, code));
    }

    public Optional<MasterDataRecord> lineByFactoryAndCode(UUID factoryId, String code) {
        return linesByFactory.computeIfAbsent(factoryId + "|" + code, key -> masterDataPort.findLineByFactoryAndCode(factoryId, code));
    }

    public Optional<MasterDataRecord> machineByFactoryAndCode(UUID factoryId, String code) {
        return machinesByFactory.computeIfAbsent(factoryId + "|" + code, key -> masterDataPort.findMachineByFactoryAndCode(factoryId, code));
    }

    public List<MasterDataRecord> machinesByCode(String code) {
        return machinesByCode.computeIfAbsent(code, masterDataPort::findMachinesByCode);
    }
}
