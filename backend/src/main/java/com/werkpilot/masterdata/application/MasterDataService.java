package com.werkpilot.masterdata.application;

import com.werkpilot.masterdata.application.port.MasterDataPort;
import com.werkpilot.masterdata.application.port.MasterDataRecord;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MasterDataService {

    private final MasterDataPort masterDataPort;

    public MasterDataService(MasterDataPort masterDataPort) {
        this.masterDataPort = masterDataPort;
    }

    @Transactional(readOnly = true)
    public MasterDataPage list(MasterDataKind kind, boolean includeInactive, int page, int size) {
        Page<MasterDataRecord> records = masterDataPort.list(
                kind,
                includeInactive,
                PageRequest.of(page, Math.min(size, 100), Sort.by("code").ascending()));
        return new MasterDataPage(
                records.getContent(),
                records.getNumber(),
                records.getSize(),
                records.getTotalElements(),
                records.getTotalPages());
    }

    @Transactional(readOnly = true)
    public MasterDataRecord get(MasterDataKind kind, UUID id) {
        return masterDataPort.findById(kind, id).orElseThrow(() -> notFound(kind, id));
    }

    @Transactional
    public MasterDataRecord createFactory(String code, String name) {
        String normalizedCode = normalizeCode(code);
        ensureGlobalCodeAvailable(MasterDataKind.FACTORY, normalizedCode, null);
        return masterDataPort.createFactory(normalizedCode, normalizeName(name));
    }

    @Transactional
    public MasterDataRecord updateFactory(UUID id, String code, String name, boolean active) {
        get(MasterDataKind.FACTORY, id);
        String normalizedCode = normalizeCode(code);
        ensureGlobalCodeAvailable(MasterDataKind.FACTORY, normalizedCode, id);
        return masterDataPort.updateFactory(id, normalizedCode, normalizeName(name), active);
    }

    @Transactional
    public MasterDataRecord createLine(UUID factoryId, String code, String name) {
        MasterDataRecord factory = requireActive(MasterDataKind.FACTORY, factoryId);
        String normalizedCode = normalizeCode(code);
        ensureLineCodeAvailable(factory.id(), normalizedCode, null);
        return masterDataPort.createLine(factory.id(), normalizedCode, normalizeName(name));
    }

    @Transactional
    public MasterDataRecord updateLine(UUID id, UUID factoryId, String code, String name, boolean active) {
        get(MasterDataKind.PRODUCTION_LINE, id);
        MasterDataRecord factory = requireActive(MasterDataKind.FACTORY, factoryId);
        String normalizedCode = normalizeCode(code);
        ensureLineCodeAvailable(factory.id(), normalizedCode, id);
        return masterDataPort.updateLine(id, factory.id(), normalizedCode, normalizeName(name), active);
    }

    @Transactional
    public MasterDataRecord createMachine(UUID lineId, String code, String name) {
        MasterDataRecord line = requireActive(MasterDataKind.PRODUCTION_LINE, lineId);
        MasterDataRecord factory = requireActive(MasterDataKind.FACTORY, line.factoryId());
        String normalizedCode = normalizeCode(code);
        ensureMachineCodeAvailable(factory.id(), normalizedCode, null);
        return masterDataPort.createMachine(factory.id(), line.id(), normalizedCode, normalizeName(name));
    }

    @Transactional
    public MasterDataRecord updateMachine(UUID id, UUID lineId, String code, String name, boolean active) {
        get(MasterDataKind.MACHINE, id);
        MasterDataRecord line = requireActive(MasterDataKind.PRODUCTION_LINE, lineId);
        MasterDataRecord factory = requireActive(MasterDataKind.FACTORY, line.factoryId());
        String normalizedCode = normalizeCode(code);
        ensureMachineCodeAvailable(factory.id(), normalizedCode, id);
        return masterDataPort.updateMachine(id, factory.id(), line.id(), normalizedCode, normalizeName(name), active);
    }

    @Transactional
    public MasterDataRecord createProduct(String code, String name, String family) {
        String normalizedCode = normalizeCode(code);
        ensureGlobalCodeAvailable(MasterDataKind.PRODUCT, normalizedCode, null);
        return masterDataPort.createProduct(normalizedCode, normalizeName(name), normalizeOptional(family));
    }

    @Transactional
    public MasterDataRecord updateProduct(UUID id, String code, String name, String family, boolean active) {
        get(MasterDataKind.PRODUCT, id);
        String normalizedCode = normalizeCode(code);
        ensureGlobalCodeAvailable(MasterDataKind.PRODUCT, normalizedCode, id);
        return masterDataPort.updateProduct(id, normalizedCode, normalizeName(name), normalizeOptional(family), active);
    }

    @Transactional
    public MasterDataRecord createShift(String code, String name, LocalTime startTime, LocalTime endTime, int plannedMinutes) {
        String normalizedCode = normalizeCode(code);
        validateShift(startTime, endTime, plannedMinutes);
        ensureGlobalCodeAvailable(MasterDataKind.SHIFT, normalizedCode, null);
        return masterDataPort.createShift(normalizedCode, normalizeName(name), startTime, endTime, plannedMinutes);
    }

    @Transactional
    public MasterDataRecord updateShift(
            UUID id,
            String code,
            String name,
            LocalTime startTime,
            LocalTime endTime,
            int plannedMinutes,
            boolean active) {
        get(MasterDataKind.SHIFT, id);
        String normalizedCode = normalizeCode(code);
        validateShift(startTime, endTime, plannedMinutes);
        ensureGlobalCodeAvailable(MasterDataKind.SHIFT, normalizedCode, id);
        return masterDataPort.updateShift(id, normalizedCode, normalizeName(name), startTime, endTime, plannedMinutes, active);
    }

    @Transactional
    public MasterDataRecord createSimple(MasterDataKind kind, String code, String name) {
        assertSimpleKind(kind);
        String normalizedCode = normalizeCode(code);
        ensureGlobalCodeAvailable(kind, normalizedCode, null);
        return masterDataPort.createSimple(kind, normalizedCode, normalizeName(name));
    }

    @Transactional
    public MasterDataRecord updateSimple(MasterDataKind kind, UUID id, String code, String name, boolean active) {
        assertSimpleKind(kind);
        get(kind, id);
        String normalizedCode = normalizeCode(code);
        ensureGlobalCodeAvailable(kind, normalizedCode, id);
        return masterDataPort.updateSimple(kind, id, normalizedCode, normalizeName(name), active);
    }

    @Transactional
    public void delete(MasterDataKind kind, UUID id) {
        get(kind, id);
        masterDataPort.setActive(kind, id, false);
    }

    private MasterDataRecord requireActive(MasterDataKind kind, UUID id) {
        MasterDataRecord record = get(kind, id);
        if (!record.active()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.BUSINESS_RULE_VIOLATION, kind + " is inactive.");
        }
        return record;
    }

    private void ensureGlobalCodeAvailable(MasterDataKind kind, String code, UUID currentId) {
        masterDataPort.findByCode(kind, code)
                .filter(existing -> !existing.id().equals(currentId))
                .ifPresent(existing -> {
                    throw duplicateCode(kind, code);
                });
    }

    private void ensureLineCodeAvailable(UUID factoryId, String code, UUID currentId) {
        masterDataPort.findLineByFactoryAndCode(factoryId, code)
                .filter(existing -> !existing.id().equals(currentId))
                .ifPresent(existing -> {
                    throw duplicateCode(MasterDataKind.PRODUCTION_LINE, code);
                });
    }

    private void ensureMachineCodeAvailable(UUID factoryId, String code, UUID currentId) {
        masterDataPort.findMachineByFactoryAndCode(factoryId, code)
                .filter(existing -> !existing.id().equals(currentId))
                .ifPresent(existing -> {
                    throw duplicateCode(MasterDataKind.MACHINE, code);
                });
    }

    private static void validateShift(LocalTime startTime, LocalTime endTime, int plannedMinutes) {
        if (startTime == null || endTime == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Shift times are required.");
        }
        if (plannedMinutes <= 0 || plannedMinutes > 1440) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Planned minutes must be between 1 and 1440.");
        }
    }

    private static void assertSimpleKind(MasterDataKind kind) {
        if (kind != MasterDataKind.DOWNTIME_REASON && kind != MasterDataKind.SCRAP_CATEGORY) {
            throw new IllegalArgumentException("Unsupported simple master-data kind: " + kind);
        }
    }

    private static String normalizeCode(String code) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Code is required.");
        }
        return normalized;
    }

    private static String normalizeName(String name) {
        String normalized = name.trim();
        if (normalized.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "Name is required.");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static ApiException notFound(MasterDataKind kind, UUID id) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, kind + " was not found: " + id);
    }

    private static ApiException duplicateCode(MasterDataKind kind, String code) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.BUSINESS_RULE_VIOLATION, kind + " code already exists: " + code);
    }

    public record MasterDataPage(
            List<MasterDataRecord> items,
            int page,
            int size,
            long totalElements,
            int totalPages) {

        public MasterDataPage {
            items = List.copyOf(items);
        }
    }
}
