package com.gtnhadjust.quest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class QuestFluidAmountsTest {

    @Test
    public void requiresWholeContainersAndHandlesLargeVolumes() {
        assertEquals(63, QuestFluidAmounts.containers(63999, 1000));
        assertEquals(64, QuestFluidAmounts.containers(64000, 1000));
        assertEquals(0, QuestFluidAmounts.containers(1000, 0));
        assertEquals(Integer.MAX_VALUE, QuestFluidAmounts.containers(Long.MAX_VALUE, 1000));
    }
}
