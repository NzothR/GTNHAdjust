package com.gtnhadjust.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OverlayGroupPolicyTest {

    @Test
    public void candidateRotationIsStableWithinEachSecond() {
        assertEquals(0, OverlayGroupPolicy.candidateIndex(3, 0));
        assertEquals(0, OverlayGroupPolicy.candidateIndex(3, 999));
        assertEquals(1, OverlayGroupPolicy.candidateIndex(3, 1000));
        assertEquals(2, OverlayGroupPolicy.candidateIndex(3, 2000));
        assertEquals(0, OverlayGroupPolicy.candidateIndex(3, 3000));
        assertEquals(0, OverlayGroupPolicy.candidateIndex(1, 8000));
    }

    @Test
    public void missingRequiresNoCraftableOrStoredAlternative() {
        assertTrue(OverlayGroupPolicy.isMissing(false, false, 4));
        assertFalse(OverlayGroupPolicy.isMissing(true, false, 4));
        assertFalse(OverlayGroupPolicy.isMissing(false, true, 4));
        assertFalse(OverlayGroupPolicy.isMissing(true, true, 4));
        assertFalse(OverlayGroupPolicy.isMissing(false, false, 0));
    }
}
