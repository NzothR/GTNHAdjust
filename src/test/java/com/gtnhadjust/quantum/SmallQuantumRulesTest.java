package com.gtnhadjust.quantum;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SmallQuantumRulesTest {

    @Test
    public void parallelUsesVoltageAndGlass() {
        assertEquals(32, SmallQuantumRules.parallel(1, 1));
        assertEquals(64, SmallQuantumRules.parallel(1, 2));
        assertEquals(128, SmallQuantumRules.parallel(1, 3));
        assertEquals(64, SmallQuantumRules.parallel(2, 1));
        assertEquals(128, SmallQuantumRules.parallel(2, 2));
        assertEquals(256, SmallQuantumRules.parallel(2, 3));
    }

    @Test
    public void tierMultipliersAndMinimum() {
        assertEquals(1.0, SmallQuantumRules.durationMultiplier(1), 0.000001);
        assertEquals(0.9, SmallQuantumRules.durationMultiplier(2), 0.000001);
        assertEquals(0.1, SmallQuantumRules.durationMultiplier(20), 0.000001);
        assertEquals(1.5, SmallQuantumRules.energyMultiplier(1, 1), 0.000001);
        assertEquals(1.4, SmallQuantumRules.energyMultiplier(3, 2), 0.000001);
        assertEquals(0.1, SmallQuantumRules.energyMultiplier(20, 20), 0.000001);
    }

    @Test
    public void recipeTimeMultiplierRoundsUpAndRespectsLimits() {
        assertEquals(40960, SmallQuantumRules.recipeDuration(409600, 0.1));
        assertEquals(2, SmallQuantumRules.recipeDuration(11, 0.1));
        assertEquals(1, SmallQuantumRules.recipeDuration(1, 0.1));
        assertEquals(400, SmallQuantumRules.recipeDuration(400, 1.0));
        assertEquals(Integer.MAX_VALUE, SmallQuantumRules.recipeDuration(Integer.MAX_VALUE, 100));
    }
}
