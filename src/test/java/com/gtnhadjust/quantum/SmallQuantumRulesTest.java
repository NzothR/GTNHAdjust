package com.gtnhadjust.quantum;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SmallQuantumRulesTest {

    @Test
    public void parallelUsesVoltageAndGlass() {
        assertEquals(1, SmallQuantumRules.parallel(1, 1));
        assertEquals(2, SmallQuantumRules.parallel(1, 2));
        assertEquals(4, SmallQuantumRules.parallel(1, 3));
        assertEquals(5, SmallQuantumRules.parallel(2, 1));
        assertEquals(10, SmallQuantumRules.parallel(2, 2));
        assertEquals(20, SmallQuantumRules.parallel(2, 3));
    }

    @Test
    public void tierMultipliersAndMinimum() {
        assertEquals(1.2, SmallQuantumRules.durationMultiplier(1), 0.000001);
        assertEquals(1.1, SmallQuantumRules.durationMultiplier(2), 0.000001);
        assertEquals(0.1, SmallQuantumRules.durationMultiplier(20), 0.000001);
        assertEquals(1.5, SmallQuantumRules.energyMultiplier(1, 1), 0.000001);
        assertEquals(1.4, SmallQuantumRules.energyMultiplier(3, 2), 0.000001);
        assertEquals(0.1, SmallQuantumRules.energyMultiplier(20, 20), 0.000001);
    }
}
