package com.gtnhadjust.terminal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PatternEncodingResultTest {

    @Test
    public void uploadsOnlyANewlyEncodedStack() {
        Object oldPattern = new Object();
        Object newPattern = new Object();
        assertFalse(PatternEncodingResult.isNew(null, null));
        assertFalse(PatternEncodingResult.isNew(oldPattern, null));
        assertFalse(PatternEncodingResult.isNew(oldPattern, oldPattern));
        assertTrue(PatternEncodingResult.isNew(null, newPattern));
        assertTrue(PatternEncodingResult.isNew(oldPattern, newPattern));
    }
}
