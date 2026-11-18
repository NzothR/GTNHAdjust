package com.gtnhadjust.recipe;

import java.util.stream.Collectors;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.gtnhadjust.GTNHAdjust;
import com.science.gtnl.common.material.GTNLRecipeMaps;
import com.science.gtnl.utils.enums.GTNLItemList;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class RecombinationFusionRecipe {

    private static final int STONE_DUST = 2_100_000_000;
    private static final int FLUID_AMOUNT = Integer.MAX_VALUE;
    private static final int DURATION = 64 * GTRecipeBuilder.SECONDS;

    private RecombinationFusionRecipe() {}

    /** Replace the dust-only GTNL recipe with a separately registered circuit variant. */
    public static void register() {
        RecipeMap<RecipeMapBackend> map = GTNLRecipeMaps.RecombinationFusionReactorRecipes;
        ItemStack stoneDust = GTOreDictUnificator.get(OrePrefixes.dust, Materials.Stone, 1);
        for (GTRecipe recipe : GTValues.RA.stdBuilder()
            .itemInputsUnsafe(GTUtility.getIntegratedCircuit(1), GTUtility.copyAmountUnsafe(STONE_DUST, stoneDust))
            .itemOutputs(
                GTNLItemList.TrollFace.get(1)
                    .setStackDisplayName(
                        StatCollector.translateToLocal("gtnl.recipe.refusion_reactor.structure_incomplete")))
            .outputChances(1)
            .fluidOutputs(
                Materials.MHDCSM.getMolten(FLUID_AMOUNT),
                Materials.Universium.getMolten(FLUID_AMOUNT),
                Materials.WhiteDwarfMatter.getMolten(FLUID_AMOUNT),
                Materials.BlackDwarfMatter.getMolten(FLUID_AMOUNT),
                Materials.SpaceTime.getMolten(FLUID_AMOUNT),
                Materials.TranscendentMetal.getMolten(FLUID_AMOUNT),
                Materials.Eternity.getMolten(FLUID_AMOUNT),
                Materials.PrimordialMatter.getFluid(FLUID_AMOUNT),
                Materials.Space.getMolten(FLUID_AMOUNT),
                Materials.Time.getMolten(FLUID_AMOUNT),
                Materials.SixPhasedCopper.getMolten(FLUID_AMOUNT),
                Materials.Antimatter.getFluid(FLUID_AMOUNT))
            .duration(DURATION)
            .eut(0)
            .ignoreCollision()
            .addTo(map)) {
            // GTNR's global recipe speed setting is applied during compilation.
            recipe.mDuration = DURATION;
            map.getBackend()
                .removeRecipes(
                    map.getAllRecipes()
                        .stream()
                        .filter(
                            original -> original.mInputs.length == 1 && original.mFluidInputs.length == 0
                                && GTUtility.areStacksEqual(original.mInputs[0], stoneDust))
                        .collect(Collectors.toList()));
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                "Registered circuit 1 recombination fusion recipe: {} ticks, {} EU/t, {} stone dust",
                recipe.mDuration,
                recipe.mEUt,
                recipe.mInputs[1].stackSize);
        }
    }
}
