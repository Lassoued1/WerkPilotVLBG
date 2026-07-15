package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.werkpilot.importing.application.csv.CsvColumn;
import com.werkpilot.importing.application.csv.CsvMasterDataResolver;
import com.werkpilot.importing.application.csv.CsvTemplate;
import com.werkpilot.importing.application.csv.CsvValidationResult;
import com.werkpilot.importing.application.csv.StrictCsvParserService;
import com.werkpilot.masterdata.application.MasterDataKind;
import com.werkpilot.shared.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CsvTemplateValidationTest {

    private final UUID machineId = UUID.randomUUID();
    private final StrictCsvParserService parser = new StrictCsvParserService(new FakeResolver()
            .active(MasterDataKind.MACHINE, "M-01", machineId));

    @Test
    void acceptsStrictUtf8CommaSeparatedTemplateAndResolvesMasterData() {
        CsvValidationResult result = parser.validate(bytes("""
                period_start,machine_code,units_produced,energy_kwh
                2026-07-01T08:00:00Z,M-01,42,12.500
                """), template());

        assertThat(result.valid()).isTrue();
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().getFirst().rowNumber()).isEqualTo(2);
        assertThat(result.rows().getFirst().resolvedMasterDataIds()).containsEntry("machine_code", machineId);
        assertThat(result.rows().getFirst().values()).containsEntry("energy_kwh", "12.500");
    }

    @Test
    void rejectsMalformedUtf8BeforeParsingRows() {
        CsvValidationResult result = parser.validate(new byte[] {(byte) 0xC3, 0x28}, template());

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.errorCode()).isEqualTo(ErrorCode.CSV_VALIDATION_FAILED);
            assertThat(error.rowNumber()).isEqualTo(1);
            assertThat(error.message()).contains("UTF-8");
        });
    }

    @Test
    void rejectsSemicolonDelimiterAndNonExactHeaders() {
        CsvValidationResult semicolon = parser.validate(bytes("""
                period_start;machine_code;units_produced;energy_kwh
                2026-07-01T08:00:00Z;M-01;42;12.500
                """), template());

        assertThat(semicolon.errors()).singleElement().satisfies(error -> {
            assertThat(error.rowNumber()).isEqualTo(1);
            assertThat(error.message()).contains("Kommas");
        });

        CsvValidationResult missingAndUnknown = parser.validate(bytes("""
                period_start,machine_code,unknown_column
                2026-07-01T08:00:00Z,M-01,x
                """), template());

        assertThat(missingAndUnknown.errors()).extracting("errorCode")
                .contains(ErrorCode.CSV_MISSING_COLUMN, ErrorCode.CSV_UNKNOWN_COLUMN);

        CsvValidationResult wrongOrder = parser.validate(bytes("""
                machine_code,period_start,units_produced,energy_kwh
                M-01,2026-07-01T08:00:00Z,42,12.500
                """), template());

        assertThat(wrongOrder.errors()).singleElement()
                .satisfies(error -> assertThat(error.message()).contains("Reihenfolge"));
    }

    @Test
    void rejectsInvalidRowsWithGermanDetails() {
        CsvValidationResult result = parser.validate(bytes("""
                period_start,machine_code,units_produced,energy_kwh
                2026-07-01T08:00:00+02:00,UNKNOWN,-1,"12,5"
                """), template());

        assertThat(result.valid()).isFalse();
        assertThat(result.totalErrorCount()).isEqualTo(4);
        assertThat(result.errors()).extracting("rowNumber").containsOnly(2);
        assertThat(result.errors()).extracting("columnName")
                .contains("period_start", "machine_code", "units_produced", "energy_kwh");
        assertThat(result.errors()).extracting("message")
                .allSatisfy(message -> assertThat((String) message).containsAnyOf("Der", "Die"));
        assertThat(result.rows()).isEmpty();
    }

    @Test
    void capsRecordedErrorsAndKeepsOverflowMetadataAccurate() {
        String rows = IntStream.rangeClosed(1, 501)
                .mapToObj(i -> "2026-07-01T08:00:00Z,M-01,-1,12.500")
                .collect(Collectors.joining("\n"));

        CsvValidationResult result = parser.validate(bytes("period_start,machine_code,units_produced,energy_kwh\n" + rows), template(), 500);

        assertThat(result.valid()).isFalse();
        assertThat(result.totalErrorCount()).isEqualTo(501);
        assertThat(result.errors()).hasSize(500);
        assertThat(result.errorOverflow()).isTrue();
    }

    @Test
    void enforcesBoundedProcessingRows() {
        CsvTemplate tinyTemplate = new CsvTemplate("tiny", template().columns(), 1);

        CsvValidationResult result = parser.validate(bytes("""
                period_start,machine_code,units_produced,energy_kwh
                2026-07-01T08:00:00Z,M-01,1,12.500
                2026-07-01T09:00:00Z,M-01,2,13.500
                """), tinyTemplate);

        assertThat(result.totalErrorCount()).isEqualTo(1);
        assertThat(result.errors()).singleElement()
                .satisfies(error -> assertThat(error.message()).contains("zu viele Datenzeilen"));
        assertThat(result.rows()).hasSize(1);
    }

    private static CsvTemplate template() {
        return new CsvTemplate("production-like", java.util.List.of(
                CsvColumn.utcInstant("period_start"),
                CsvColumn.requiredMasterData("machine_code", MasterDataKind.MACHINE),
                CsvColumn.nonNegativeInteger("units_produced"),
                CsvColumn.nonNegativeDecimal("energy_kwh")), 100_000);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static final class FakeResolver implements CsvMasterDataResolver {
        private final Map<String, UUID> active = new HashMap<>();

        FakeResolver active(MasterDataKind kind, String code, UUID id) {
            active.put(key(kind, code), id);
            return this;
        }

        @Override
        public Optional<UUID> resolveActiveId(MasterDataKind kind, String code) {
            return Optional.ofNullable(active.get(key(kind, code)));
        }

        private static String key(MasterDataKind kind, String code) {
            return kind.name() + ":" + code;
        }
    }
}