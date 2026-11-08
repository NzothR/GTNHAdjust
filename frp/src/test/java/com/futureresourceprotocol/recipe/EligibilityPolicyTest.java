package com.futureresourceprotocol.recipe;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EligibilityPolicyTest {

    @Test
    public void bypassOnlyChangesRecipeRequirement() {
        assertFalse(EligibilityPolicy.allows(true, false, false, false));
        assertTrue(EligibilityPolicy.allows(true, false, true, false));
        assertTrue(EligibilityPolicy.allows(true, false, false, true));
        assertFalse(EligibilityPolicy.allows(true, true, true, true));
        assertFalse(EligibilityPolicy.allows(false, false, true, true));
    }
}
