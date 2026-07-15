package com.werkpilot.importing.application.csv;

import com.werkpilot.masterdata.application.MasterDataKind;
import com.werkpilot.masterdata.application.port.MasterDataPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PortBackedCsvMasterDataResolver implements CsvMasterDataResolver {

    private final MasterDataPort masterDataPort;

    public PortBackedCsvMasterDataResolver(MasterDataPort masterDataPort) {
        this.masterDataPort = masterDataPort;
    }

    @Override
    public Optional<UUID> resolveActiveId(MasterDataKind kind, String code) {
        return masterDataPort.findByCode(kind, code)
                .filter(record -> record.active())
                .map(record -> record.id());
    }
}