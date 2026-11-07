package com.gtnhadjust.machine.loot;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class LootSimulatorRulesTest {

    @Test
    public void targetLimitIsSharedAcrossStacks() {
        assertEquals(40, LootSimulatorRules.acceptedCount(0, 40));
        assertEquals(24, LootSimulatorRules.acceptedCount(40, 64));
        assertEquals(0, LootSimulatorRules.acceptedCount(64, Integer.MAX_VALUE));
        assertEquals(64, LootSimulatorRules.acceptedCount(0, Integer.MAX_VALUE));
    }

    @Test
    public void experienceScalesForEachTargetAndCoil() {
        assertEquals(240, LootSimulatorRules.experience(2, 0));
        assertEquals(480, LootSimulatorRules.experience(2, 1));
        assertEquals(960, LootSimulatorRules.experience(2, 2));
        assertEquals(257698037640L, LootSimulatorRules.experience(Integer.MAX_VALUE, 0));
    }

    @Test
    public void probabilityBoundariesAndIndependentRolls() {
        assertEquals(0, LootSimulatorRules.rolls(new Random(1), 2, 0));
        assertEquals(2, LootSimulatorRules.rolls(new Random(1), 2, 10000));
        Random expected = new Random(42);
        int hits = 0;
        for (int i = 0; i < 100; i++) if (expected.nextInt(10000) < 3000) hits++;
        assertEquals(hits, LootSimulatorRules.rolls(new Random(42), 100, 3000));
    }
}
