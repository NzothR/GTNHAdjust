package com.gtnhadjust.machine.proxy;

import net.minecraft.item.ItemStack;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import gregtech.api.GregTechAPI;
import gregtech.api.util.GTModHandler;

public final class ProxyFactoryRegistration {

    private static ItemStack controller;

    private ProxyFactoryRegistration() {}

    public static void registerMachine(int id) {
        if (id < 0 || id >= GregTechAPI.METATILEENTITIES.length || GregTechAPI.METATILEENTITIES[id] != null)
            throw new IllegalStateException("Proxy joint factory MTE ID is already in use: " + id);
        controller = new ProxyFactory(id, "gtnhadjust.proxy_factory", "Proxy Joint Factory").getStackForm(1);
    }

    public static void registerCrafting() {
        ItemStack original = GTNRItemList.UniversalJointFactory.get(1);
        if (controller == null || original == null
            || !GTModHandler.addShapelessCraftingRecipe(controller.copy(), new Object[] { original }))
            GTNHAdjust.LOG.warn("Proxy joint factory controller crafting recipe could not be registered");
    }
}
