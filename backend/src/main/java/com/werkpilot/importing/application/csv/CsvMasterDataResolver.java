package com.werkpilot.importing.application.csv;

import com.werkpilot.masterdata.application.MasterDataKind;
import java.util.Optional;
import java.util.UUID;

public interface CsvMasterDataResolver {

    Optional<UUID> resolveActiveId(MasterDataKind kind, String code);
}