package com.werkpilot.shared.seed;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SeedDataContract {

    private static final List<SeedReference> REFERENCES = List.of(
            new SeedReference(SeedReferenceKind.FACTORY, "VLBG", "VLBG Demo Plant"),
            new SeedReference(SeedReferenceKind.LINE, "L-2", "Machining"),
            new SeedReference(SeedReferenceKind.MACHINE, "M-04", "Machine M-04"),
            new SeedReference(SeedReferenceKind.MACHINE, "M-07", "Machine M-07"),
            new SeedReference(SeedReferenceKind.PRODUCT, "P-03", "Product P-03"),
            new SeedReference(SeedReferenceKind.SHIFT, "MORNING", "Morning"),
            new SeedReference(SeedReferenceKind.SHIFT, "AFTERNOON", "Afternoon"),
            new SeedReference(SeedReferenceKind.SHIFT, "NIGHT", "Night"),
            new SeedReference(SeedReferenceKind.DOWNTIME_REASON, "TOOL_CHANGE", "Tool change"),
            new SeedReference(SeedReferenceKind.DOWNTIME_REASON, "MATERIAL_WAIT", "Material wait"),
            new SeedReference(SeedReferenceKind.SCRAP_CATEGORY, "DIMENSIONAL_DEFECT", "Dimensional defect"));

    public List<SeedReference> references() {
        return REFERENCES;
    }
}
