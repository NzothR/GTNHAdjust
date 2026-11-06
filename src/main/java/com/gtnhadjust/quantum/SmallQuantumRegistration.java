package com.gtnhadjust.quantum;

import java.util.Collection;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import com.gtnhadjust.GTNHAdjust;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.objects.OreDictItemStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class SmallQuantumRegistration {

    private static ItemStack controller;

    private SmallQuantumRegistration() {}

    public static void registerMachine(int id) {
        if (id < 0 || id >= GregTechAPI.METATILEENTITIES.length || GregTechAPI.METATILEENTITIES[id] != null)
            throw new IllegalStateException("Small quantum controller MTE ID is already in use: " + id);
        controller = new SmallQuantumMachine(id, "gtnhadjust.small_quantum", "Xiaoyi's Small Quantum Manipulator")
            .getStackForm(1);
    }

    public static ItemStack controller() {
        return controller == null ? null : controller.copy();
    }

    public static void registerCrafting() {
        Block workbench = GameRegistry.findBlock("Avaritia", "Dire_Crafting");
        if (workbench == null) throw new IllegalStateException("Missing Avaritia Dire_Crafting block");
        ItemStack circuit = GTUtility.getIntegratedCircuit(24);
        circuit.stackSize = 0;
        Collection<GTRecipe> recipes = GTRecipeBuilder.builder()
            .itemInputs(
                new OreDictItemStack("circuitHV", 64),
                ItemList.Electric_Pump_HV.get(64),
                ItemList.Field_Generator_MV.get(64),
                ItemList.Robot_Arm_HV.get(64),
                new ItemStack(workbench, 32),
                circuit)
            .fluidInputs(Materials.StyreneButadieneRubber.getMolten(10_000_000))
            .itemOutputs(controller())
            .duration(1024 * 20)
            .eut(2048)
            .addTo(RecipeMaps.assemblerRecipes);
        if (recipes.isEmpty()) GTNHAdjust.LOG.warn("Small quantum controller assembler recipe was rejected");
        for (GTRecipe recipe : recipes) recipe.mDuration = 1024 * 20;
    }
}
