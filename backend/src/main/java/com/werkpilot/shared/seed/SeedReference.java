package com.werkpilot.shared.seed;

public record SeedReference(
        SeedReferenceKind kind,
        String code,
        String label) {
}
