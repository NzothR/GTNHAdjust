package com.gtnhadjust.machine.reshaping;

import net.minecraft.item.ItemStack;

import com.gtnewhorizons.modularui.api.drawable.UITexture;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

/** EyeOfHarmonyBuffer EV controller and its retained processing recipe pool. */
public final class ReshapingRegistration {

    static final int PROCESSING_TICKS = 64 * GTRecipeBuilder.SECONDS;
    static final long PROCESSING_EUT = TierEU.MAX;

    public static final RecipeMap<?> MAP = RecipeMapBuilder.of("gtnhadjust.recipe.substance_reshaping")
        .maxIO(4, 16, 4, 16)
        .neiRecipeBackgroundSize(170, 185)
        .useCustomFilterForNEI()
        .logo(UITexture.fullImage("gtnhadjust", "gui/reshaping"))
        .logoSize(20, 20)
        .logoPos(152, 63)
        .frontend(SubstanceReshapingDeviceFrontend::new)
        .build();

    private static ItemStack controller;

    private ReshapingRegistration() {}

    /** The EV structure uses existing GTNH glass and casings; register its controller. */
    public static void registerMachine(int id) {
        if (GregTechAPI.METATILEENTITIES[id] != null)
            throw new IllegalStateException("Substance reshaping controller MTE ID is already in use: " + id);
        controller = new SubstanceReshapingDevice(id).getStackForm(1);
    }

    /** Register the EV controller and the retained processing recipes. */
    public static void registerRecipes() {
        GTValues.RA.stdBuilder()
            .itemInputs(
                ItemList.Casing_RobustTungstenSteel.get(8),
                ItemList.Emitter_EV.get(8),
                ItemList.Sensor_EV.get(8),
                ItemList.Field_Generator_EV.get(8),
                GTOreDictUnificator.get(OrePrefixes.circuit, Materials.EV, 16))
            .itemOutputs(controller.copy())
            .eut(TierEU.RECIPE_EV)
            .duration(30 * 20)
            .addTo(RecipeMaps.assemblerRecipes);
        new SubstanceReshapingDeviceRecipes().loadRecipes();
    }
}
