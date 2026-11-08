package com.futureresourceprotocol.protocol;

import static org.junit.Assert.*;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class WorldProtocolTest {

    @Test
    public void settlementStagesAndPersistence() {
        WorldProtocol ledger = new WorldProtocol();
        String key = "example:item@0";
        assertFalse(ledger.canAdvance(true));
        ledger.advance(key, false, 8);
        assertFalse(ledger.permitsHull(9));
        assertFalse(ledger.permitsHull(12));
        assertFalse(ledger.permitsHull(13));
        assertFalse(ledger.completeUhv());
        assertEquals(5, ledger.settle(key, 5));
        assertEquals(3, ledger.settle(key, 20));
        assertTrue(ledger.completeUhv());
        assertTrue(ledger.permitsHull(9));
        ledger.advance(key, true, 7);
        ledger.advance(key, false, 4);
        assertEquals(6, ledger.settle(key, 6));
        assertEquals(0, ledger.debt(key, false));
        assertEquals(5, ledger.debt(key, true));
        assertFalse(ledger.completeUmv());
        NBTTagCompound tag = new NBTTagCompound();
        ledger.writeToNBT(tag);
        WorldProtocol loaded = new WorldProtocol();
        loaded.readFromNBT(tag);
        assertEquals(5, loaded.debt(key, true));
        assertEquals(Boolean.FALSE, loaded.cachedClass(key));
        assertEquals(5, loaded.settle(key, 100));
        assertTrue(loaded.completeUmv());
        assertTrue(loaded.permitsHull(13));
        assertFalse(loaded.canAdvance(false));
    }

    @Test
    public void refusesOverflowAndUnknownVersions() {
        WorldProtocol ledger = new WorldProtocol();
        ledger.advance("x:y@0", false, Long.MAX_VALUE);
        assertFalse(ledger.canRecord("x:y@0", false, 1));
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("version", 99);
        WorldProtocol loaded = new WorldProtocol();
        loaded.readFromNBT(tag);
        assertTrue(loaded.isTerminated());
        assertEquals(0, loaded.settle("x:y@0", 1));
    }

    @Test
    public void overpaymentConsumesOnlyOutstandingDebt() {
        WorldProtocol ledger = new WorldProtocol();
        String key = "example:item@3";
        ledger.advance(key, false, 2);
        assertEquals(2, ledger.settle(key, 64));
        assertEquals(0, ledger.debt(key, false));
        assertEquals(0, ledger.settle(key, 62));
    }

    @Test
    public void hullPermissionFollowsBothDebtClassesAndFinalMilestone() {
        WorldProtocol ledger = new WorldProtocol();
        assertTrue(ledger.permitsHull(9));
        assertTrue(ledger.permitsHull(12));
        assertFalse(ledger.permitsHull(13));
        assertTrue(ledger.completeUhv());
        ledger.advance("example:umv@0", true, 1);
        assertTrue(ledger.permitsHull(9));
        assertFalse(ledger.permitsHull(12));
        ledger.advance("example:uhv@0", false, 1);
        assertFalse(ledger.permitsHull(9));
        ledger.settle("example:uhv@0", 1);
        ledger.settle("example:umv@0", 1);
        assertTrue(ledger.permitsHull(12));
        assertTrue(ledger.completeUmv());
        assertTrue(ledger.permitsHull(13));
    }
}
