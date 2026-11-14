package com.gtnhadjust.machine.proxy;

import net.minecraft.item.ItemStack;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.utils.enums.GTNRItemList;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Mods;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;

public final class ProxyFactoryRegistration {

    private static ItemStack controller;
    private static MachineLocatorItem locator;

    private ProxyFactoryRegistration() {}

    /** The locator and its recipe share the proxyFactory feature switch. */
    public static void registerItems(boolean client) {
        locator = new MachineLocatorItem();
        GameRegistry.registerItem(locator, "multiblock_machine_locator");
        if (client) locator.registerRenderer();
    }

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
        ItemStack chip = GTModHandler.getModItem(Mods.RemoteIO.ID, "item.chip.location", 1, 0);
        if (chip == null) {
            GTNHAdjust.LOG.warn("Multiblock machine locator recipe unavailable: RemoteIO location chip is missing");
            return;
        }
        GTValues.RA.stdBuilder()
            .itemInputs(ItemList.Tool_DataStick.get(1), chip, GTUtility.getIntegratedCircuit(1))
            .itemOutputs(new ItemStack(locator))
            .duration(60 * 20)
            .eut(480)
            .addTo(RecipeMaps.assemblerRecipes);
    }
}
