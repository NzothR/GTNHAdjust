package com.gtnhadjust.recipe;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnhadjust.GTNHAdjust;

import forestry.api.core.ForestryAPI;
import forestry.api.recipes.ICarpenterRecipe;
import forestry.api.recipes.IDescriptiveRecipe;
import forestry.api.recipes.RecipeManagers;
import gregtech.api.enums.GTValues;
import gregtech.api.objects.OreDictItemStack;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

public final class CarpenterRecipes {

    private CarpenterRecipes() {}

    public static void register(File configDirectory) {
        File circuitFile = new File(configDirectory, "GTNHAdjust-carpenter-circuits.cfg");
        Configuration cache = new Configuration(circuitFile);
        cache.load();
        Map<String, Entry> frozen;
        try {
            frozen = CarpenterRecipeCache.load(new File(configDirectory, "GTNHAdjust-carpenter-recipes.dat"));
        } catch (Exception error) {
            GTNHAdjust.LOG.error("Cannot read carpenter recipe cache; conversion aborted to preserve patterns", error);
            return;
        }
        Map<String, List<Entry>> groups = new TreeMap<>();
        int skipped = 0;
        List<Entry> discovered = new ArrayList<>();
        for (ICarpenterRecipe recipe : RecipeManagers.carpenterManager.recipes()) {
            try {
                discovered.add(convert(recipe));
            } catch (RuntimeException error) {
                GTNHAdjust.LOG.warn("Cannot convert carpenter recipe: {}", error.getMessage());
                skipped++;
            }
        }
        discovered.sort(
            Comparator.comparing((Entry entry) -> entry.inputKey)
                .thenComparing(entry -> entry.outputKey)
                .thenComparingInt(entry -> entry.duration)
                .thenComparingInt(entry -> entry.eut));
        for (Entry entry : discovered) frozen.putIfAbsent(digest(entry.inputKey + "\n" + entry.outputKey), entry);
        try {
            CarpenterRecipeCache.save(new File(configDirectory, "GTNHAdjust-carpenter-recipes.dat"), frozen);
        } catch (Exception error) {
            GTNHAdjust.LOG.error("Cannot save carpenter recipe cache; conversion aborted to preserve patterns", error);
            return;
        }
        for (Entry entry : frozen.values()) groups.computeIfAbsent(entry.inputKey, ignored -> new ArrayList<>())
            .add(entry);
        int registered = 0;
        for (List<Entry> entries : groups.values()) {
            entries.sort(Comparator.comparing(entry -> entry.outputKey));
            Map<String, Entry> outputs = new LinkedHashMap<>();
            for (Entry entry : entries) outputs.putIfAbsent(entry.outputKey, entry);
            Entry representative = entries.get(0);
            List<GTRecipe> existing = findExactExisting(representative);
            boolean generatedCopy = false;
            for (GTRecipe match : existing) {
                boolean found = false;
                for (GTRecipe original : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                    if (match == original) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    generatedCopy = true;
                    break;
                }
            }
            if (generatedCopy) {
                GTNHAdjust.LOG
                    .warn("Cannot safely distinguish dynamic assembler recipe for {}", representative.outputKey);
                skipped += outputs.size();
                continue;
            }
            Set<String> existingOutputs = new HashSet<>();
            for (GTRecipe recipe : existing) existingOutputs.add(outputKey(recipe));
            outputs.keySet()
                .removeAll(existingOutputs);
            if (outputs.isEmpty()) continue;

            String category = "group_" + digest(representative.inputKey);
            Set<Integer> occupied = occupiedCircuits(representative);
            boolean needsCircuits = outputs.size() + existingOutputs.size() > 1 || cache.hasCategory(category)
                || !occupied.isEmpty();
            if (representative.inputs.size() + (needsCircuits ? 1 : 0) > 9 || existing.stream()
                .anyMatch(r -> r.mInputs.length + (needsCircuits ? 1 : 0) > 9)) {
                GTNHAdjust.LOG.warn(
                    "Carpenter input group exceeds the assembler's nine input slots: {}",
                    representative.inputKey);
                skipped += outputs.size();
                continue;
            }
            Map<String, Integer> assignments = new TreeMap<>();
            if (needsCircuits) {
                List<String> keys = new ArrayList<>();
                for (GTRecipe recipe : existing) keys.add("existing_" + digest(existingKey(recipe)));
                for (Entry entry : outputs.values()) keys.add("carpenter_" + digest(entry.outputKey));
                Collections.sort(keys);
                if (!assignCircuits(cache, category, keys, occupied, assignments)) {
                    GTNHAdjust.LOG
                        .warn("No free circuit configurations for carpenter input group {}", representative.inputKey);
                    skipped += outputs.size();
                    continue;
                }
                if (!persistCircuits(cache, circuitFile, category, assignments)) {
                    GTNHAdjust.LOG.error(
                        "Cannot persist circuit assignments for {}; skipping the group",
                        representative.inputKey);
                    skipped += outputs.size();
                    continue;
                }
                for (GTRecipe recipe : existing) {
                    int circuit = assignments.get("existing_" + digest(existingKey(recipe)));
                    addCircuitToExisting(recipe, circuit);
                    GTNHAdjust.LOG.info(
                        "Assembler recipe {} now requires circuit {} to distinguish carpenter recipes",
                        outputKey(recipe),
                        circuit);
                }
            }
            for (Entry entry : outputs.values()) {
                List<Object> inputs = new ArrayList<>();
                for (Ingredient ingredient : entry.inputs) inputs.add(ingredient.asBuilderInput());
                int circuit = needsCircuits ? assignments.get("carpenter_" + digest(entry.outputKey)) : 0;
                if (needsCircuits) inputs.add(GTUtility.getIntegratedCircuit(circuit));
                GTRecipeBuilder builder = GTValues.RA.stdBuilder()
                    .itemInputs(inputs.toArray(new Object[0]))
                    .itemOutputs(entry.output.copy())
                    .duration(entry.duration)
                    .eut(entry.eut);
                if (entry.fluid != null) builder.fluidInputs(entry.fluid.copy());
                GTRecipe converted = builder.buildWithAlt()
                    .orElse(null);
                if (converted == null || RecipeMaps.assemblerRecipes.addRecipe(converted, true, false, false) == null) {
                    GTNHAdjust.LOG
                        .warn("Assembler rejected carpenter recipe {} (circuit {})", entry.outputKey, circuit);
                    skipped++;
                } else {
                    registered++;
                    if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                        "Converted carpenter recipe {} circuit {} duration {} EU/t {}",
                        entry.outputKey,
                        circuit,
                        entry.duration,
                        entry.eut);
                }
            }
        }
        if (cache.hasChanged()) cache.save();
        GTNHAdjust.LOG.info(
            "Carpenter to assembler: {} registered, {} skipped; source recipes {}",
            registered,
            skipped,
            RecipeManagers.carpenterManager.recipes()
                .size());
    }

    private static boolean persistCircuits(Configuration cache, File file, String category,
        Map<String, Integer> assignments) {
        cache.save();
        if (!file.isFile()) return false;
        try {
            Configuration saved = new Configuration(file);
            saved.load();
            if (!saved.hasCategory(category)) return false;
            for (Map.Entry<String, Integer> entry : assignments.entrySet()) {
                Property property = saved.getCategory(category)
                    .get(entry.getKey());
                if (property == null || property.getInt(-1) != entry.getValue()) return false;
            }
            return true;
        } catch (RuntimeException error) {
            GTNHAdjust.LOG.error("Cannot verify saved carpenter circuit assignments", error);
            return false;
        }
    }

    private static boolean assignCircuits(Configuration cache, String category, List<String> keys,
        Set<Integer> occupied, Map<String, Integer> result) {
        ConfigCategory saved = cache.getCategory(category);
        Set<Integer> used = new HashSet<>(occupied);
        for (Property property : saved.values()) {
            int value = property.getInt(-1);
            if (value >= 1 && value <= 24) used.add(value);
        }
        for (String key : keys) {
            if (saved.containsKey(key)) {
                int value = saved.get(key)
                    .getInt(-1);
                if (value < 1 || value > 24 || occupied.contains(value) || result.containsValue(value)) return false;
                result.put(key, value);
            } else {
                int value = 1;
                while (value <= 24 && used.contains(value)) value++;
                if (value > 24) return false;
                used.add(value);
                result.put(key, value);
            }
        }
        for (Map.Entry<String, Integer> assignment : result.entrySet()) {
            cache
                .get(
                    category,
                    assignment.getKey(),
                    assignment.getValue(),
                    "Stable circuit assignment; do not edit while using saved patterns.")
                .set(assignment.getValue());
        }
        return true;
    }

    private static Set<Integer> occupiedCircuits(Entry entry) {
        Set<Integer> result = new HashSet<>();
        for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
            if (!matchesMaterials(entry, recipe)) continue;
            for (ItemStack input : recipe.mInputs) {
                if (input != null && input.stackSize == 0 && GTUtility.isAnyIntegratedCircuit(input)) {
                    int circuit = input.getItemDamage();
                    if (circuit >= 1 && circuit <= 24) result.add(circuit);
                }
            }
        }
        return result;
    }

    private static List<GTRecipe> findExactExisting(Entry entry) {
        return RecipeMaps.assemblerRecipes.getAllRecipes()
            .stream()
            .filter(recipe -> {
                for (ItemStack input : recipe.mInputs) if (input != null && input.stackSize <= 0) return false;
                return matchesMaterials(entry, recipe);
            })
            .collect(Collectors.toList());
    }

    private static boolean matchesMaterials(Entry entry, GTRecipe recipe) {
        if (!recipe.mEnabled || recipe.mFakeRecipe) return false;
        int fluids = entry.fluid == null ? 0 : 1;
        if (recipe.mFluidInputs.length != fluids) return false;
        if (fluids == 1 && (!recipe.mFluidInputs[0].isFluidEqual(entry.fluid)
            || recipe.mFluidInputs[0].amount != entry.fluid.amount)) return false;
        List<Ingredient> source = new ArrayList<>();
        for (Ingredient ingredient : entry.inputs) for (int i = 0; i < ingredient.count; i++) source.add(ingredient);
        List<Integer> targetSlots = new ArrayList<>();
        for (int i = 0; i < recipe.mInputs.length; i++) {
            ItemStack input = recipe.mInputs[i];
            if (input == null || input.stackSize == 0) continue;
            if (input.stackSize < 0 || targetSlots.size() + input.stackSize > source.size()) return false;
            for (int j = 0; j < input.stackSize; j++) targetSlots.add(i);
        }
        if (source.size() != targetSlots.size()) return false;
        return matchUnits(source, recipe, targetSlots, 0, new boolean[targetSlots.size()]);
    }

    private static boolean matchUnits(List<Ingredient> source, GTRecipe recipe, List<Integer> targetSlots, int index,
        boolean[] used) {
        if (index == source.size()) return true;
        Ingredient ingredient = source.get(index);
        List<ItemStack> choices = ingredient.oreName == null ? ingredient.alternatives
            : OreDictionary.getOres(ingredient.oreName);
        for (int i = 0; i < targetSlots.size(); i++) {
            if (used[i] || !intersects(choices, recipe, targetSlots.get(i))) continue;
            used[i] = true;
            if (matchUnits(source, recipe, targetSlots, index + 1, used)) return true;
            used[i] = false;
        }
        return false;
    }

    private static boolean intersects(List<ItemStack> source, GTRecipe recipe, int slot) {
        List<ItemStack> target = new ArrayList<>();
        target.add(recipe.mInputs[slot]);
        if (recipe instanceof GTRecipe.GTRecipe_WithAlt) {
            GTRecipe.GTRecipe_WithAlt alt = (GTRecipe.GTRecipe_WithAlt) recipe;
            if (alt.mOreDictAlt != null && slot < alt.mOreDictAlt.length && alt.mOreDictAlt[slot] != null)
                target.addAll(Arrays.asList(alt.mOreDictAlt[slot]));
            if (alt.mOreDictIds != null && slot < alt.mOreDictIds.length && alt.mOreDictIds[slot] >= 0)
                target.addAll(OreDictionary.getOres(OreDictionary.getOreName(alt.mOreDictIds[slot])));
        }
        for (ItemStack a : source) for (ItemStack b : target) {
            if (GTUtility.areStacksEqual(a, b, !recipe.isNBTSensitive)) return true;
        }
        return false;
    }

    private static void addCircuitToExisting(GTRecipe recipe, int circuit) {
        RecipeMaps.assemblerRecipes.getBackend()
            .removeRecipe(recipe);
        int size = recipe.mInputs.length;
        recipe.mInputs = Arrays.copyOf(recipe.mInputs, size + 1);
        recipe.mInputs[size] = GTUtility.getIntegratedCircuit(circuit);
        if (recipe.mInputChances != null) {
            recipe.mInputChances = Arrays.copyOf(recipe.mInputChances, size + 1);
            recipe.mInputChances[size] = 10000;
        }
        if (recipe instanceof GTRecipe.GTRecipe_WithAlt) {
            GTRecipe.GTRecipe_WithAlt alt = (GTRecipe.GTRecipe_WithAlt) recipe;
            if (alt.mOreDictAlt != null) {
                alt.mOreDictAlt = Arrays.copyOf(alt.mOreDictAlt, size + 1);
                alt.mOreDictAlt[size] = new ItemStack[] { recipe.mInputs[size] };
            }
            if (alt.mOreDictIds != null) {
                alt.mOreDictIds = Arrays.copyOf(alt.mOreDictIds, size + 1);
                alt.mOreDictIds[size] = -1;
            }
        }
        RecipeMaps.assemblerRecipes.add(recipe);
    }

    private static Entry convert(ICarpenterRecipe recipe) {
        IDescriptiveRecipe grid = recipe.getCraftingGridRecipe();
        if (grid == null || grid.getRecipeOutput() == null)
            throw new IllegalArgumentException("missing grid or output");
        if (grid.preserveNBT())
            throw new IllegalArgumentException("dynamic NBT output " + stackKey(grid.getRecipeOutput()));
        int time = recipe.getPackagingTime();
        if (time <= 0 || time > Integer.MAX_VALUE / 204)
            throw new IllegalArgumentException("unsupported packaging time " + time);
        Map<String, Ingredient> ingredients = new TreeMap<>();
        for (Object ingredient : grid.getIngredients()) if (ingredient != null) addIngredient(ingredients, ingredient);
        if (recipe.getBox() != null) addIngredient(ingredients, recipe.getBox());
        if (ingredients.isEmpty()) throw new IllegalArgumentException("no input " + stackKey(grid.getRecipeOutput()));
        List<Ingredient> distinct = new ArrayList<>(ingredients.values());
        for (int i = 0; i < distinct.size(); i++) {
            for (int j = i + 1; j < distinct.size(); j++) {
                for (ItemStack a : distinct.get(i).alternatives) for (ItemStack b : distinct.get(j).alternatives) {
                    if (GTUtility.areStacksEqual(a, b, true)) {
                        throw new IllegalArgumentException(
                            "overlapping ingredient alternatives for " + stackKey(grid.getRecipeOutput()));
                    }
                }
            }
        }
        StringBuilder key = new StringBuilder();
        for (Ingredient ingredient : ingredients.values()) key.append(ingredient.count)
            .append('*')
            .append(ingredient.key)
            .append(';');
        FluidStack fluid = recipe.getFluidResource();
        if (fluid != null) key.append("fluid:")
            .append(
                fluid.getFluid()
                    .getName())
            .append(':')
            .append(fluid.amount)
            .append(':')
            .append(fluid.tag);
        int scaledRf = Math.round(204 * time * ForestryAPI.activeMode.getFloatSetting("energy.demand.modifier"));
        int rfPerWork = (int) Math.ceil(scaledRf / (double) time);
        int eut = Math.max(1, (int) Math.ceil(rfPerWork / 40.0));
        ItemStack output = grid.getRecipeOutput()
            .copy();
        return new Entry(
            key.toString(),
            new ArrayList<>(ingredients.values()),
            fluid == null ? null : fluid.copy(),
            output,
            stackKey(output) + "x" + output.stackSize,
            time * 5,
            eut);
    }

    private static void addIngredient(Map<String, Ingredient> ingredients, Object value) {
        List<ItemStack> choices = new ArrayList<>();
        String oreName = null;
        if (value instanceof ItemStack) choices.add((ItemStack) value);
        else if (value instanceof ItemStack[]) choices.addAll(Arrays.asList((ItemStack[]) value));
        else if (value instanceof List<?>) {
            for (Object item : (List<?>) value) {
                if (!(item instanceof ItemStack))
                    throw new IllegalArgumentException("unsupported ingredient alternative");
                choices.add((ItemStack) item);
            }
            for (String name : OreDictionary.getOreNames())
                if (OreDictionary.getOres(name) == value && (oreName == null || name.compareTo(oreName) < 0))
                    oreName = name;
        } else throw new IllegalArgumentException(
            "unsupported ingredient " + value.getClass()
                .getName());
        TreeMap<String, ItemStack> unique = new TreeMap<>();
        for (ItemStack choice : choices) if (choice != null && choice.getItem() != null) {
            if (choice.hasTagCompound() && !choice.getTagCompound()
                .hasNoTags()) {
                throw new IllegalArgumentException("NBT-sensitive carpenter ingredient " + stackKey(choice));
            }
            ItemStack copy = choice.copy();
            copy.stackSize = 1;
            unique.put(stackKey(copy), copy);
        }
        if (unique.isEmpty()) throw new IllegalArgumentException("empty ingredient alternatives");
        String key = oreName == null ? "items:" + String.join("|", unique.keySet()) : "ore:" + oreName;
        Ingredient existing = ingredients.get(key);
        if (existing != null) existing.count++;
        else ingredients.put(key, new Ingredient(key, oreName, new ArrayList<>(unique.values())));
    }

    private static String outputKey(GTRecipe recipe) {
        if (recipe.mOutputs == null || recipe.mOutputs.length == 0 || recipe.mOutputs[0] == null) return "<no output>";
        ItemStack output = recipe.mOutputs[0];
        String result = stackKey(output) + "x" + output.stackSize;
        if (recipe.mOutputs.length > 1 || (recipe.mFluidOutputs != null && recipe.mFluidOutputs.length > 0)) {
            result += ":additional_outputs";
        }
        return result;
    }

    private static String existingKey(GTRecipe recipe) {
        StringBuilder key = new StringBuilder(outputKey(recipe)).append(':')
            .append(recipe.mDuration)
            .append(':')
            .append(recipe.mEUt);
        for (ItemStack input : recipe.mInputs) if (input != null) key.append(';')
            .append(stackKey(input))
            .append('x')
            .append(input.stackSize);
        return key.toString();
    }

    private static String stackKey(ItemStack stack) {
        return Item.itemRegistry.getNameForObject(stack.getItem()) + ":"
            + stack.getItemDamage()
            + ":"
            + stack.getTagCompound();
    }

    private static String digest(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8))
            .toString()
            .replace("-", "");
    }

    static final class Ingredient {

        final String key;
        final String oreName;
        final List<ItemStack> alternatives;
        int count = 1;

        Ingredient(String key, String oreName, List<ItemStack> alternatives) {
            this.key = key;
            this.oreName = oreName;
            this.alternatives = alternatives;
        }

        ItemStack sample() {
            ItemStack stack = alternatives.get(0)
                .copy();
            stack.stackSize = count;
            return stack;
        }

        Object asBuilderInput() {
            if (alternatives.size() == 1 && oreName == null) return sample();
            String name = oreName;
            if (name == null) {
                name = "gtnhadjustCarpenter" + digest(key);
                if (!OreDictionary.doesOreNameExist(name))
                    for (ItemStack stack : alternatives) OreDictionary.registerOre(name, stack.copy());
            }
            return new OreDictItemStack(name, count);
        }
    }

    static final class Entry {

        final String inputKey;
        final List<Ingredient> inputs;
        final FluidStack fluid;
        final ItemStack output;
        final String outputKey;
        final int duration;
        final int eut;

        Entry(String inputKey, List<Ingredient> inputs, FluidStack fluid, ItemStack output, String outputKey,
            int duration, int eut) {
            this.inputKey = inputKey;
            this.inputs = inputs;
            this.fluid = fluid;
            this.output = output;
            this.outputKey = outputKey;
            this.duration = duration;
            this.eut = eut;
        }
    }
}
