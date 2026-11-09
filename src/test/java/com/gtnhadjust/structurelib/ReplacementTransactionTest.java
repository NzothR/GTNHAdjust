package com.gtnhadjust.structurelib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReplacementTransactionTest {

    private static final class State implements ReplacementTransaction.Steps {

        String block = "old";
        boolean removeOk = true, placeOk = true, restoreOk = true, foreignOnFailure;
        int returned, refunded, changed;

        public boolean remove() {
            if (!removeOk) return false;
            block = "air";
            return true;
        }

        public boolean isEmpty() {
            return block.equals("air");
        }

        public boolean place() {
            if (placeOk) {
                block = "new";
                return true;
            }
            if (foreignOnFailure) block = "foreign";
            return false;
        }

        public boolean isOriginal() {
            return block.equals("old");
        }

        public boolean restore() {
            if (restoreOk) block = "old";
            return restoreOk;
        }

        public void returnOriginal() {
            returned++;
        }

        public void refundReplacement() {
            refunded++;
        }

        public void markChanged() {
            changed++;
        }
    }

    @Test
    public void successfulReplacementReturnsOnlyOldBlock() {
        State state = new State();
        assertTrue(ReplacementTransaction.execute(state));
        assertEquals("new", state.block);
        assertEquals(1, state.returned);
        assertEquals(0, state.refunded);
        assertEquals(1, state.changed);
    }

    @Test
    public void failedPlacementRestoresOldAndRefundsNew() {
        State state = new State();
        state.placeOk = false;
        assertFalse(ReplacementTransaction.execute(state));
        assertEquals("old", state.block);
        assertEquals(0, state.returned);
        assertEquals(1, state.refunded);
        assertEquals(0, state.changed);
    }

    @Test
    public void failedRestorationReturnsBothItems() {
        State state = new State();
        state.placeOk = false;
        state.restoreOk = false;
        assertFalse(ReplacementTransaction.execute(state));
        assertEquals("air", state.block);
        assertEquals(1, state.returned);
        assertEquals(1, state.refunded);
        assertEquals(1, state.changed);
    }

    @Test
    public void unexpectedWorldChangeDoesNotDuplicateReplacement() {
        State state = new State();
        state.placeOk = false;
        state.foreignOnFailure = true;
        assertFalse(ReplacementTransaction.execute(state));
        assertEquals("foreign", state.block);
        assertEquals(1, state.returned);
        assertEquals(0, state.refunded);
        assertEquals(1, state.changed);
    }

    @Test
    public void deniedRemovalRefundsReplacement() {
        State state = new State();
        state.removeOk = false;
        assertFalse(ReplacementTransaction.execute(state));
        assertEquals("old", state.block);
        assertEquals(0, state.returned);
        assertEquals(1, state.refunded);
    }
}
