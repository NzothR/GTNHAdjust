package com.futureresourceprotocol.device;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AdvanceInteractionTest {

    @Test
    public void gesturesChooseTheRequestedDestinationAndAmount() {
        int left = AdvanceInteraction.action(0, false, false);
        int ctrlLeft = AdvanceInteraction.action(0, false, true);
        int shiftLeft = AdvanceInteraction.action(0, true, false);
        int right = AdvanceInteraction.action(1, false, false);
        int shiftRight = AdvanceInteraction.action(1, true, false);
        assertTrue(AdvanceInteraction.toCursor(left));
        assertEquals(64, AdvanceInteraction.amount(left, 64));
        assertFalse(AdvanceInteraction.toCursor(ctrlLeft));
        assertEquals(1, AdvanceInteraction.amount(ctrlLeft, 64));
        assertFalse(AdvanceInteraction.toCursor(shiftLeft));
        assertEquals(64, AdvanceInteraction.amount(shiftLeft, 64));
        assertTrue(AdvanceInteraction.toCursor(right));
        assertEquals(32, AdvanceInteraction.amount(right, 64));
        assertFalse(AdvanceInteraction.toCursor(shiftRight));
        assertEquals(32, AdvanceInteraction.amount(shiftRight, 64));
        assertEquals(1, AdvanceInteraction.amount(right, 1));
        assertEquals(3, AdvanceInteraction.amount(right, 5));
        assertFalse(AdvanceInteraction.isAdvance(AdvanceInteraction.REPAY_CURSOR));
    }
}
