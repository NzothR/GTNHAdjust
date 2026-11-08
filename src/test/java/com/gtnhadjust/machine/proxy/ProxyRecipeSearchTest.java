package com.gtnhadjust.machine.proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProxyRecipeSearchTest {

    @Test
    public void findsLocalizedNamesOrRegistryNamesWithoutColorAndCase() {
        assertTrue(ProxyRecipeSearch.matches("simplification.recipe.petrochemical_complex", "§a石油化工综合体", "石油"));
        assertTrue(ProxyRecipeSearch.matches("simplification.recipe.petrochemical_complex", "§a石油化工综合体", "PETROCHEM"));
        assertTrue(ProxyRecipeSearch.matches("some.id", "§a中文名称", ""));
        assertFalse(ProxyRecipeSearch.matches("some.id", "中文名称", "石油"));
    }
}
