package com.gtnhadjust.machine.collector;

import static org.junit.Assert.*;

import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import com.gtnhadjust.machine.proxy.ProxyThreadOutputs;
import com.gtnhadjust.machine.proxy.ProxyThreadTask;

public class CollectorThreadPersistenceTest {

    @Test
    public void separateThreadsSurviveReloadAndClearTogether() {
        ProxyThreadTask first = new ProxyThreadTask(0, 512, 20, 16, new ProxyThreadOutputs(null, null));
        ProxyThreadTask second = new ProxyThreadTask(1, 1024, 50, 32, new ProxyThreadOutputs(null, null));
        CollectorThreadLedger ledger = new CollectorThreadLedger();
        assertTrue(ledger.add("overworld|20", first, 8));
        assertTrue(ledger.add("overworld|21", second, 8));
        assertFalse(ledger.add("overworld|20", second, 8));
        assertFalse(ledger.add("nether|20", second, 8)); // slot 1 is already occupied
        first.advance();
        first.advance();
        second.advance();
        NBTTagList saved = ledger.save();
        CollectorThreadLedger restored = new CollectorThreadLedger();
        restored.load(saved);
        assertEquals(2, restored.size());
        ProxyThreadTask restoredFirst = restored.entries()
            .stream()
            .filter(
                entry -> entry.getKey()
                    .endsWith("20"))
            .findFirst()
            .get()
            .getValue();
        ProxyThreadTask restoredSecond = restored.entries()
            .stream()
            .filter(
                entry -> entry.getKey()
                    .endsWith("21"))
            .findFirst()
            .get()
            .getValue();
        assertEquals(2, restoredFirst.getProgress());
        assertEquals(1, restoredSecond.getProgress());
        assertEquals(512, restoredFirst.getEUt());
        assertEquals(1024, restoredSecond.getEUt());
        assertEquals(16, restoredFirst.getParallels());
        assertEquals(32, restoredSecond.getParallels());
        restoredFirst.advance();
        assertEquals(1, restoredSecond.getProgress());
        restored.clear();
        assertTrue(restored.isEmpty());
        assertEquals(
            0,
            restored.save()
                .tagCount());
    }
}
