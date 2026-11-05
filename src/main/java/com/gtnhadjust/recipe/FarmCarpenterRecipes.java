package com.gtnhadjust.recipe;

import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.gtnhadjust.GTNHAdjust;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;

final class FarmCarpenterRecipes {

    private FarmCarpenterRecipes() {}

    static int register(NBTTagList frozenRecipes) {
        Item farm = GameRegistry.findItem("Forestry", "ffarm");
        Item tubes = GameRegistry.findItem("Forestry", "thermionicTubes");
        FluidStack creosote = FluidRegistry.getFluidStack("creosote", 1000);
        NBTTagCompound basicCircuit = findBasicCircuit(frozenRecipes);
        if (farm == null || tubes == null || creosote == null || basicCircuit == null) {
            GTNHAdjust.LOG.warn(
                "Cannot register farm carpenter recipes: Forestry items, creosote or circuit alternatives missing");
            return 0;
        }
        int registered = 0;
        for (int variant = 0; variant < 11; variant++) {
            for (int type = 2; type <= 5; type++) {
                try {
                    ItemStack input = farmBlock(farm, 0, variant, 1);
                    ItemStack output = farmBlock(farm, type, variant, 2);
                    Object[] components = components(type, input, tubes, basicCircuit);
                    for (Object component : components) {
                        if (component == null) throw new IllegalArgumentException("missing farm recipe ingredient");
                    }
                    GTRecipe recipe = GTValues.RA.stdBuilder()
                        .itemInputs(components)
                        .itemOutputs(output)
                        .fluidInputs(creosote.copy())
                        .duration(300)
                        .eut(11)
                        .nbtSensitive()
                        .buildWithAlt()
                        .orElseThrow(() -> new IllegalArgumentException("invalid farm recipe"));
                    RecipeMaps.assemblerRecipes.add(recipe);
                    registered++;
                    if (GTNHAdjust.debug)
                        GTNHAdjust.LOG.info("Registered farm carpenter recipe variant {} type {}", variant, type);
                } catch (RuntimeException error) {
                    GTNHAdjust.LOG.warn(
                        "Cannot register farm carpenter recipe variant {} type {}: {}",
                        variant,
                        type,
                        error.getMessage());
                }
            }
        }
        return registered;
    }

    private static Object[] components(int type, ItemStack farm, Item tubes, NBTTagCompound basicCircuit) {
        ItemStack gear = GTOreDictUnificator.get(OrePrefixes.gearGt, Materials.Steel, type == 2 ? 4 : 1);
        ItemStack motor = ItemList.Electric_Motor_LV.get(1);
        if (gear == null || motor == null) throw new IllegalArgumentException("missing steel gear or LV motor");
        switch (type) {
            case 2:
                return new Object[] { farm, new ItemStack(tubes, 4, 2), gear, motor };
            case 3:
                return new Object[] { farm, new ItemStack(tubes, 4, 1), gear, ItemList.Conveyor_Module_LV.get(2), motor,
                    new ItemStack(Blocks.hopper) };
            case 4:
                return new Object[] { farm, new ItemStack(tubes, 4, 11), gear, ItemList.Electric_Pump_LV.get(2), motor,
                    GTOreDictUnificator.get(OrePrefixes.ring, Materials.Rubber, 1) };
            case 5:
                return new Object[] { farm, new ItemStack(tubes, 4, 4), gear,
                    CarpenterRecipes.readIngredient(basicCircuit, 2), motor,
                    GTOreDictUnificator.get(OrePrefixes.cableGt01, Materials.Tin, 1) };
            default:
                throw new IllegalArgumentException("invalid farm type");
        }
    }

    private static ItemStack farmBlock(Item item, int type, int variant, int count) {
        ItemStack result = new ItemStack(item, count, type);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("FarmBlock", variant);
        result.setTagCompound(tag);
        return result;
    }

    private static NBTTagCompound findBasicCircuit(NBTTagList recipes) {
        for (int i = 0; i < recipes.tagCount(); i++) {
            NBTTagList inputs = recipes.getCompoundTagAt(i)
                .getTagList("inputs", 10);
            for (int j = 0; j < inputs.tagCount(); j++) {
                NBTTagCompound input = inputs.getCompoundTagAt(j);
                if ("circuitBasic".equals(input.getString("ore"))) return input;
            }
        }
        return null;
    }
}
