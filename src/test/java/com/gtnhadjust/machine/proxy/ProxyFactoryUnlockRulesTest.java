package com.gtnhadjust.machine.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class ProxyFactoryUnlockRulesTest {

    @Test
    public void intakeRequiresANewWhitelistedPool() {
        assertFalse(ProxyFactoryUnlockRules.addsNew(Arrays.asList("base", "a"), Arrays.asList("base", "a")));
        assertFalse(ProxyFactoryUnlockRules.addsNew(Arrays.asList("base"), Collections.emptyList()));
        assertTrue(ProxyFactoryUnlockRules.addsNew(Arrays.asList("base", "a"), Arrays.asList("a", "b")));
    }

    @Test
    public void fuelRefiningPoolUnlocksOnceAndIsRetainedForSharedControllers() {
        String pool = "gtnl.recipe.fuel_refining_complex";
        List<String> base = Collections.singletonList("base");
        List<String> offered = Collections.singletonList(pool);
        assertTrue(ProxyFactoryUnlockRules.addsNew(base, offered));
        assertFalse(ProxyFactoryUnlockRules.addsNew(Arrays.asList("base", pool), offered));
        assertEquals(
            offered,
            ProxyFactoryUnlockRules
                .orderedAdditional(base, ProxyFactoryRecipeWhitelist.ADDITIONAL, Arrays.asList(offered, offered)));
        assertEquals(
            offered,
            ProxyFactoryUnlockRules
                .orderedAdditional(base, ProxyFactoryRecipeWhitelist.ADDITIONAL, Collections.singletonList(offered)));
        assertTrue(
            ProxyFactoryUnlockRules
                .orderedAdditional(base, ProxyFactoryRecipeWhitelist.ADDITIONAL, Collections.emptyList())
                .isEmpty());
    }

    @Test
    public void outputRecalculatesSharedPoolsAndKeepsOriginals() {
        List<String> base = Arrays.asList("base");
        List<String> order = Arrays.asList("a", "b", "c");
        List<String> first = Arrays.asList("base", "a", "b", "not_whitelisted");
        List<String> second = Arrays.asList("b", "c");
        assertEquals(
            Arrays.asList("a", "b", "c"),
            ProxyFactoryUnlockRules.orderedAdditional(base, order, Arrays.asList(first, second)));
        assertEquals(
            Arrays.asList("b", "c"),
            ProxyFactoryUnlockRules.orderedAdditional(base, order, Collections.singletonList(second)));
        assertEquals(
            Collections.emptyList(),
            ProxyFactoryUnlockRules.orderedAdditional(base, order, Collections.emptyList()));
    }
}
