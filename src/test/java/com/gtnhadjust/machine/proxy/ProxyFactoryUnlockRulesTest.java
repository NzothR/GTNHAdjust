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
