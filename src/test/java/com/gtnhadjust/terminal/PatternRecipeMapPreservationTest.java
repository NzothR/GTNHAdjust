package com.gtnhadjust.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PatternRecipeMapPreservationTest {

    @Test
    public void preservesOnlyMatchingExistingPatternAndHonorsNewTransfers() {
        assertEquals("assembler", PatternRecipeMapPreservation.select(null, "assembler", "assembler"));
        assertEquals("mixer", PatternRecipeMapPreservation.select("mixer", "mixer", "assembler"));
        assertNull(PatternRecipeMapPreservation.select(null, null, "assembler"));
        assertNull(PatternRecipeMapPreservation.select(null, "mixer", "assembler"));
        assertNull(PatternRecipeMapPreservation.select(null, "assembler", null));
    }
}
