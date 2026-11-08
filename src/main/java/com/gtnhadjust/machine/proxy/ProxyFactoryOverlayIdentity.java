package com.gtnhadjust.machine.proxy;

import net.minecraft.item.ItemStack;

/** The early render mixin can safely query this before machine registration has run. */
public final class ProxyFactoryOverlayIdentity {

    private static ItemStack controller;

    private ProxyFactoryOverlayIdentity() {}

    static void setController(ItemStack stack) {
        controller = stack == null ? null : stack.copy();
    }

    public static boolean isController(ItemStack stack) {
        return stack != null && controller != null && stack.isItemEqual(controller);
    }
}
