package com.futureresourceprotocol.integration.ae2;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Proxy;

import org.junit.Test;

import appeng.api.config.Actionable;
import appeng.api.storage.data.IAEItemStack;

public class AdvanceCellInventoryTest {

    @Test
    public void missingHostRejectsExtractionAndReturnsInjectedItem() {
        IAEItemStack request = (IAEItemStack) Proxy.newProxyInstance(
            IAEItemStack.class.getClassLoader(),
            new Class<?>[] { IAEItemStack.class },
            (proxy, method, args) -> method.getName()
                .equals("getStackSize") ? 1L : null);
        AdvanceCellInventory inventory = new AdvanceCellInventory(null, null);
        assertSame(request, inventory.injectItems(request, Actionable.SIMULATE, null));
        assertSame(request, inventory.injectItems(request, Actionable.MODULATE, null));
        assertNull(inventory.extractItems(request, Actionable.SIMULATE, null));
        assertNull(inventory.extractItems(request, Actionable.MODULATE, null));
    }
}
