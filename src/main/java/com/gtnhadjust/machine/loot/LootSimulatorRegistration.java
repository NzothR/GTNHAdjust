package com.gtnhadjust.machine.loot;

import net.minecraft.item.ItemStack;

import com.gtnhadjust.GTNHAdjust;

import gregtech.api.GregTechAPI;
import gregtech.api.util.GTModHandler;
import kubatech.api.enums.ItemList;

public final class LootSimulatorRegistration {

    private static ItemStack controller;

    private LootSimulatorRegistration() {}

    public static void registerMachine(int id) {
        if (id < 0 || id >= GregTechAPI.METATILEENTITIES.length || GregTechAPI.METATILEENTITIES[id] != null)
            throw new IllegalStateException("Loot simulator MTE ID is already in use: " + id);
        controller = new LootSimulator(id, "gtnhadjust.loot_simulator", "Loot Simulation Chamber").getStackForm(1);
    }

    public static void registerCrafting() {
        ItemStack crusher = ItemList.ExtremeEntityCrusher.get(1);
        if (controller == null || crusher == null
            || !GTModHandler.addShapelessCraftingRecipe(controller.copy(), new Object[] { crusher }))
            GTNHAdjust.LOG.warn("Loot simulator controller crafting recipe could not be registered");
    }
}
