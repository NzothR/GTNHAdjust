package com.gtnhadjust.machine.threadedproxy;

import net.minecraft.item.ItemStack;

import com.science.gtnl.utils.enums.GTNLItemList;

import gregtech.api.GregTechAPI;
import gregtech.api.util.GTModHandler;

/** Registers this independent machine without changing any existing controller IDs or recipes. */
public final class ThreadedProxyRegistration {

    private static ItemStack controller;

    private ThreadedProxyRegistration() {}

    public static void registerMachine(int id) {
        if (GregTechAPI.METATILEENTITIES[id] != null)
            throw new IllegalStateException("Threaded joint proxy MTE ID is already in use: " + id);
        controller = new ThreadedJointProxy(id, "gtnhadjust.threaded_joint_proxy", "Threaded Joint Proxy")
            .getStackForm(1);
    }

    public static void registerCrafting() {
        if (!GTModHandler
            .addShapelessCraftingRecipe(controller.copy(), new Object[] { GTNLItemList.SmeltingMixingFurnace.get(1) }))
            throw new IllegalStateException("Cannot register threaded joint proxy controller recipe");
    }
}
