package com.futureresourceprotocol.recipe;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EligibilityPolicyTest {

    @Test
    public void bypassPreservesBlacklistAndVoltageCeiling() {
        assertFalse(EligibilityPolicy.allows(true, false, false, false, false));
        assertTrue(EligibilityPolicy.allows(true, false, true, false, false));
        assertTrue(EligibilityPolicy.allows(true, false, false, true, false));
        assertFalse(EligibilityPolicy.allows(true, true, true, true, false));
        assertFalse(EligibilityPolicy.allows(false, false, true, true, false));
        assertFalse(EligibilityPolicy.allows(true, false, true, true, true));
        assertFalse(EligibilityPolicy.allows(true, false, true, false, true));
    }

    @Test
    public void voltageTierBoundariesIncludePracticalVoltages() {
        long uvMax = 524288L;
        long uivMax = 33554432L;
        assertFalse(EligibilityPolicy.isUmvClass(uvMax, uvMax));
        assertTrue(EligibilityPolicy.isUmvClass(uvMax + 1, uvMax));
        assertTrue(EligibilityPolicy.isUmvClass(1966080, uvMax));
        assertFalse(EligibilityPolicy.exceedsUmvLimit(uivMax, uivMax));
        assertTrue(EligibilityPolicy.exceedsUmvLimit(uivMax + 1, uivMax));
    }

}
