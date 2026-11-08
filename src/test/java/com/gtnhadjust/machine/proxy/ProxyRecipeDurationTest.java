package com.gtnhadjust.machine.proxy;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProxyRecipeDurationTest {

    @Test
    public void tangshanUsesSquaredGlassTierOnlyForItsOwnPool() {
        assertEquals(1.0 / 25.0, ProxyRecipeDuration.factor("otht.recipe.TSSF", 5), 1e-12);
        assertEquals(1.0 / 9.0, ProxyRecipeDuration.factor("otht.recipe.TSSF", 3), 1e-12);
        assertEquals(1.0, ProxyRecipeDuration.factor("otht.recipe.TSSF", -1), 1e-12);
        assertEquals(1.0, ProxyRecipeDuration.factor("gt.recipe.assembler", 5), 1e-12);
    }
}
