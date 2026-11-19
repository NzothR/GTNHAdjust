package com.gtnhadjust.machine.threadedproxy;

import net.minecraft.item.ItemStack;

import com.gtnhadjust.machine.proxy.ProxyFactoryRegistration;

import gregtech.api.GregTechAPI;
import gregtech.api.util.GTModHandler;

/** Registers this independent machine without changing any existing controller IDs or recipes. */
public final class ThreadedProxyRegistration {

    private static ItemStack controller;

    private ThreadedProxyRegistration() {}

    public static void registerMachine(int id) {
        if (GregTechAPI.METATILEENTITIES[id] != null)
            throw new IllegalStateException("Threaded joint proxy MTE ID is already in use: " + id);
        controller = new ThreadedJointProxy(id, "gtnhadjust.threaded_joint_proxy", "Pattern Computation Matrix")
            .getStackForm(1);
    }

    public static void registerCrafting() {
        ItemStack integrationMatrix = ProxyFactoryRegistration.controllerStack();
        if (integrationMatrix == null) return;
        if (!GTModHandler.addShapelessCraftingRecipe(controller.copy(), new Object[] { integrationMatrix }))
            throw new IllegalStateException("Cannot register threaded joint proxy controller recipe");
    }
}
