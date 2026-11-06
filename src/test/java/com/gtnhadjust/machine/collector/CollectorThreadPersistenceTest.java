package com.gtnhadjust.machine.collector;

import static org.junit.Assert.*;

import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;

public class CollectorThreadPersistenceTest {

    @Test
    public void separateThreadsSurviveReloadAndClearTogether() {
        RecipeThreadTask first = new RecipeThreadTask(0, 512, 20, 16, new RecipeThreadOutputs(null, null));
        RecipeThreadTask second = new RecipeThreadTask(1, 1024, 50, 32, new RecipeThreadOutputs(null, null));
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
        RecipeThreadTask restoredFirst = restored.entries()
            .stream()
            .filter(
                entry -> entry.getKey()
                    .endsWith("20"))
            .findFirst()
            .get()
            .getValue();
        RecipeThreadTask restoredSecond = restored.entries()
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
