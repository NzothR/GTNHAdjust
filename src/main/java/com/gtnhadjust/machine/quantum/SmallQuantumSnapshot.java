package com.gtnhadjust.machine.quantum;

import static gregtech.api.enums.Mods.GalaxySpace;
import static gregtech.api.enums.Mods.NewHorizonsCoreMod;
import static gregtech.api.util.GTModHandler.getModItem;

import java.util.Arrays;
import java.util.Collection;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;

import com.gtnewhorizon.cropsnh.api.CropsNHItemList;

import bartworks.system.material.WerkstoffLoader;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;
import gtPlusPlus.core.material.MaterialMisc;
import gtPlusPlus.core.material.MaterialsElements;
import gtnhlanth.common.register.WerkstoffMaterialPool;

/** Generated fixed GT5 QFT recipe snapshot. See tools/generate_small_quantum_snapshot.py. */
final class SmallQuantumSnapshot {

    private SmallQuantumSnapshot() {}

    static void register() {
        // QFT source recipe 0; fixed output chance 1666/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(WerkstoffLoader.PTMetallicPowder.get(OrePrefixes.dust, 1))
                .itemOutputs(
                    Materials.Platinum.getDust(2),
                    Materials.Palladium.getDust(2),
                    Materials.Iridium.getDust(2),
                    Materials.Osmium.getDust(2),
                    WerkstoffLoader.Rhodium.get(OrePrefixes.dust, 2),
                    WerkstoffLoader.Ruthenium.get(OrePrefixes.dust, 2))
                .outputChances(chances(6, 1666))
                .fluidOutputChances(chances(0, 1666))
                .duration(409600)
                .eut(480),
            409600,
            "0");
        // QFT source recipe 1; fixed output chance 3333/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(WerkstoffLoader.PDMetallicPowder.get(OrePrefixes.dust, 1))
                .itemOutputs(
                    Materials.Palladium.getDust(2),
                    Materials.Platinum.getDust(2),
                    WerkstoffLoader.RhodiumPlatedPalladium.get(OrePrefixes.dust, 2))
                .outputChances(chances(3, 3333))
                .fluidOutputChances(chances(0, 3333))
                .duration(409600)
                .eut(480),
            409600,
            "1");
        // QFT source recipe 2; fixed output chance 3333/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(WerkstoffLoader.IrLeachResidue.get(OrePrefixes.dust, 1))
                .itemOutputs(
                    Materials.Iridium.getDust(2),
                    Materials.Platinum.getDust(2),
                    Materials.Osmiridium.getDust(2))
                .outputChances(chances(3, 3333))
                .fluidOutputChances(chances(0, 3333))
                .duration(409600)
                .eut(480),
            409600,
            "2");
        // QFT source recipe 3; fixed output chance 3333/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(WerkstoffLoader.IrOsLeachResidue.get(OrePrefixes.dust, 1))
                .itemOutputs(Materials.Osmium.getDust(2), Materials.Iridium.getDust(2), Materials.Osmiridium.getDust(2))
                .outputChances(chances(3, 3333))
                .fluidOutputChances(chances(0, 3333))
                .duration(409600)
                .eut(480),
            409600,
            "3");
        // QFT source recipe 4; fixed output chance 2500/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(WerkstoffLoader.CrudeRhMetall.get(OrePrefixes.dust, 1))
                .itemOutputs(
                    WerkstoffLoader.Rhodium.get(OrePrefixes.dust, 2),
                    Materials.Palladium.getDust(2),
                    Materials.Platinum.getDust(2),
                    WerkstoffLoader.RhodiumPlatedPalladium.get(OrePrefixes.dust, 2))
                .outputChances(chances(4, 2500))
                .fluidOutputChances(chances(0, 2500))
                .duration(409600)
                .eut(480),
            409600,
            "4");
        // QFT source recipe 5; fixed output chance 2500/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(WerkstoffLoader.LeachResidue.get(OrePrefixes.dust, 1))
                .itemOutputs(
                    Materials.Iridium.getDust(2),
                    Materials.Osmium.getDust(2),
                    WerkstoffLoader.Rhodium.get(OrePrefixes.dust, 2),
                    WerkstoffLoader.Ruthenium.get(OrePrefixes.dust, 2))
                .outputChances(chances(4, 2500))
                .fluidOutputChances(chances(0, 2500))
                .duration(409600)
                .eut(480),
            409600,
            "5");
        // QFT source recipe 6; fixed output chance 1666/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(Materials.Carbon.getDust(1))
                .fluidInputs(
                    Materials.Oxygen.getGas(250),
                    Materials.Hydrogen.getGas(250),
                    Materials.Chlorine.getGas(250),
                    Materials.Fluorine.getGas(250))
                .fluidOutputs(
                    Materials.Polyethylene.getMolten(576),
                    Materials.PolyvinylChloride.getMolten(288),
                    Materials.Polystyrene.getMolten(144),
                    Materials.Polytetrafluoroethylene.getMolten(288),
                    Materials.Epoxid.getMolten(144),
                    Materials.Polybenzimidazole.getMolten(144))
                .outputChances(chances(0, 1666))
                .fluidOutputChances(chances(6, 1666))
                .duration(1638400)
                .eut(32),
            1638400,
            "6");
        // QFT source recipe 7; fixed output chance 2500/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(Materials.Sulfur.getDust(1), Materials.Carbon.getDust(1))
                .fluidInputs(
                    Materials.Oxygen.getGas(500),
                    Materials.Hydrogen.getGas(500),
                    Materials.Chlorine.getGas(500))
                .fluidOutputs(
                    Materials.RubberSilicone.getMolten(288),
                    Materials.StyreneButadieneRubber.getMolten(288),
                    Materials.PolyphenyleneSulfide.getMolten(576),
                    Materials.Rubber.getMolten(1152))
                .outputChances(chances(0, 2500))
                .fluidOutputChances(chances(4, 2500))
                .duration(409600)
                .eut(120),
            409600,
            "7");
        // QFT source recipe 9; fixed output chance 2500/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(Materials.Lead.getDust(1), Materials.Bauxite.getDust(2), Materials.Tungstate.getDust(1))
                .itemOutputs(
                    Materials.Titanium.getDust(4),
                    Materials.TungstenSteel.getDust(4),
                    Materials.TungstenCarbide.getDust(4),
                    Materials.Indium.getDust(4))
                .outputChances(chances(4, 2500))
                .fluidOutputChances(chances(0, 2500))
                .duration(102400)
                .eut(1920),
            102400,
            "9");
        // QFT source recipe 10; fixed output chance 1666/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(Materials.Rutile.getDust(2), Materials.Scheelite.getDust(1), Materials.Ilmenite.getDust(1))
                .itemOutputs(
                    Materials.Titanium.getDust(4),
                    Materials.TungstenSteel.getDust(4),
                    Materials.Tantalum.getDust(4),
                    MaterialsElements.getInstance().RHENIUM.getDust(4),
                    Materials.Niobium.getDust(4),
                    MaterialsElements.getInstance().HAFNIUM.getDust(4))
                .outputChances(chances(6, 1666))
                .fluidOutputChances(chances(0, 1666))
                .duration(102400)
                .eut(1920),
            102400,
            "10");
        // QFT source recipe 11; fixed output chance 1666/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(Materials.Thorium.getDust(1), Materials.Uranium.getDust(1))
                .itemOutputs(
                    MaterialsElements.getInstance().THORIUM232.getDust(2),
                    MaterialsElements.getInstance().URANIUM233.getDust(2),
                    Materials.Uranium235.getDust(2),
                    MaterialsElements.getInstance().PLUTONIUM238.getDust(2),
                    Materials.Plutonium.getDust(2),
                    Materials.Plutonium241.getDust(2))
                .outputChances(chances(6, 1666))
                .fluidOutputChances(chances(0, 1666))
                .duration(409600)
                .eut(480),
            409600,
            "11");
        // QFT source recipe 13; fixed output chance 2000/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(Materials.Bastnasite.getDust(1))
                .itemOutputs(
                    Materials.Holmium.getDust(2),
                    Materials.Cerium.getDust(2),
                    Materials.Samarium.getDust(2),
                    Materials.Gadolinium.getDust(2),
                    Materials.Lanthanum.getDust(2))
                .outputChances(chances(5, 2000))
                .fluidOutputChances(chances(0, 2000))
                .duration(409600)
                .eut(1920),
            409600,
            "13");
        // QFT source recipe 14; fixed output chance 2000/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(WerkstoffMaterialPool.CeriumRichMixture.get(OrePrefixes.dust, 1))
                .itemOutputs(
                    Materials.Holmium.getDust(4),
                    Materials.Cerium.getDust(4),
                    Materials.Samarium.getDust(4),
                    Materials.Gadolinium.getDust(4),
                    Materials.Lanthanum.getDust(4))
                .outputChances(chances(5, 2000))
                .fluidOutputChances(chances(0, 2000))
                .duration(102400)
                .eut(7680),
            102400,
            "14");
        // QFT source recipe 16; fixed output chance 3333/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(
                    GTOreDictUnificator.get(OrePrefixes.shard, Materials.Prismarine, 1),
                    GTOreDictUnificator.get(OrePrefixes.dust, Materials.NaquadahEnriched, 2))
                .itemOutputs(GTOreDictUnificator.get(OrePrefixes.dust, Materials.PrismaticNaquadah, 1))
                .fluidInputs(
                    Materials.Hydrogen.getGas(125),
                    Materials.Oxygen.getGas(125),
                    Materials.Nitrogen.getGas(1500))
                .fluidOutputs(Materials.PrismaticAcid.getFluid(2000), Materials.Strontium.getMolten(144))
                .outputChances(chances(1, 3333))
                .fluidOutputChances(chances(2, 3333))
                .duration(102400)
                .eut(7680),
            102400,
            "16");
        // QFT source recipe 17; fixed output chance 3333/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(
                    Materials.Calcium.getDust(1),
                    Materials.MeatRaw.getDust(1),
                    getModItem(NewHorizonsCoreMod.ID, "GTNHBioItems", 1, 2))
                .itemOutputs(ItemList.Circuit_Chip_Stemcell.get(64))
                .fluidOutputs(
                    Materials.GrowthMediumRaw.getFluid(32000),
                    Materials.GrowthMediumSterilized.getFluid(16000))
                .outputChances(chances(1, 3333))
                .fluidOutputChances(chances(2, 3333))
                .duration(409600)
                .eut(7680),
            409600,
            "17");
        // QFT source recipe 18; fixed output chance 2500/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(Materials.Carbon.getDust(2), Materials.Osmium.getDust(1))
                .fluidInputs(Materials.Hydrogen.getGas(500), Materials.Nitrogen.getGas(500))
                .fluidOutputs(
                    Materials.Xenoxene.getFluid(500),
                    Materials.RadoxPolymer.getMolten(288),
                    Materials.RadoxHeavy.getFluid(500),
                    Materials.Kevlar.getMolten(288))
                .outputChances(chances(0, 2500))
                .fluidOutputChances(chances(4, 2500))
                .duration(409600)
                .eut(30720),
            409600,
            "18");
        // QFT source recipe 19; fixed output chance 3333/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(getModItem("gregtech", "gt.comb", 1, 161), Materials.Carbon.getDust(2))
                .fluidInputs(Materials.Nitrogen.getGas(500), Materials.Hydrogen.getGas(500))
                .fluidOutputs(
                    Materials.PolyurethaneResin.getFluid(1000),
                    Materials.LiquidCrystalKevlar.getFluid(144),
                    Materials.Kevlar.getMolten(288))
                .outputChances(chances(0, 3333))
                .fluidOutputChances(chances(3, 3333))
                .duration(409600)
                .eut(30720),
            409600,
            "19");
        // QFT source recipe 20; fixed output chance 2500/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(
                    getModItem("gregtech", "gt.comb", 1, 69),
                    getModItem("gregtech", "gt.comb", 1, 164),
                    getModItem("gregtech", "gt.comb", 1, 72),
                    getModItem("gregtech", "gt.comb", 1, 70))
                .fluidOutputs(
                    Materials.Osmium.getMolten(1152),
                    Materials.Palladium.getMolten(1152),
                    Materials.Iridium.getMolten(1152),
                    Materials.Platinum.getMolten(1152))
                .outputChances(chances(0, 2500))
                .fluidOutputChances(chances(4, 2500))
                .duration(409600)
                .eut(480),
            409600,
            "20");
        // QFT source recipe 21; fixed output chance 2500/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(ItemList.Circuit_Chip_Stemcell.get(4), Materials.InfinityCatalyst.getDust(1))
                .itemOutputs(GTUtility.copyAmountUnsafe(512, ItemList.Circuit_Chip_Biocell.get(1)))
                .fluidOutputs(
                    MaterialMisc.MUTATED_LIVING_SOLDER.getFluidStack(4608),
                    Materials.BioMediumSterilized.getFluid(64000),
                    Materials.BioMediumRaw.getFluid(128000))
                .outputChances(chances(1, 2500))
                .fluidOutputChances(chances(3, 2500))
                .duration(6400)
                .eut(1966080),
            6400,
            "21");
        // QFT source recipe 22; fixed output chance 2500/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(GTOreDictUnificator.get("cropSeaweed", 4), Materials.Mytryl.getDust(1))
                .itemOutputs(
                    GTUtility.copyAmountUnsafe(128, getModItem(GalaxySpace.ID, "tcetiedandelions", 1, 4)),
                    getModItem(NewHorizonsCoreMod.ID, "TCetiESeaweedExtract", 1))
                .fluidInputs(FluidRegistry.getFluidStack("unknowwater", 1563))
                .fluidOutputs(
                    FluidRegistry.getFluidStack("seaweedbroth", 3125),
                    WerkstoffMaterialPool.Iodine.getFluidOrGas(4000))
                .outputChances(chances(2, 2500))
                .fluidOutputChances(chances(2, 2500))
                .duration(102400)
                .eut(122880),
            102400,
            "22");
        // QFT source recipe CropsNH; fixed output chance 3333/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(
                    CropsNHItemList.platinaLeaf.get(1),
                    CropsNHItemList.osmianthFlower.get(1),
                    CropsNHItemList.iridineFlower.get(1))
                .fluidOutputs(
                    Materials.Platinum.getMolten(144),
                    Materials.Osmium.getMolten(144),
                    Materials.Iridium.getMolten(144))
                .outputChances(chances(0, 3333))
                .fluidOutputChances(chances(3, 3333))
                .duration(1638400)
                .eut(120),
            1638400,
            "CropsNH");
        // QFT source recipe GTNL; fixed output chance 2500/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(
                    Materials.Lead.getDust(1),
                    Materials.Bauxite.getDust(2),
                    WerkstoffLoader.Ferberite.get(OrePrefixes.dust, 1))
                .itemOutputs(
                    Materials.Titanium.getDust(4),
                    Materials.TungstenSteel.getDust(4),
                    Materials.TungstenCarbide.getDust(4),
                    Materials.Indium.getDust(4))
                .outputChances(chances(4, 2500))
                .fluidOutputChances(chances(0, 2500))
                .duration(102400)
                .eut(1920),
            102400,
            "GTNL");
        // QFT source recipe GTRH-water-1; fixed output chance 2500/10000.
        add(
            GTRecipeBuilder.builder()
                .fluidInputs(Materials.Water.getFluid(157))
                .fluidOutputs(
                    Materials.Grade1PurifiedWater.getFluid(79),
                    Materials.Grade2PurifiedWater.getFluid(40),
                    Materials.Grade3PurifiedWater.getFluid(20),
                    Materials.Grade4PurifiedWater.getFluid(4))
                .circuit(1)
                .outputChances(chances(0, 2500))
                .fluidOutputChances(chances(4, 2500))
                .duration(26214400)
                .eut(32),
            26214400,
            "GTRH-water-1");
        // QFT source recipe GTRH-water-2; fixed output chance 2500/10000.
        add(
            GTRecipeBuilder.builder()
                .fluidInputs(Materials.Water.getFluid(40))
                .fluidOutputs(
                    Materials.Grade5PurifiedWater.getFluid(20),
                    Materials.Grade6PurifiedWater.getFluid(10),
                    Materials.Grade7PurifiedWater.getFluid(5),
                    Materials.Grade8PurifiedWater.getFluid(1))
                .circuit(2)
                .outputChances(chances(0, 2500))
                .fluidOutputChances(chances(4, 2500))
                .duration(419430400)
                .eut(32),
            419430400,
            "GTRH-water-2");
        // QFT source recipe TST; fixed output chance 2000/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(WerkstoffMaterialPool.SamariumOreConcentrate.get(OrePrefixes.dust, 1))
                .itemOutputs(
                    Materials.Samarium.getDust(2),
                    Materials.Gadolinium.getDust(2),
                    Materials.Phosphorus.getDust(2),
                    Materials.Thorium.getDust(2),
                    WerkstoffLoader.Thorium232.get(OrePrefixes.dust, 2))
                .outputChances(chances(5, 2000))
                .fluidOutputChances(chances(0, 2000))
                .duration(409600)
                .eut(1920),
            409600,
            "TST");
        // QFT source recipe OTH-machines; fixed output chance 1250/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(
                    getModItem("gregtech", "gt.blockmachines", 0, 31027),
                    getModItem("gregtech", "gt.blockmachines", 0, 15560))
                .itemOutputs(
                    getModItem("miscutils", "itemDustRhenium", 1),
                    getModItem("miscutils", "itemDustThallium", 1),
                    getModItem("miscutils", "itemDustSelenium", 1),
                    getModItem("miscutils", "itemDustGermanium", 1),
                    getModItem("gregtech", "gt.metaitem.01", 1, 2044),
                    getModItem("gregtech", "gt.metaitem.01", 1, 2073))
                .fluidInputs(
                    FluidRegistry.getFluidStack("pineoil", 8),
                    FluidRegistry.getFluidStack("molten.helicopter", 2))
                .fluidOutputs(
                    FluidRegistry.getFluidStack("water", 7696),
                    FluidRegistry.getFluidStack("mud.red.slurry", 119989))
                .outputChances(chances(6, 1250))
                .fluidOutputChances(chances(2, 1250))
                .duration(31488)
                .eut(480950),
            31488,
            "OTH-machines");
        // QFT source recipe OTH-wood; fixed output chance 5000/10000.
        add(
            GTRecipeBuilder.builder()
                .itemInputs(
                    Materials.Iron.getDust(1),
                    Materials.Steel.getDust(1),
                    Materials.Wood.getDust(1),
                    new ItemStack(Blocks.leaves, 1))
                .itemOutputs(Materials.IronWood.getDust(1), Materials.Steeleaf.getDust(1))
                .outputChances(chances(2, 5000))
                .fluidOutputChances(chances(0, 5000))
                .duration(466944)
                .eut(30060),
            466944,
            "OTH-wood");
    }

    private static int[] chances(int count, int chance) {
        int[] result = new int[count];
        Arrays.fill(result, chance);
        return result;
    }

    private static void add(GTRecipeBuilder builder, int duration, String source) {
        Collection<GTRecipe> recipes = builder.addTo(SmallQuantumRecipes.MAP);
        if (recipes.isEmpty()) com.gtnhadjust.GTNHAdjust.LOG.warn("Fixed small quantum recipe {} was rejected", source);
        else {
            int adjusted = SmallQuantumRules
                .recipeDuration(duration, com.gtnhadjust.GTNHAdjust.smallQuantumRecipeTimeMultiplier);
            for (GTRecipe recipe : recipes) recipe.mDuration = adjusted;
            if (com.gtnhadjust.GTNHAdjust.debug) com.gtnhadjust.GTNHAdjust.LOG.info(
                "Registered small quantum recipe {} (base {} ticks, adjusted {} ticks)",
                source,
                duration,
                adjusted);
        }
    }
}
