package com.gtnhadjust.collector;

import net.minecraft.item.ItemStack;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTModHandler;

public final class SuperFragmentRegistration {

    private static ItemStack controller;

    private SuperFragmentRegistration() {}

    public static void registerMachine(int id) {
        if (id < 0 || id >= GregTechAPI.METATILEENTITIES.length || GregTechAPI.METATILEENTITIES[id] != null)
            throw new IllegalStateException("Super fragment collector MTE ID is already in use: " + id);
        controller = new SuperFragmentCollector(
            id,
            "gtnhadjust.super_fragment_collector",
            "Super Fragment World Collector").getStackForm(1);
    }

    public static ItemStack controller() {
        return controller == null ? null : controller.copy();
    }

    public static void registerCrafting() {
        GTModHandler.addCraftingRecipe(
            controller(),
            new Object[] { "WNW", "PCQ", "XNX", 'W', OrePrefixes.cableGt01.get(Materials.Electrum), 'N', "logRubber",
                'P', ItemList.Sensor_HV.get(1), 'Q', ItemList.Emitter_HV.get(1), 'C', ItemList.Hull_HV.get(1), 'X',
                ItemList.Machine_HV_Hammer.get(1) });
    }
}
