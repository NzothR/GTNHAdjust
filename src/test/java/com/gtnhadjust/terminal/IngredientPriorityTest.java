package com.gtnhadjust.terminal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IngredientPriorityTest {

    @Test
    public void ordersFavoritesCircuitCraftableStoredAndFallback() {
        assertEquals(
            1,
            IngredientPriority.select(
                new int[] { 7, 2, -1 },
                new boolean[] { true, false, false },
                new boolean[] { true, false, true },
                new boolean[] { true, true, true }));
        assertEquals(
            0,
            IngredientPriority.select(
                new int[] { -1, -1, -1 },
                new boolean[] { true, false, false },
                new boolean[] { false, true, true },
                new boolean[] { true, true, true }));
        assertEquals(
            2,
            IngredientPriority.select(
                new int[] { -1, -1, -1 },
                new boolean[3],
                new boolean[] { true, true, true },
                new boolean[] { true, true, true }));
        assertEquals(
            1,
            IngredientPriority
                .select(new int[] { -1, -1, -1 }, new boolean[3], new boolean[3], new boolean[] { true, true, false }));
        assertEquals(
            -1,
            IngredientPriority.select(new int[] { -1, -1, -1 }, new boolean[3], new boolean[3], new boolean[3]));
    }
}
