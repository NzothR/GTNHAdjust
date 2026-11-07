package com.gtnhadjust.machine.collector;

import static gregtech.api.util.GTRecipeConstants.COMPRESSION_TIER;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gtnhadjust.GTNHAdjust;

import bartworks.system.material.Werkstoff;
import bartworks.system.material.WerkstoffLoader;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTUtility;

/** Fixed, deduplicated resource lists derived from GT-Not-Hard. */
final class SingularityResources {

    private static boolean unresolved;

    private SingularityResources() {}

    static void register() {
        String path = "/assets/gtnhadjust/recipes/gtnothard_singularity.json";
        try (InputStream stream = SingularityResources.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing " + path);
            Data data = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Data.class);
            int registered = 0;
            int compressedMarkers = 0;
            MaterialCatalog catalog = new MaterialCatalog();
            List<Map<String, List<ItemStack>>> tierItems = new ArrayList<>();
            List<Map<String, List<FluidStack>>> tierFluids = new ArrayList<>();
            List<List<ItemStack>> ingredients = new ArrayList<>();
            boolean[] complete = new boolean[10];
            java.util.Arrays.fill(complete, true);
            for (int i = 0; i < 10; i++) {
                tierItems.add(new LinkedHashMap<>());
                tierFluids.add(new LinkedHashMap<>());
                ingredients.add(new ArrayList<>());
            }
            for (Map.Entry<String, Map<String, Resource[]>> dimension : data.dimensions.entrySet()) {
                Integer tier = data.tiers.get(dimension.getKey());
                if (tier == null || tier < 0 || tier > 9)
                    throw new IllegalStateException("Invalid tier for dimension " + dimension.getKey());
                int eut = 8 << (tier * 2);
                Item marker = GameRegistry.findItem("gtneioreplugin", "blockDimensionDisplay_" + dimension.getKey());
                if (marker == null) {
                    GTNHAdjust.LOG.warn("Singularity: missing dimension marker {}", dimension.getKey());
                    unresolved = true;
                    complete[tier] = false;
                    continue;
                }
                ingredients.get(tier)
                    .add(new ItemStack(marker, 1, 0));
                Collection<GTRecipe> compression = GTRecipeBuilder.builder()
                    .itemInputs(new ItemStack(marker, 9, 0))
                    .itemOutputs(new ItemStack(GTNHAdjust.tierDimension, 1, tier))
                    .metadata(COMPRESSION_TIER, 2)
                    .duration(128)
                    .eut(512)
                    .addTo(RecipeMaps.compressorRecipes);
                if (compression.isEmpty()) {
                    GTNHAdjust.LOG
                        .warn("Singularity: compression recipe for dimension {} rejected", dimension.getKey());
                } else {
                    // GTNR's global recipe-speed adjustment must not alter this fixed base duration.
                    for (GTRecipe recipe : compression) recipe.mDuration = 128;
                    compressedMarkers += compression.size();
                }
                Map<String, Pool> pools = new LinkedHashMap<>();
                for (Map.Entry<String, Resource[]> mode : dimension.getValue()
                    .entrySet()) {
                    List<ItemStack> items = new ArrayList<>();
                    List<FluidStack> fluids = new ArrayList<>();
                    int missing = 0;
                    for (Resource resource : mode.getValue()) {
                        if ("item".equals(resource.kind)) {
                            ItemStack stack = item(resource);
                            if (stack == null) missing++;
                            else items.add(stack);
                        } else {
                            FluidStack stack = fluid(resource);
                            if (stack == null) missing++;
                            else fluids.add(stack);
                        }
                    }
                    if (missing > 0) {
                        unresolved = true;
                        GTNHAdjust.LOG.warn(
                            "Singularity {} {}: {} of {} source entries missing; remaining outputs used",
                            dimension.getKey(),
                            mode.getKey(),
                            missing,
                            mode.getValue().length);
                    }
                    pools.put(
                        mode.getKey(),
                        new Pool(items.toArray(new ItemStack[0]), fluids.toArray(new FluidStack[0])));
                }
                complementMaterialForms(pools, catalog);
                for (Map.Entry<String, Pool> mode : pools.entrySet()) {
                    Pool pool = mode.getValue();
                    if (pool.items.length == 0 && pool.fluids.length == 0) continue;
                    Collections.addAll(
                        tierItems.get(tier)
                            .computeIfAbsent(mode.getKey(), key -> new ArrayList<>()),
                        pool.items);
                    Collections.addAll(
                        tierFluids.get(tier)
                            .computeIfAbsent(mode.getKey(), key -> new ArrayList<>()),
                        pool.fluids);
                    if (registerPool(new ItemStack(marker, 0, 0), circuit(mode.getKey()), pool, eut)) registered++;
                }
            }
            Map<String, Set<String>> seenItems = new HashMap<>();
            Map<String, Set<String>> seenFluids = new HashMap<>();
            for (int tier = 0; tier < 10; tier++) {
                if (complete[tier] && !ingredients.get(tier)
                    .isEmpty()) {
                    Collection<GTRecipe> recipes = GTRecipeBuilder.builder()
                        .itemInputs(
                            ingredients.get(tier)
                                .toArray(new ItemStack[0]))
                        .itemOutputs(new ItemStack(GTNHAdjust.tierDimension, 1, tier))
                        .duration(128)
                        .eut(512)
                        .addTo(RecipeMaps.assemblerRecipes);
                    if (recipes.isEmpty()) GTNHAdjust.LOG.warn("Tier {} dimension assembler recipe rejected", tier);
                } else GTNHAdjust.LOG.warn("Tier {} dimension assembler recipe unavailable: missing markers", tier);
                for (String mode : new String[] { "ore", "gem", "dust", "ingot", "fluid" }) {
                    Set<String> itemKeys = seenItems.computeIfAbsent(mode, key -> new LinkedHashSet<>());
                    Set<String> fluidKeys = seenFluids.computeIfAbsent(mode, key -> new LinkedHashSet<>());
                    List<ItemStack> items = TierResourceDeduplication.takeNew(
                        tierItems.get(tier)
                            .getOrDefault(mode, Collections.emptyList()),
                        itemKeys,
                        SingularityResources::itemKey);
                    List<FluidStack> fluids = TierResourceDeduplication.takeNew(
                        tierFluids.get(tier)
                            .getOrDefault(mode, Collections.emptyList()),
                        fluidKeys,
                        stack -> stack.getFluid()
                            .getName());
                    if (!items.isEmpty() || !fluids.isEmpty()) {
                        Pool pool = new Pool(items.toArray(new ItemStack[0]), fluids.toArray(new FluidStack[0]));
                        if (registerPool(
                            new ItemStack(GTNHAdjust.tierDimension, 0, tier),
                            circuit(mode),
                            pool,
                            8 << (tier * 2))) registered++;
                    }
                }
            }
            GTNHAdjust.LOG.info("Registered {} GT-Not-Hard dimension resource pools", registered);
            GTNHAdjust.LOG.info("Registered {} black hole dimension marker compression recipes", compressedMarkers);
            if (unresolved && GTNHAdjust.debug) exportRegistry();
        } catch (Exception error) {
            throw new IllegalStateException("Cannot register GT-Not-Hard resource pools", error);
        }
    }

    /** Resolve material identity through the ore dictionary and registered material fluids. */
    private static final class MaterialCatalog {

        final Map<String, String> fluidMaterials = new HashMap<>();
        final Map<String, FluidStack> materialFluids = new HashMap<>();

        MaterialCatalog() {
            for (Materials material : Materials.values()) {
                String name = material.mName;
                indexFluid(name, material.getMolten(1), fluidMaterials, materialFluids);
                indexFluid(name, material.getFluid(1), fluidMaterials, materialFluids);
                indexFluid(name, material.getGas(1), fluidMaterials, materialFluids);
            }
            for (Werkstoff material : Werkstoff.werkstoffHashSet) {
                ItemStack sample = material.hasItemType(OrePrefixes.dust)
                    ? WerkstoffLoader.getCorrespondingItemStackUnsafe(OrePrefixes.dust, material, 1)
                    : null;
                if (sample == null && material.hasItemType(OrePrefixes.ingot))
                    sample = WerkstoffLoader.getCorrespondingItemStackUnsafe(OrePrefixes.ingot, material, 1);
                if (sample == null) continue;
                for (String name : materialNames(sample)) {
                    net.minecraftforge.fluids.Fluid molten = WerkstoffLoader.molten.get(material);
                    net.minecraftforge.fluids.Fluid fluid = WerkstoffLoader.fluids.get(material);
                    if (molten != null) indexFluid(name, new FluidStack(molten, 1), fluidMaterials, materialFluids);
                    if (fluid != null) indexFluid(name, new FluidStack(fluid, 1), fluidMaterials, materialFluids);
                }
            }
        }
    }

    private static void complementMaterialForms(Map<String, Pool> pools, MaterialCatalog catalog) {
        Map<String, String> fluidMaterials = catalog.fluidMaterials;
        Map<String, FluidStack> materialFluids = catalog.materialFluids;
        Set<String> materials = new LinkedHashSet<>();
        for (String mode : new String[] { "dust", "ingot", "fluid" }) {
            Pool pool = pools.get(mode);
            if (pool == null) continue;
            for (ItemStack stack : pool.items) materials.addAll(materialNames(stack));
            for (FluidStack stack : pool.fluids) {
                String name = fluidMaterials.get(
                    stack.getFluid()
                        .getName());
                if (name != null) materials.add(name);
            }
        }
        for (String mode : new String[] { "dust", "ingot", "fluid" }) {
            Pool pool = pools.getOrDefault(mode, new Pool(new ItemStack[0], new FluidStack[0]));
            List<ItemStack> items = uniqueItems(pool.items);
            List<FluidStack> fluids = uniqueFluids(pool.fluids);
            Set<String> present = new LinkedHashSet<>();
            if ("fluid".equals(mode)) {
                for (FluidStack stack : fluids) {
                    String name = fluidMaterials.get(
                        stack.getFluid()
                            .getName());
                    if (name != null) present.add(name);
                }
            } else {
                for (ItemStack stack : items) present.addAll(materialNames(stack));
            }
            for (String name : materials) {
                if ("fluid".equals(mode)) {
                    FluidStack stack = materialFluids.get(name);
                    if (stack != null && present.add(name)) fluids.add(stack.copy());
                } else if (!present.contains(name)) {
                    List<ItemStack> registered = OreDictionary.getOres(mode + name, false);
                    if (!registered.isEmpty() && registered.get(0) != null) {
                        ItemStack stack = registered.get(0)
                            .copy();
                        stack.stackSize = 1;
                        items.add(stack);
                        present.addAll(materialNames(stack));
                    }
                }
            }
            pools.put(mode, new Pool(items.toArray(new ItemStack[0]), fluids.toArray(new FluidStack[0])));
        }
    }

    private static Set<String> materialNames(ItemStack stack) {
        Set<String> names = new LinkedHashSet<>();
        for (int id : OreDictionary.getOreIDs(stack)) {
            String name = OreDictionary.getOreName(id);
            if (name.startsWith("dust") && name.length() > 4) names.add(name.substring(4));
            else if (name.startsWith("ingot") && name.length() > 5) names.add(name.substring(5));
        }
        return names;
    }

    private static void indexFluid(String material, FluidStack stack, Map<String, String> fluidMaterials,
        Map<String, FluidStack> materialFluids) {
        if (stack == null) return;
        fluidMaterials.putIfAbsent(
            stack.getFluid()
                .getName(),
            material);
        materialFluids.putIfAbsent(material, stack);
    }

    private static boolean registerPool(ItemStack markerStack, int circuit, Pool pool, int eut) {
        ItemStack circuitStack = GTUtility.getIntegratedCircuit(circuit);
        circuitStack.stackSize = 0;
        List<ItemStack> displayItems = uniqueItems(pool.items);
        List<FluidStack> displayFluids = uniqueFluids(pool.fluids);
        for (int i = 0; i < displayFluids.size(); i++) {
            FluidStack output = displayFluids.get(i)
                .copy();
            output.amount = 1000;
            displayFluids.set(i, output);
        }
        GTRecipeBuilder actual = GTRecipeBuilder.builder()
            .itemInputs(markerStack.copy(), circuitStack.copy())
            .itemOutputs(displayItems.toArray(new ItemStack[0]))
            .fluidOutputs(displayFluids.toArray(new FluidStack[0]))
            .duration(GTNHAdjust.collectorRecipeTicks)
            .eut(eut)
            .recipeCategory(SuperFragmentRecipes.categoryFor(circuit))
            .hidden();
        Collection<GTRecipe> actualRecipes = actual.addTo(SuperFragmentRecipes.MAP);
        if (actualRecipes.isEmpty()) {
            GTNHAdjust.LOG
                .warn("Singularity recipe rejected for {} circuit {}", markerStack.getUnlocalizedName(), circuit);
            return false;
        }
        for (GTRecipe recipe : actualRecipes) recipe.mDuration = GTNHAdjust.collectorRecipeTicks;
        int pageSize = pool.fluids.length == 0 ? SuperFragmentRecipes.ITEM_OUTPUTS : SuperFragmentRecipes.FLUID_OUTPUTS;
        int total = pool.fluids.length == 0 ? displayItems.size() : displayFluids.size();
        for (int start = 0; start < total; start += pageSize) {
            int end = Math.min(total, start + pageSize);
            GTRecipeBuilder builder = GTRecipeBuilder.builder()
                .itemInputs(markerStack.copy(), circuitStack.copy())
                .duration(GTNHAdjust.collectorRecipeTicks)
                .eut(eut)
                .setNEIDesc("完整产物包含该配方的所有页面")
                .recipeCategory(SuperFragmentRecipes.categoryFor(circuit))
                .fake()
                .ignoreCollision();
            if (pool.fluids.length == 0) builder.itemOutputs(
                displayItems.subList(start, end)
                    .toArray(new ItemStack[0]));
            else builder.fluidOutputs(
                displayFluids.subList(start, end)
                    .toArray(new FluidStack[0]));
            for (GTRecipe recipe : builder.addTo(SuperFragmentRecipes.MAP))
                recipe.mDuration = GTNHAdjust.collectorRecipeTicks;
        }
        return true;
    }

    private static ItemStack item(Resource value) {
        String[] name = value.id.split(":", 2);
        Item item = name.length == 2 ? GameRegistry.findItem(name[0], name[1]) : null;
        if (item == null && GTNHAdjust.debug)
            GTNHAdjust.LOG.info("Singularity missing item {} meta {}", value.id, value.meta);
        return item == null ? null : new ItemStack(item, 1, value.meta);
    }

    private static FluidStack fluid(Resource value) {
        FluidStack result = null;
        if ("fluid".equals(value.kind)) result = FluidRegistry.getFluidStack(value.id, 1);
        else if ("materialFluid".equals(value.kind)) {
            Materials material = Materials.get(value.material);
            if (material != null && material != Materials._NULL) {
                switch (value.method) {
                    case "getMolten":
                        result = material.getMolten(1);
                        break;
                    case "getFluid":
                        result = material.getFluid(1);
                        break;
                    case "getGas":
                        result = material.getGas(1);
                        break;
                    default:
                        break;
                }
            }
        } else if ("werkstoffFluid".equals(value.kind)) {
            try {
                Field field = WerkstoffLoader.class.getField(value.material);
                Werkstoff material = (Werkstoff) field.get(null);
                if ("getFluidOrGas".equals(value.method)) result = material.getFluidOrGas(1);
                else if ("getMolten".equals(value.method)) result = material.getMolten(1);
            } catch (ReflectiveOperationException | ClassCastException ignored) {}
        }
        if (result == null && GTNHAdjust.debug)
            GTNHAdjust.LOG.info("Singularity missing fluid {} {} {}", value.id, value.material, value.method);
        return result;
    }

    private static int circuit(String mode) {
        switch (mode) {
            case "ore":
                return 20;
            case "gem":
                return 21;
            case "dust":
                return 22;
            case "ingot":
                return 23;
            case "fluid":
                return 24;
            default:
                throw new IllegalArgumentException("Unknown mode " + mode);
        }
    }

    private static String itemKey(ItemStack stack) {
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        return id + ":" + stack.getItemDamage();
    }

    private static List<ItemStack> uniqueItems(ItemStack[] stacks) {
        Map<String, ItemStack> result = new LinkedHashMap<>();
        for (ItemStack stack : stacks) result.put(itemKey(stack), stack);
        return new ArrayList<>(result.values());
    }

    private static List<FluidStack> uniqueFluids(FluidStack[] stacks) {
        Map<String, FluidStack> result = new LinkedHashMap<>();
        for (FluidStack stack : stacks) result.put(
            stack.getFluid()
                .getName(),
            stack);
        return new ArrayList<>(result.values());
    }

    private static void exportRegistry() {
        try {
            Path path = Launch.minecraftHome.toPath()
                .resolve("config/gtnhadjust/singularity-registries.json");
            Files.createDirectories(path.getParent());
            Set<String> items = new TreeSet<>();
            for (Object value : Item.itemRegistry) {
                if (!(value instanceof Item)) continue;
                GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor((Item) value);
                if (id != null) items.add(id.modId + ":" + id.name);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", items);
            Map<String, List<String>> solidMaterials = new java.util.TreeMap<>();
            for (String name : OreDictionary.getOreNames()) {
                if (!(name.startsWith("dust") || name.startsWith("ingot")
                    || name.startsWith("gem")
                    || name.startsWith("rawOre")
                    || name.startsWith("ore"))) continue;
                List<String> stacks = new ArrayList<>();
                for (ItemStack stack : OreDictionary.getOres(name, false)) {
                    if (stack != null && stack.getItem() != null) stacks.add(itemKey(stack));
                }
                solidMaterials.put(name, stacks);
            }
            data.put("solidMaterials", solidMaterials);
            data.put(
                "fluids",
                new TreeSet<>(
                    FluidRegistry.getRegisteredFluids()
                        .keySet()));
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting()
                    .create()
                    .toJson(data, writer);
            }
            GTNHAdjust.LOG.info("Exported registry diagnostics to {}", path);
        } catch (Exception error) {
            GTNHAdjust.LOG.warn("Cannot export registry diagnostics", error);
        }
    }

    private static final class Data {

        Map<String, Map<String, Resource[]>> dimensions = Collections.emptyMap();
        Map<String, Integer> tiers = Collections.emptyMap();
    }

    private static final class Resource {

        String kind, id, material, method;
        int meta;
    }

    private static final class Pool {

        final ItemStack[] items;
        final FluidStack[] fluids;

        Pool(ItemStack[] items, FluidStack[] fluids) {
            this.items = items;
            this.fluids = fluids;
        }
    }
}
