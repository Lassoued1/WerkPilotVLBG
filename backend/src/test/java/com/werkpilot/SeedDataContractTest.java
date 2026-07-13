package com.werkpilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.werkpilot.shared.seed.SeedDataContract;
import com.werkpilot.shared.seed.SeedReference;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SeedDataContractTest {

    private final SeedDataContract seedDataContract = new SeedDataContract();

    @Test
    void seedReferencesMatchDocumentedDemoFixtureContract() {
        Set<String> references = seedDataContract.references().stream()
                .map(reference -> reference.kind() + ":" + reference.code() + ":" + reference.label())
                .collect(Collectors.toSet());

        assertEquals(11, references.size());
        assertTrue(references.contains("FACTORY:VLBG:VLBG Demo Plant"));
        assertTrue(references.contains("LINE:L-2:Machining"));
        assertTrue(references.contains("MACHINE:M-04:Machine M-04"));
        assertTrue(references.contains("MACHINE:M-07:Machine M-07"));
        assertTrue(references.contains("PRODUCT:P-03:Product P-03"));
        assertTrue(references.contains("SHIFT:MORNING:Morning"));
        assertTrue(references.contains("SHIFT:AFTERNOON:Afternoon"));
        assertTrue(references.contains("SHIFT:NIGHT:Night"));
        assertTrue(references.contains("DOWNTIME_REASON:TOOL_CHANGE:Tool change"));
        assertTrue(references.contains("DOWNTIME_REASON:MATERIAL_WAIT:Material wait"));
        assertTrue(references.contains("SCRAP_CATEGORY:DIMENSIONAL_DEFECT:Dimensional defect"));
    }

    @Test
    void everySeedReferenceHasKindCodeAndLabel() {
        for (SeedReference reference : seedDataContract.references()) {
            assertNotNull(reference.kind());
            assertFalseBlank(reference.code());
            assertFalseBlank(reference.label());
        }
    }

    private static void assertFalseBlank(String value) {
        assertTrue(value != null && !value.isBlank());
    }
}
