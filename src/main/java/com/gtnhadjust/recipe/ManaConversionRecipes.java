package com.gtnhadjust.recipe;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnhadjust.GTNHAdjust;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.TierEU;
import gregtech.api.recipe.RecipeMaps;

public final class ManaConversionRecipes {

    private static final int BATCH_AMOUNT = 1_000_000;

    private ManaConversionRecipes() {}

    /** Register both directions once the two mana fluids are available. */
    public static void register() {
        Fluid fluidMana = FluidRegistry.getFluid("fluid mana");
        Fluid liquidMana = FluidRegistry.getFluid("liquid mana");
        if (fluidMana == null || liquidMana == null || fluidMana == liquidMana) {
            if (GTNHAdjust.debug)
                GTNHAdjust.LOG.info("[mana conversion] missing distinct fluid mana and liquid mana registrations");
            return;
        }
        addConversion(fluidMana, liquidMana);
        addConversion(liquidMana, fluidMana);
    }

    /** Add a one-second, lossless fluid heater conversion. */
    private static void addConversion(Fluid input, Fluid output) {
        int added = GTValues.RA.stdBuilder()
            .fluidInputs(new FluidStack(input, BATCH_AMOUNT))
            .fluidOutputs(new FluidStack(output, BATCH_AMOUNT))
            .duration(20)
            .eut(TierEU.RECIPE_HV)
            .addTo(RecipeMaps.fluidHeaterRecipes)
            .size();
        if (GTNHAdjust.debug) GTNHAdjust.LOG
            .info("[mana conversion] {} -> {}: {} recipes registered", input.getName(), output.getName(), added);
    }
}
