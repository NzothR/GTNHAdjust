package com.gtnhadjust.machine.collector;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class CollectorThreadPolicyTest {

    @Test
    public void voltageAndGlassControlEachThreadIndependently() {
        assertEquals(0, CollectorThreadPolicy.parallel(0, 3, 64, 2));
        assertEquals(0, CollectorThreadPolicy.parallel(1, 2, 64, 2));
        assertEquals(64, CollectorThreadPolicy.parallel(1, 3, 64, 2));
        assertEquals(128, CollectorThreadPolicy.parallel(2, 3, 64, 2));
        assertEquals(256, CollectorThreadPolicy.parallel(3, 4, 64, 2));
        assertEquals(256, CollectorThreadPolicy.parallel(3, 10, 64, 2));
        assertEquals(30, CollectorThreadPolicy.parallel(2, 3, 10, 3));
        assertEquals(8, CollectorThreadPolicy.threads(1, 3, 8, 4));
        assertEquals(16, CollectorThreadPolicy.threads(2, 4, 8, 4));
        assertEquals(23, CollectorThreadPolicy.threads(2, 4, 12, 7));
        assertEquals(256, CollectorThreadPolicy.threads(15, 20, 250, 256));
        assertEquals(Integer.MAX_VALUE, CollectorThreadPolicy.parallel(30, 20, 64, 2));
    }

    @Test
    public void glassDiscountAffectsMinimumPowerWithRoundingAndFloor() {
        assertEquals(100, CollectorThreadPolicy.glassEnergyPercent(3));
        assertEquals(90, CollectorThreadPolicy.glassEnergyPercent(4));
        assertEquals(10, CollectorThreadPolicy.glassEnergyPercent(12));
        assertEquals(10, CollectorThreadPolicy.glassEnergyPercent(20));
        assertEquals(8, CollectorThreadPolicy.minimumEnergy(8, 90));
        assertEquals(7, CollectorThreadPolicy.minimumEnergy(8, 80));
        assertEquals(1, CollectorThreadPolicy.minimumEnergy(8, 10));
    }

    @Test
    public void distinctRecipesCanStartButTheSameRecipeCannotOccupyTwoThreads() {
        Set<String> keys = new HashSet<>();
        String ore = "overworld|20", gems = "overworld|21";
        assertTrue(CollectorThreadPolicy.canStart(keys, ore, 8));
        keys.add(ore);
        assertFalse(CollectorThreadPolicy.canStart(keys, ore, 8));
        assertTrue(CollectorThreadPolicy.canStart(keys, gems, 8));
        keys.add(gems);
        assertFalse(CollectorThreadPolicy.canStart(keys, "nether|20", 2));
        keys.remove(ore);
        assertTrue(CollectorThreadPolicy.canStart(keys, ore, 8));
    }

    @Test
    public void energyHatchesRaiseVoltageByAtMostOneTier() {
        assertEquals(2048, CollectorThreadPolicy.cappedVoltage(2048, 2048));
        assertEquals(4096, CollectorThreadPolicy.cappedVoltage(4096, 2048));
        assertEquals(8192, CollectorThreadPolicy.cappedVoltage(20480, 2048));
        assertEquals(Long.MAX_VALUE, CollectorThreadPolicy.cappedVoltage(Long.MAX_VALUE, Long.MAX_VALUE));
        assertEquals(0, CollectorThreadPolicy.cappedVoltage(0, 0));
    }

    @Test
    public void powerIsSharedWhileRespectingEachRecipesMinimum() {
        assertArrayEquals(new long[] { 64, 64 }, CollectorThreadPolicy.allocatePower(128, new long[] { 32, 32 }, 8));
        assertArrayEquals(new long[] { 32, 96 }, CollectorThreadPolicy.allocatePower(128, new long[] { 32, 96 }, 8));
        assertArrayEquals(
            new long[] { 0, 64, 64 },
            CollectorThreadPolicy.allocatePower(128, new long[] { 128, 32, 32 }, 8));
        assertArrayEquals(new long[] { 64, 0 }, CollectorThreadPolicy.allocatePower(64, new long[] { 32, 32 }, 1));
        assertArrayEquals(new long[] { 0, 0 }, CollectorThreadPolicy.allocatePower(31, new long[] { 32, 64 }, 8));
        assertArrayEquals(new long[] { 51, 50 }, CollectorThreadPolicy.allocatePower(101, new long[] { 32, 32 }, 8));
    }

    @Test
    public void threadPowerIsSummedAndCannotWrapAround() {
        long used = CollectorThreadPolicy.addPower(512, 1024);
        assertEquals(1536, used);
        assertEquals(512, CollectorThreadPolicy.remainingPower(2048, used));
        assertEquals(0, CollectorThreadPolicy.remainingPower(1024, used));
        assertEquals(Long.MAX_VALUE, CollectorThreadPolicy.addPower(Long.MAX_VALUE - 1, 2));
        assertEquals(0, CollectorThreadPolicy.remainingPower(Long.MAX_VALUE, Long.MAX_VALUE));
    }
}
