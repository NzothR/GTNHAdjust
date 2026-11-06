package com.gtnhadjust.collector;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.google.gson.Gson;
import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectionBackend;
import com.xyp.gtneverreset.utils.enums.ModsItemlist;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.enums.StoneType;
import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class SuperFragmentRecipes {

    public static final int ITEM_OUTPUTS = 93;
    public static final int FLUID_OUTPUTS = 8;
    public static final RecipeMap<FragmentWorldCollectionBackend> MAP = RecipeMapBuilder
        .of("gtnhadjust.recipe.super_fragment_collection", FragmentWorldCollectionBackend::new)
        .maxIO(5, ITEM_OUTPUTS, 1, FLUID_OUTPUTS)
        .minInputs(1, 0)
        .progressBar(GTUITextures.PROGRESSBAR_MACERATE)
        .progressBarPos(78, 25)
        .neiHandlerInfo(
            builder -> builder.setDisplayStack(SuperFragmentRegistration.controller())
                .setHeight(344)
                .setMultipleWidgetsAllowed(false))
        .frontend(SuperFragmentFrontend::new)
        .build();

    private SuperFragmentRecipes() {}

    /** Create the recipe category before NEI discovers handlers, after the controller exists. */
    public static void initialize() {
        if (MAP == null) throw new IllegalStateException("Missing super fragment recipe map");
    }

    public static void register() {
        String path = "/assets/gtnhadjust/recipes/super_fragment_world.json";
        try (InputStream stream = SuperFragmentRecipes.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing " + path);
            Data data = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Data.class);
            int registered = 0;
            for (Entry entry : data.recipes) {
                register(entry);
                registered++;
            }
            GTNHAdjust.LOG.info("Registered {} static super fragment collector recipes", registered);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot register static super fragment collector recipes", error);
        }
    }

    private static void register(Entry entry) {
        List<ItemStack> inputs = new ArrayList<>();
        List<Integer> inputChances = new ArrayList<>();
        ItemStack dimension = required(
            ModsItemlist.valueOf(entry.dimension)
                .get(0),
            entry.dimension);
        inputs.add(dimension);
        inputChances.add(10000);
        if ("fluids".equals(entry.mode)) {
            inputs.add(
                required(
                    GTOreDictUnificator.get(OrePrefixes.toolHeadDrill, Materials.get(entry.drill), 1),
                    entry.drill));
            inputChances.add(entry.drillChance);
        }
        if (entry.extraSteelAndLubricant) {
            inputs.add(required(Materials.Steel.getDust(1), "steel dust"));
            inputChances.add(10000);
        }
        ItemStack circuit = GTUtility.getIntegratedCircuit(1);
        circuit.stackSize = 0;
        inputs.add(circuit);
        inputChances.add(10000);

        List<ItemStack> outputs = new ArrayList<>();
        List<Integer> chances = new ArrayList<>();
        for (ItemOutput output : entry.outputs) {
            ItemStack item = resolveItem(output);
            if (item == null) {
                GTNHAdjust.LOG.warn(
                    "Super fragment collector {}: unavailable output {}:{}",
                    entry.dimension,
                    output.kind,
                    output.name);
                continue;
            }
            // GTUtility.copyAmount and several registry helpers clamp counts to 64.
            // Preserve the summed recipe quantity; GT's parallel output code splits large stacks safely.
            item.stackSize = output.amount;
            boolean found = false;
            for (int i = 0; i < outputs.size(); i++) {
                ItemStack previous = outputs.get(i);
                if (previous.isItemEqual(item) && ItemStack.areItemStackTagsEqual(previous, item)) {
                    previous.stackSize = Math.addExact(previous.stackSize, item.stackSize);
                    chances.set(i, Math.max(chances.get(i), output.chance));
                    found = true;
                    break;
                }
            }
            if (!found) {
                outputs.add(item);
                chances.add(output.chance);
            }
        }
        List<FluidStack> fluids = new ArrayList<>();
        List<Integer> fluidChances = new ArrayList<>();
        for (FluidOutput output : entry.fluids) {
            FluidStack stack = resolveFluid(output.name, output.amount);
            if (stack == null) {
                GTNHAdjust.LOG.warn("Super fragment collector {}: unavailable fluid {}", entry.dimension, output.name);
                continue;
            }
            boolean found = false;
            for (int i = 0; i < fluids.size(); i++) {
                FluidStack previous = fluids.get(i);
                if (previous.isFluidEqual(stack)) {
                    previous.amount = Math.addExact(previous.amount, stack.amount);
                    fluidChances.set(i, Math.max(fluidChances.get(i), output.chance));
                    found = true;
                    break;
                }
            }
            if (!found) {
                fluids.add(stack);
                fluidChances.add(output.chance);
            }
        }
        if (outputs.size() > ITEM_OUTPUTS || fluids.size() > FLUID_OUTPUTS)
            throw new IllegalStateException("Recipe exceeds display limits: " + entry.dimension);
        GTRecipeBuilder builder = GTRecipeBuilder.builder()
            .itemInputs(
                inputs.toArray(new ItemStack[0]),
                inputChances.stream()
                    .mapToInt(Integer::intValue)
                    .toArray())
            .itemOutputs(
                outputs.toArray(new ItemStack[0]),
                chances.stream()
                    .mapToInt(Integer::intValue)
                    .toArray())
            .fluidOutputs(fluids.toArray(new FluidStack[0]))
            .fluidOutputChances(
                fluidChances.stream()
                    .mapToInt(Integer::intValue)
                    .toArray())
            .duration(entry.duration)
            .eut(entry.eut);
        if (entry.extraSteelAndLubricant) builder.fluidInputs(required(Materials.Lubricant.getFluid(100), "lubricant"));
        if (builder.addTo(MAP)
            .isEmpty())
            throw new IllegalStateException(
                "Rejected recipe " + entry.dimension + "/" + entry.mode + "/" + entry.drill);
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "Super fragment recipe {} {} {}: {} items, {} fluids, {} ticks, {} EU/t",
            entry.dimension,
            entry.mode,
            entry.drill,
            outputs.size(),
            fluids.size(),
            entry.duration,
            entry.eut);
    }

    private static ItemStack resolveItem(ItemOutput value) {
        switch (value.kind) {
            case "ore": {
                ItemStack raw = ore("rawOre" + value.name, 1);
                return raw == null ? ore("crushed" + value.name, 1) : raw;
            }
            case "oreDust":
                return ore("dust" + value.name, 1);
            case "dust":
                return Materials.get(value.name)
                    .getDust(1);
            case "stoneDust":
                return StoneType.Deepslate.getDust(true, 1);
            case "mod":
                return ModsItemlist.valueOf(value.name)
                    .get(1);
            case "ic2":
                return GTModHandler.getIC2Item(value.name, 1);
            case "item": {
                net.minecraft.item.Item item = GameRegistry.findItem("minecraft", value.name);
                return item == null ? null : new ItemStack(item, 1, value.meta);
            }
            case "block": {
                net.minecraft.block.Block block = GameRegistry.findBlock("minecraft", value.name);
                return block == null ? null : new ItemStack(block, 1, value.meta);
            }
            default:
                throw new IllegalArgumentException("Unknown output type " + value.kind);
        }
    }

    private static ItemStack ore(String name, int amount) {
        for (ItemStack item : OreDictionary.getOres(name, false))
            if (item != null && item.getItem() != null) return GTUtility.copyAmount(amount, item);
        return null;
    }

    private static FluidStack resolveFluid(String name, int amount) {
        String material = "RawOil".equals(name) ? "Oil" : "Oil".equals(name) ? "OilMedium" : name;
        Materials m = Materials.get(material);
        if (m != Materials._NULL) {
            FluidStack f = m.getFluid(amount);
            if (f == null) f = m.getGas(amount);
            if (f != null) return f;
        }
        String id = "RawOil".equals(name) ? "oil" : "Oil".equals(name) ? "oilmedium" : name.toLowerCase(Locale.ROOT);
        FluidStack f = FluidRegistry.getFluidStack(id, amount);
        return f == null ? FluidRegistry.getFluidStack("fluid." + id, amount) : f;
    }

    private static <T> T required(T value, String name) {
        if (value == null) throw new IllegalStateException("Missing " + name);
        return value;
    }

    private static final class Data {

        Entry[] recipes;
    }

    private static final class Entry {

        String dimension, mode, drill;
        int drillChance, duration, eut;
        boolean extraSteelAndLubricant;
        ItemOutput[] outputs;
        FluidOutput[] fluids;
    }

    private static final class ItemOutput {

        String kind, name;
        int meta, amount, chance;
    }

    private static final class FluidOutput {

        String name;
        int amount, chance;
    }
}
