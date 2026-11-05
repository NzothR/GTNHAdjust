package com.gtnhadjust.recipe;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GtsrRecipeMapPolicyTest {

    @Test
    public void onlyMatchesGtsrExclusiveNamespace() {
        assertTrue(GtsrRecipeMapPolicy.isExclusive("gtsr.recipe.steam_fluid_drill"));
        assertTrue(GtsrRecipeMapPolicy.isExclusive("gtsr.recipe.critical.loom"));
        assertFalse(GtsrRecipeMapPolicy.isExclusive("gt.recipe.assembler"));
        assertFalse(GtsrRecipeMapPolicy.isExclusive("other.gtsr.recipe.assembler"));
        assertFalse(GtsrRecipeMapPolicy.isExclusive("gtsr.recipe2.foo"));
        assertFalse(GtsrRecipeMapPolicy.isExclusive(null));
    }
}
