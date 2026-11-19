package com.gtnhadjust.recipe;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;

public final class ManaConversionRecipes {

    private static final int BATCH_AMOUNT = 1_000_000;

    private ManaConversionRecipes() {}

    /** Register both directions once the two mana fluids are available. */
    public static void register() {
        Fluid fluidMana = FluidRegistry.getFluid("fluidmana");
        Fluid liquidMana = FluidRegistry.getFluid("liquid mana");
        if (fluidMana == null || liquidMana == null || fluidMana == liquidMana) return;
        addConversion(fluidMana, liquidMana);
        addConversion(liquidMana, fluidMana);
    }

    /** Add a one-second, lossless fluid heater conversion. */
    private static void addConversion(Fluid input, Fluid output) {
        GTValues.RA.stdBuilder()
            .fluidInputs(new FluidStack(input, BATCH_AMOUNT))
            .fluidOutputs(new FluidStack(output, BATCH_AMOUNT))
            .duration(20)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.fluidHeaterRecipes);
    }
}
