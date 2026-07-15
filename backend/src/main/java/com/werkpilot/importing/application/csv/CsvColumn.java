package com.werkpilot.importing.application.csv;

import com.werkpilot.masterdata.application.MasterDataKind;

public record CsvColumn(
        String name,
        boolean required,
        CsvFieldType type,
        int maxLength,
        MasterDataKind masterDataKind) {

    public CsvColumn {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("CSV column name is required.");
        }
        if (type == null) {
            throw new IllegalArgumentException("CSV column type is required.");
        }
        if (type == CsvFieldType.MASTER_DATA_CODE && masterDataKind == null) {
            throw new IllegalArgumentException("Master-data kind is required for master-data columns.");
        }
    }

    public static CsvColumn requiredString(String name, int maxLength) {
        return new CsvColumn(name, true, CsvFieldType.STRING, maxLength, null);
    }

    public static CsvColumn optionalString(String name, int maxLength) {
        return new CsvColumn(name, false, CsvFieldType.STRING, maxLength, null);
    }

    public static CsvColumn nonNegativeInteger(String name) {
        return new CsvColumn(name, true, CsvFieldType.INTEGER_NON_NEGATIVE, 0, null);
    }

    public static CsvColumn nonNegativeDecimal(String name) {
        return new CsvColumn(name, true, CsvFieldType.DECIMAL_NON_NEGATIVE, 0, null);
    }

    public static CsvColumn utcInstant(String name) {
        return new CsvColumn(name, true, CsvFieldType.UTC_INSTANT, 0, null);
    }

    public static CsvColumn requiredMasterData(String name, MasterDataKind kind) {
        return new CsvColumn(name, true, CsvFieldType.MASTER_DATA_CODE, 80, kind);
    }
}