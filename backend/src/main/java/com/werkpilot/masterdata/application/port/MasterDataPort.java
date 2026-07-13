package com.werkpilot.masterdata.application.port;

import com.werkpilot.masterdata.application.MasterDataKind;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MasterDataPort {

    Page<MasterDataRecord> list(MasterDataKind kind, boolean includeInactive, Pageable pageable);

    Optional<MasterDataRecord> findById(MasterDataKind kind, UUID id);

    Optional<MasterDataRecord> findByCode(MasterDataKind kind, String code);

    Optional<MasterDataRecord> findLineByFactoryAndCode(UUID factoryId, String code);

    Optional<MasterDataRecord> findMachineByFactoryAndCode(UUID factoryId, String code);

    List<MasterDataRecord> findMachinesByFactoryAndCode(UUID factoryId, String code);

    MasterDataRecord createFactory(String code, String name);

    MasterDataRecord createLine(UUID factoryId, String code, String name);

    MasterDataRecord createMachine(UUID factoryId, UUID lineId, String code, String name);

    MasterDataRecord createProduct(String code, String name, String family);

    MasterDataRecord createShift(String code, String name, LocalTime startTime, LocalTime endTime, int plannedMinutes);

    MasterDataRecord createSimple(MasterDataKind kind, String code, String name);

    MasterDataRecord updateFactory(UUID id, String code, String name, boolean active);

    MasterDataRecord updateLine(UUID id, UUID factoryId, String code, String name, boolean active);

    MasterDataRecord updateMachine(UUID id, UUID factoryId, UUID lineId, String code, String name, boolean active);

    MasterDataRecord updateProduct(UUID id, String code, String name, String family, boolean active);

    MasterDataRecord updateShift(UUID id, String code, String name, LocalTime startTime, LocalTime endTime, int plannedMinutes, boolean active);

    MasterDataRecord updateSimple(MasterDataKind kind, UUID id, String code, String name, boolean active);

    void setActive(MasterDataKind kind, UUID id, boolean active);
}
