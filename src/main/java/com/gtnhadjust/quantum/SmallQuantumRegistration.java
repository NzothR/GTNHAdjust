package com.gtnhadjust.quantum;

import java.util.Collection;

import net.minecraft.item.ItemStack;

import com.gtnhadjust.GTNHAdjust;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.objects.OreDictItemStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
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
        String circuitName = OrePrefixes.circuit.get(Materials.HV)
            .toString();
        int circuitCount = GTOreDictUnificator.getOres(circuitName)
            .size();
        if (circuitCount == 0) {
            GTNHAdjust.LOG
                .error("Small quantum controller recipe unavailable: {} entries={}", circuitName, circuitCount);
            return;
        }
        ItemStack circuit = GTUtility.getIntegratedCircuit(24);
        ItemStack pump = ItemList.Electric_Pump_HV.get(64);
        ItemStack field = ItemList.Field_Generator_MV.get(64);
        ItemStack arm = ItemList.Robot_Arm_HV.get(64);
        ItemStack hull = ItemList.Hull_HV.get(64);
        if (circuit == null || pump == null
            || field == null
            || arm == null
            || hull == null
            || controller == null
            || Materials.StyreneButadieneRubber.getMolten(10_000_000) == null) {
            GTNHAdjust.LOG.error(
                "Small quantum controller recipe unavailable: circuit={}, pump={}, field={}, arm={}, hull={}, controller={}, rubber={}",
                circuit != null,
                pump != null,
                field != null,
                arm != null,
                hull != null,
                controller != null,
                Materials.StyreneButadieneRubber.getMolten(10_000_000) != null);
            return;
        }
        circuit.stackSize = 0;
        Collection<GTRecipe> recipes = GTRecipeBuilder.builder()
            .itemInputs(new OreDictItemStack(circuitName, 64), pump, field, arm, hull, circuit)
            .fluidInputs(Materials.StyreneButadieneRubber.getMolten(10_000_000))
            .itemOutputs(controller())
            .duration(1024 * 20)
            .eut(2048)
            .addTo(RecipeMaps.assemblerRecipes);
        if (recipes.isEmpty()) GTNHAdjust.LOG.warn("Small quantum controller assembler recipe was rejected");
        for (GTRecipe recipe : recipes) recipe.mDuration = 1024 * 20;
    }
}
