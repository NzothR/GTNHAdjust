// Ported from EyeOfHarmonyBuffer (GPL-3.0); see THIRD_PARTY_NOTICES.md.
package com.gtnhadjust.machine.reshaping;

import static gregtech.api.enums.Mods.*;
import static gregtech.api.recipe.RecipeMaps.assemblerRecipes;
import static gregtech.api.util.GTModHandler.getModItem;
import static gregtech.api.util.GTRecipeBuilder.MINUTES;
import static gregtech.api.util.GTRecipeBuilder.SECONDS;
import static gregtech.api.util.GTRecipeConstants.*;
import static gtPlusPlus.core.material.Particle.*;
import static gtPlusPlus.xmod.gregtech.api.enums.GregtechItemList.Controller_ElementalDuplicator;
import static tectech.thing.CustomItemList.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.TierEU;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTUtility;
import gregtech.api.util.recipe.Scanning;
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.core.material.MaterialsElements;
import gtPlusPlus.core.material.Particle;

final class ReshapingCraftingRecipes {

    private enum Tier {
        LV,
        MV,
        HV,
        EV,
        IV,
        LuV,
        ZPM,
        UV,
        UHV,
        UEV,
        UIV,
        UMV,
        UXV,
        MAX
    }

    private static final Map<Tier, ItemStack> CIRCUIT_MAP = new EnumMap<>(Tier.class);
    private static final Map<Tier, ItemStack> SINGULARITY_CASINGS_MAP = new EnumMap<>(Tier.class);
    private static final Map<Tier, ItemStack[]> MACHINE_COMPONENTS = new EnumMap<>(Tier.class);
    private static final String[] COMPONENT_NAMES = { "Electric_Motor", "Electric_Pump", "Electric_Piston",
        "Conveyor_Module", "Robot_Arm", "Emitter", "Field_Generator", "Sensor" };

    /** Register original controller and fourteen ring recipes after all dependency items exist. */
    static void register() {
        for (Tier tier : Tier.values()) {
            CIRCUIT_MAP.put(tier, getModItem(NewHorizonsCoreMod.ID, "Circuit" + tier.name(), 1));
            SINGULARITY_CASINGS_MAP.put(tier, ReshapingRegistration.ring(tier.ordinal()));
            if (tier == Tier.MAX) continue;
            ItemStack[] components = new ItemStack[COMPONENT_NAMES.length];
            for (int i = 0; i < components.length; i++)
                components[i] = ItemList.valueOf(COMPONENT_NAMES[i] + "_" + tier.name())
                    .get(1);
            MACHINE_COMPONENTS.put(tier, components);
        }
        // 物质重塑仪
        if (!com.gtnhadjust.GTNHAdjust.reshapingSimple) {
            GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, Controller_ElementalDuplicator.get(1))
                .metadata(SCANNING, new Scanning(500 * MINUTES, TierEU.RECIPE_UEV))
                .itemInputs(
                    eM_Hollow.get(1),
                    Godforge_MagneticConfinementCasing.get(8),
                    Godforge_GravitonFlowModulatorTier1.get(8),
                    Godforge_HarmonicPhononTransmissionConduit.get(8),

                    Godforge_StellarEnergySiphonCasing.get(8),
                    ItemList.Field_Generator_UIV.get(32),
                    ItemList.Emitter_UIV.get(8),
                    ItemList.Sensor_UIV.get(8),

                    GTUtility.copyAmountUnsafe(8, Particle.getBaseParticle(HIGGS_BOSON)),
                    GTUtility.copyAmountUnsafe(8, Particle.getBaseParticle(ELECTRON_NEUTRINO)),
                    GTUtility.copyAmountUnsafe(8, Particle.getBaseParticle(UNKNOWN)),
                    GTUtility.copyAmountUnsafe(8, Particle.getBaseParticle(GRAVITON)),

                    GTOreDictUnificator.get(OrePrefixes.circuit, Materials.UIV, 32),
                    GTOreDictUnificator.get(OrePrefixes.wireGt01, Materials.SuperconductorUIV, 64),
                    MaterialsElements.STANDALONE.CELESTIAL_TUNGSTEN.getPlate(64),
                    MaterialsElements.STANDALONE.ASTRAL_TITANIUM.getPlate(64))
                .fluidInputs(
                    MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(128000000),
                    Materials.Infinity.getMolten(128000000),
                    Materials.SpaceTime.getMolten(128000000))
                .itemOutputs(ReshapingRegistration.controller())
                .eut(TierEU.RECIPE_UIV)
                .duration(2000 * SECONDS)
                .addTo(AssemblyLine);
        } else {
            GTValues.RA.stdBuilder()
                .itemInputs(
                    ItemList.Casing_RobustTungstenSteel.get(8),
                    ItemList.Emitter_EV.get(8),
                    ItemList.Sensor_EV.get(8),
                    ItemList.Field_Generator_EV.get(8),
                    GTOreDictUnificator.get(OrePrefixes.circuit, Materials.EV, 16))
                .itemOutputs(ReshapingRegistration.controller())
                .eut(TierEU.RECIPE_EV)
                .duration(30 * SECONDS)
                .addTo(assemblerRecipes);
        }

        // 奇点稳定环
        Tier[] tiers = Tier.values();

        for (int i = 0; i < tiers.length; i++) {
            Tier tier = tiers[i];
            if (tier == Tier.MAX) continue;

            ItemStack[] components = MACHINE_COMPONENTS.get(tier);
            if (components == null || components.length == 0) continue;

            ItemStack[] adjustedComponents = new ItemStack[components.length];
            for (int j = 0; j < components.length; j++) {
                ItemStack stack = components[j].copy();
                stack.stackSize = 1024;
                adjustedComponents[j] = stack;
            }

            ItemStack circuit = CIRCUIT_MAP.get(tier)
                .copy();
            circuit.stackSize = 123123;

            ItemStack previousCasing;
            int casingCount;

            if (tier == Tier.LV) {
                casingCount = 1;
                previousCasing = ReshapingRegistration.catalyst(OrePrefixes.ingot, casingCount);
            } else {
                casingCount = 2;
                previousCasing = SINGULARITY_CASINGS_MAP.get(tiers[i - 1])
                    .copy();
                previousCasing.stackSize = casingCount;
            }

            List<ItemStack> allInputs = new ArrayList<>(
                Arrays.asList(
                    previousCasing,
                    GTUtility.copyAmountUnsafe(
                        123,
                        getModItem(AppliedEnergistics2.ID, "item.ItemExtremeStorageCell.Universe", 1)),
                    GTUtility.copyAmountUnsafe(123, getModItem(AE2FluidCraft.ID, "fluid_storage.Universe", 1)),
                    GTUtility.copyAmountUnsafe(123, getModItem(AppliedEnergistics2.ID, "item.ItemVoidStorageCell", 1)),
                    GTUtility.copyAmountUnsafe(
                        1280000,
                        getModItem(AppliedEnergistics2.ID, "tile.BlockSingularityCraftingStorage", 1)),
                    GTUtility.copyAmountUnsafe(
                        1280000,
                        getModItem(AppliedEnergistics2.ID, "tile.BlockAdvancedCraftingUnit", 1, 3)),
                    GTUtility.copyAmountUnsafe(
                        100000000,
                        getModItem(AppliedEnergistics2.ID, "item.ItemMultiMaterial", 1, 47)),
                    circuit));
            Collections.addAll(allInputs, adjustedComponents);

            ItemStack output = SINGULARITY_CASINGS_MAP.get(tier)
                .copy();

            ItemStack researchInput = (tier == Tier.LV) ? ReshapingRegistration.catalyst(OrePrefixes.ingot, 1)
                : SINGULARITY_CASINGS_MAP.get(tiers[i - 1])
                    .copy();

            GTValues.RA.stdBuilder()
                .metadata(RESEARCH_ITEM, researchInput)
                .metadata(SCANNING, new Scanning(500 * MINUTES, TierEU.RECIPE_UEV))
                .itemInputsUnsafe(allInputs.toArray(new ItemStack[0]))
                .fluidInputs(
                    MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(128000000),
                    Materials.Infinity.getMolten(128000000),
                    Materials.SpaceTime.getMolten(128000000))
                .itemOutputs(output)
                .duration(5000 * SECONDS + +tier.ordinal() * (1000 * SECONDS))
                .eut(TierEU.RECIPE_MAX)
                .addTo(AssemblyLine);
        }

        // 奇点稳定环MAX
        GTValues.RA.stdBuilder()
            .metadata(RESEARCH_ITEM, ReshapingRegistration.ring(12))
            .metadata(SCANNING, new Scanning(500 * MINUTES, TierEU.RECIPE_UEV))
            .itemInputsUnsafe(
                ReshapingRegistration.ring(12),
                GTUtility.copyAmountUnsafe(114514, getModItem(SGCraft.ID, "sgChevronUpgrade", 1)),
                GTUtility.copyAmountUnsafe(114514, getModItem(SGCraft.ID, "sgCoreCrystal", 1)),
                GTUtility.copyAmountUnsafe(114514, getModItem(SGCraft.ID, "ic2Capacitor", 1)),
                GTUtility.copyAmountUnsafe(114514, getModItem(SGCraft.ID, "sgControllerCrystal", 1)),
                GTUtility.copyAmountUnsafe(114514, getModItem(NewHorizonsCoreMod.ID, "item.StargateChevron", 1)),
                GTUtility.copyAmountUnsafe(114514, getModItem(SGCraft.ID, "sgIrisUpgrade", 1)),
                GTUtility.copyAmountUnsafe(114514, getModItem(SGCraft.ID, "sgIrisBlade", 1)),
                GTUtility.copyAmountUnsafe(114514, getModItem(SGCraft.ID, "stargateBase", 1)),
                GTUtility.copyAmountUnsafe(
                    102400000,
                    getModItem(AppliedEnergistics2.ID, "item.ItemExtremeStorageCell.Universe", 1)),
                GTUtility.copyAmountUnsafe(102400000, getModItem(AE2FluidCraft.ID, "fluid_storage.Universe", 1)),
                GTUtility
                    .copyAmountUnsafe(102400000, getModItem(AppliedEnergistics2.ID, "item.ItemVoidStorageCell", 1)),
                GTUtility.copyAmountUnsafe(10240000, GTOreDictUnificator.get(OrePrefixes.circuit, Materials.MAX, 1)),
                GTUtility.copyAmountUnsafe(114514, getModItem(NewHorizonsCoreMod.ID, "item.StargateShieldingFoil", 1)),
                GTUtility.copyAmountUnsafe(114514, getModItem(NewHorizonsCoreMod.ID, "item.StargateFramePart", 1)),
                GTUtility.copyAmountUnsafe(10240000, ItemList.Field_Generator_MAX.get(1)))
            .fluidInputs(
                MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(128000000),
                Materials.Infinity.getMolten(128000000),
                Materials.SpaceTime.getMolten(128000000))
            .itemOutputs(ReshapingRegistration.ring(13))
            .duration(20000000 * SECONDS)
            .eut(TierEU.RECIPE_MAX)
            .addTo(AssemblyLine);

    }
}
