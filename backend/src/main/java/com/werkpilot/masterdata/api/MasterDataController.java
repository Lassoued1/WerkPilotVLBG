package com.werkpilot.masterdata.api;

import com.werkpilot.masterdata.application.MasterDataKind;
import com.werkpilot.masterdata.application.MasterDataService;
import com.werkpilot.masterdata.application.MasterDataService.MasterDataPage;
import com.werkpilot.masterdata.application.port.MasterDataRecord;
import com.werkpilot.shared.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping("/factories")
    PageResponse<FactoryResponse> listFactories(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        MasterDataPage records = masterDataService.list(MasterDataKind.FACTORY, includeInactive, page, size);
        return page(records, records.items().stream().map(this::factory).toList());
    }

    @PostMapping("/factories")
    ResponseEntity<FactoryResponse> createFactory(@Valid @RequestBody NamedRequest request) {
        FactoryResponse created = factory(masterDataService.createFactory(request.code(), request.name()));
        return ResponseEntity.created(URI.create("/factories/" + created.id())).body(created);
    }

    @GetMapping("/factories/{id}")
    FactoryResponse getFactory(@PathVariable UUID id) {
        return factory(masterDataService.get(MasterDataKind.FACTORY, id));
    }

    @PutMapping("/factories/{id}")
    FactoryResponse updateFactory(@PathVariable UUID id, @Valid @RequestBody NamedUpdateRequest request) {
        return factory(masterDataService.updateFactory(id, request.code(), request.name(), request.active()));
    }

    @DeleteMapping("/factories/{id}")
    ResponseEntity<Void> deleteFactory(@PathVariable UUID id) {
        masterDataService.delete(MasterDataKind.FACTORY, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/production-lines")
    PageResponse<ProductionLineResponse> listProductionLines(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        MasterDataPage records = masterDataService.list(MasterDataKind.PRODUCTION_LINE, includeInactive, page, size);
        return page(records, records.items().stream().map(this::line).toList());
    }

    @PostMapping("/production-lines")
    ResponseEntity<ProductionLineResponse> createProductionLine(@Valid @RequestBody ProductionLineRequest request) {
        ProductionLineResponse created = line(masterDataService.createLine(request.factoryId(), request.code(), request.name()));
        return ResponseEntity.created(URI.create("/production-lines/" + created.id())).body(created);
    }

    @GetMapping("/production-lines/{id}")
    ProductionLineResponse getProductionLine(@PathVariable UUID id) {
        return line(masterDataService.get(MasterDataKind.PRODUCTION_LINE, id));
    }

    @PutMapping("/production-lines/{id}")
    ProductionLineResponse updateProductionLine(@PathVariable UUID id, @Valid @RequestBody ProductionLineUpdateRequest request) {
        return line(masterDataService.updateLine(id, request.factoryId(), request.code(), request.name(), request.active()));
    }

    @DeleteMapping("/production-lines/{id}")
    ResponseEntity<Void> deleteProductionLine(@PathVariable UUID id) {
        masterDataService.delete(MasterDataKind.PRODUCTION_LINE, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/machines")
    PageResponse<MachineResponse> listMachines(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        MasterDataPage records = masterDataService.list(MasterDataKind.MACHINE, includeInactive, page, size);
        return page(records, records.items().stream().map(this::machine).toList());
    }

    @PostMapping("/machines")
    ResponseEntity<MachineResponse> createMachine(@Valid @RequestBody MachineRequest request) {
        MachineResponse created = machine(masterDataService.createMachine(request.lineId(), request.code(), request.name()));
        return ResponseEntity.created(URI.create("/machines/" + created.id())).body(created);
    }

    @GetMapping("/machines/{id}")
    MachineResponse getMachine(@PathVariable UUID id) {
        return machine(masterDataService.get(MasterDataKind.MACHINE, id));
    }

    @PutMapping("/machines/{id}")
    MachineResponse updateMachine(@PathVariable UUID id, @Valid @RequestBody MachineUpdateRequest request) {
        return machine(masterDataService.updateMachine(id, request.lineId(), request.code(), request.name(), request.active()));
    }

    @DeleteMapping("/machines/{id}")
    ResponseEntity<Void> deleteMachine(@PathVariable UUID id) {
        masterDataService.delete(MasterDataKind.MACHINE, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products")
    PageResponse<ProductResponse> listProducts(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        MasterDataPage records = masterDataService.list(MasterDataKind.PRODUCT, includeInactive, page, size);
        return page(records, records.items().stream().map(this::product).toList());
    }

    @PostMapping("/products")
    ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = product(masterDataService.createProduct(request.code(), request.name(), request.family()));
        return ResponseEntity.created(URI.create("/products/" + created.id())).body(created);
    }

    @GetMapping("/products/{id}")
    ProductResponse getProduct(@PathVariable UUID id) {
        return product(masterDataService.get(MasterDataKind.PRODUCT, id));
    }

    @PutMapping("/products/{id}")
    ProductResponse updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest request) {
        return product(masterDataService.updateProduct(id, request.code(), request.name(), request.family(), request.active()));
    }

    @DeleteMapping("/products/{id}")
    ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        masterDataService.delete(MasterDataKind.PRODUCT, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/shifts")
    PageResponse<ShiftResponse> listShifts(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        MasterDataPage records = masterDataService.list(MasterDataKind.SHIFT, includeInactive, page, size);
        return page(records, records.items().stream().map(this::shift).toList());
    }

    @PostMapping("/shifts")
    ResponseEntity<ShiftResponse> createShift(@Valid @RequestBody ShiftRequest request) {
        ShiftResponse created = shift(masterDataService.createShift(
                request.code(),
                request.name(),
                request.startTime(),
                request.endTime(),
                request.plannedMinutes()));
        return ResponseEntity.created(URI.create("/shifts/" + created.id())).body(created);
    }

    @GetMapping("/shifts/{id}")
    ShiftResponse getShift(@PathVariable UUID id) {
        return shift(masterDataService.get(MasterDataKind.SHIFT, id));
    }

    @PutMapping("/shifts/{id}")
    ShiftResponse updateShift(@PathVariable UUID id, @Valid @RequestBody ShiftUpdateRequest request) {
        return shift(masterDataService.updateShift(
                id,
                request.code(),
                request.name(),
                request.startTime(),
                request.endTime(),
                request.plannedMinutes(),
                request.active()));
    }

    @DeleteMapping("/shifts/{id}")
    ResponseEntity<Void> deleteShift(@PathVariable UUID id) {
        masterDataService.delete(MasterDataKind.SHIFT, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/downtime-reasons")
    PageResponse<SimpleResponse> listDowntimeReasons(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        MasterDataPage records = masterDataService.list(MasterDataKind.DOWNTIME_REASON, includeInactive, page, size);
        return page(records, records.items().stream().map(this::simple).toList());
    }

    @PostMapping("/downtime-reasons")
    ResponseEntity<SimpleResponse> createDowntimeReason(@Valid @RequestBody NamedRequest request) {
        SimpleResponse created = simple(masterDataService.createSimple(MasterDataKind.DOWNTIME_REASON, request.code(), request.name()));
        return ResponseEntity.created(URI.create("/downtime-reasons/" + created.id())).body(created);
    }

    @GetMapping("/downtime-reasons/{id}")
    SimpleResponse getDowntimeReason(@PathVariable UUID id) {
        return simple(masterDataService.get(MasterDataKind.DOWNTIME_REASON, id));
    }

    @PutMapping("/downtime-reasons/{id}")
    SimpleResponse updateDowntimeReason(@PathVariable UUID id, @Valid @RequestBody NamedUpdateRequest request) {
        return simple(masterDataService.updateSimple(
                MasterDataKind.DOWNTIME_REASON,
                id,
                request.code(),
                request.name(),
                request.active()));
    }

    @DeleteMapping("/downtime-reasons/{id}")
    ResponseEntity<Void> deleteDowntimeReason(@PathVariable UUID id) {
        masterDataService.delete(MasterDataKind.DOWNTIME_REASON, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/scrap-categories")
    PageResponse<SimpleResponse> listScrapCategories(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        MasterDataPage records = masterDataService.list(MasterDataKind.SCRAP_CATEGORY, includeInactive, page, size);
        return page(records, records.items().stream().map(this::simple).toList());
    }

    @PostMapping("/scrap-categories")
    ResponseEntity<SimpleResponse> createScrapCategory(@Valid @RequestBody NamedRequest request) {
        SimpleResponse created = simple(masterDataService.createSimple(MasterDataKind.SCRAP_CATEGORY, request.code(), request.name()));
        return ResponseEntity.created(URI.create("/scrap-categories/" + created.id())).body(created);
    }

    @GetMapping("/scrap-categories/{id}")
    SimpleResponse getScrapCategory(@PathVariable UUID id) {
        return simple(masterDataService.get(MasterDataKind.SCRAP_CATEGORY, id));
    }

    @PutMapping("/scrap-categories/{id}")
    SimpleResponse updateScrapCategory(@PathVariable UUID id, @Valid @RequestBody NamedUpdateRequest request) {
        return simple(masterDataService.updateSimple(
                MasterDataKind.SCRAP_CATEGORY,
                id,
                request.code(),
                request.name(),
                request.active()));
    }

    @DeleteMapping("/scrap-categories/{id}")
    ResponseEntity<Void> deleteScrapCategory(@PathVariable UUID id) {
        masterDataService.delete(MasterDataKind.SCRAP_CATEGORY, id);
        return ResponseEntity.noContent().build();
    }

    private static <T> PageResponse<T> page(MasterDataPage records, List<T> items) {
        return new PageResponse<>(items, records.page(), records.size(), records.totalElements(), records.totalPages());
    }

    private FactoryResponse factory(MasterDataRecord record) {
        return new FactoryResponse(
                record.id(),
                record.code(),
                record.name(),
                record.active(),
                record.createdAt(),
                record.updatedAt());
    }

    private ProductionLineResponse line(MasterDataRecord record) {
        return new ProductionLineResponse(
                record.id(),
                record.factoryId(),
                record.code(),
                record.name(),
                record.active(),
                record.createdAt(),
                record.updatedAt());
    }

    private MachineResponse machine(MasterDataRecord record) {
        return new MachineResponse(
                record.id(),
                record.factoryId(),
                record.lineId(),
                record.code(),
                record.name(),
                record.active(),
                record.createdAt(),
                record.updatedAt());
    }

    private ProductResponse product(MasterDataRecord record) {
        return new ProductResponse(
                record.id(),
                record.code(),
                record.name(),
                record.family(),
                record.active(),
                record.createdAt(),
                record.updatedAt());
    }

    private ShiftResponse shift(MasterDataRecord record) {
        return new ShiftResponse(
                record.id(),
                record.code(),
                record.name(),
                record.startTime(),
                record.endTime(),
                record.plannedMinutes(),
                record.active(),
                record.createdAt(),
                record.updatedAt());
    }

    private SimpleResponse simple(MasterDataRecord record) {
        return new SimpleResponse(
                record.id(),
                record.code(),
                record.name(),
                record.active(),
                record.createdAt(),
                record.updatedAt());
    }

    public record NamedRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 160) String name) {
    }

    public record NamedUpdateRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 160) String name,
            boolean active) {
    }

    public record ProductionLineRequest(
            @NotNull UUID factoryId,
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 160) String name) {
    }

    public record ProductionLineUpdateRequest(
            @NotNull UUID factoryId,
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 160) String name,
            boolean active) {
    }

    public record MachineRequest(
            @NotNull UUID lineId,
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 160) String name) {
    }

    public record MachineUpdateRequest(
            @NotNull UUID lineId,
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 160) String name,
            boolean active) {
    }

    public record ProductRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 120) String family) {
    }

    public record ProductUpdateRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 120) String family,
            boolean active) {
    }

    public record ShiftRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 160) String name,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @Min(1) @Max(1440) int plannedMinutes) {
    }

    public record ShiftUpdateRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 160) String name,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime,
            @Min(1) @Max(1440) int plannedMinutes,
            boolean active) {
    }

    public record FactoryResponse(
            UUID id,
            String code,
            String name,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ProductionLineResponse(
            UUID id,
            UUID factoryId,
            String code,
            String name,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record MachineResponse(
            UUID id,
            UUID factoryId,
            UUID lineId,
            String code,
            String name,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ProductResponse(
            UUID id,
            String code,
            String name,
            String family,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ShiftResponse(
            UUID id,
            String code,
            String name,
            LocalTime startTime,
            LocalTime endTime,
            int plannedMinutes,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record SimpleResponse(
            UUID id,
            String code,
            String name,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {
    }
}
