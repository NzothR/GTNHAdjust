// Ported from EyeOfHarmonyBuffer (GPL-3.0); see THIRD_PARTY_NOTICES.md.
package com.gtnhadjust.machine.reshaping;

import static gregtech.api.enums.Mods.EternalSingularity;
import static gregtech.api.recipe.RecipeMaps.mixerNonCellRecipes;
import static gregtech.api.recipe.RecipeMaps.vacuumFreezerRecipes;
import static gregtech.api.util.GTModHandler.getModItem;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.COIL_HEAT;
import static tectech.recipe.TecTechRecipeMaps.godforgeMoltenRecipes;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import tectech.thing.CustomItemList;

public class ReshapingMaterialsRecipes {

    /** Preserve the upstream dust, hot-ingot and vacuum cooling steps used by the LV ring. */
    public void loadRecipes() {

        GTValues.RA.stdBuilder()
            .itemInputsUnsafe(
                GTUtility.copyAmount(0, CustomItemList.Machine_Multi_EyeOfHarmony.get(1)),
                GTUtility.copyAmountUnsafe(Integer.MAX_VALUE, Materials.MagMatter.getDust(1)),
                GTUtility.copyAmountUnsafe(
                    Integer.MAX_VALUE,
                    GTOreDictUnificator.get(OrePrefixes.dust, Materials.Bedrockium, 1)),
                GTUtility.copyAmountUnsafe(Integer.MAX_VALUE, Materials.Eternity.getDust(1)),
                GTUtility.copyAmountUnsafe(Integer.MAX_VALUE, Materials.Infinity.getDust(1))

            )
            .fluidInputs(
                Materials.Antimatter.getFluid(1919810),
                Materials.PhononMedium.getFluid(1145140),
                Materials.QuarkGluonPlasma.getFluid(1231231))
            .itemOutputs(ReshapingRegistration.catalyst(OrePrefixes.dust, 1))
            .eut(TierEU.RECIPE_UXV)
            .duration(3000 * SECONDS)
            .addTo(mixerNonCellRecipes);

        GTValues.RA.stdBuilder()
            .itemInputsUnsafe(
                GTUtility.copyAmountUnsafe(
                    1280000,
                    GTOreDictUnificator.get(OrePrefixes.ingot, Materials.WhiteDwarfMatter, 1)),
                GTUtility.copyAmountUnsafe(
                    1280000,
                    GTOreDictUnificator.get(OrePrefixes.ingot, Materials.BlackDwarfMatter, 1)),
                GTUtility.copyAmountUnsafe(1280000, getModItem(EternalSingularity.ID, "eternal_singularity", 1)),
                GTUtility.copyAmountUnsafe(1280000, Materials.Universium.getIngots(1)),
                ReshapingRegistration.catalyst(OrePrefixes.dust, 1))
            .fluidInputs(Materials.SpaceTime.getMolten(1440000))
            .itemOutputs(ReshapingRegistration.catalyst(OrePrefixes.ingotHot, 1))
            .eut(TierEU.RECIPE_UXV)
            .duration(300 * SECONDS)
            .metadata(COIL_HEAT, 49999)
            .addTo(godforgeMoltenRecipes);

        GTValues.RA.stdBuilder()
            .itemInputs(ReshapingRegistration.catalyst(OrePrefixes.ingotHot, 1))
            .fluidInputs(Materials.Space.getMolten(1440000), Materials.Time.getMolten(1440000))
            .itemOutputs(ReshapingRegistration.catalyst(OrePrefixes.ingot, 1))
            .eut(TierEU.RECIPE_UXV)
            .duration(50 * SECONDS)
            .addTo(vacuumFreezerRecipes);
    }
}
