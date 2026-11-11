package com.gtnhadjust.machine.threadedproxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.me.CircuitPatternCodec;
import com.xyp.gtneverreset.common.me.CircuitRecipeInputs;
import com.xyp.gtneverreset.common.me.MoldDataManager;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipe.RecipeItemInput;
import gregtech.api.util.GTUtility;

/** Resolves a definition independently of the real inventory, preferring all known non-consumed tools. */
final class ProxyPatternRecipe {

    final GTRecipe recipe;
    final ItemStack[] consumedItems;
    final FluidStack[] consumedFluids;
    final ItemStack[] virtualTools;
    final boolean exact;

    private ProxyPatternRecipe(GTRecipe recipe, ItemStack[] items, FluidStack[] fluids, ItemStack[] tools,
        boolean exact) {
        this.recipe = recipe;
        consumedItems = items;
        consumedFluids = fluids;
        virtualTools = tools;
        this.exact = exact;
    }

    /** Scans only when the controller or encoded patterns change; partial output lists are accepted. */
    static ProxyPatternRecipe find(RecipeMap<?> map, ICraftingPatternDetails details) {
        if (details == null || details.isCraftable() || details.isInputOnly()) return null;
        int circuit = CircuitPatternCodec.circuit(details);
        ItemStack[] molds = CircuitPatternCodec.molds(details);
        if (circuit == -2 || CircuitPatternCodec.storedMolds(details.getPattern()) == null) return null;
        List<ItemStack> items = new ArrayList<>();
        List<FluidStack> fluids = new ArrayList<>();
        for (IAEStack<?> input : details.getAEInputs()) {
            if (input instanceof IAEItemStack item) items.add(item.getItemStack());
            else if (input instanceof IAEFluidStack fluid) fluids.add(fluid.getFluidStack());
        }
        if (circuit >= 0 && items.stream()
            .noneMatch(GTUtility::isAnyIntegratedCircuit)) items.add(GTUtility.getIntegratedCircuit(circuit));
        for (ItemStack mold : molds) if (items.stream()
            .noneMatch(item -> GTUtility.areStacksEqual(item, mold, false))) items.add(mold.copy());
        ItemStack[] inputs = items.toArray(new ItemStack[0]);
        FluidStack[] liquid = fluids.toArray(new FluidStack[0]);
        Iterable<GTRecipe> candidates = map.getAllRecipes();
        if (map.getBackend()
            .doesOverwriteFindRecipe()) {
            List<GTRecipe> dynamic = new ArrayList<>(map.getAllRecipes());
            map.findRecipeQuery()
                .items(inputs)
                .fluids(liquid)
                .findAll()
                .forEach(dynamic::add);
            candidates = dynamic;
        }
        ProxyPatternRecipe relaxed = null;
        for (GTRecipe recipe : candidates) {
            ProxyPatternRecipe match = match(recipe, inputs, liquid, details);
            if (match == null) continue;
            if (match.exact) return match;
            if (relaxed == null) relaxed = match;
        }
        return relaxed;
    }

    /** Uses GT's input consumer on copies, so ore alternatives, NBT and whole encoded batches retain GT semantics. */
    private static ProxyPatternRecipe match(GTRecipe recipe, ItemStack[] inputs, FluidStack[] fluids,
        ICraftingPatternDetails details) {
        if (!recipe.mEnabled || recipe.mFakeRecipe || recipe.mEUt < 0 || recipe.mDuration < 1) return null;
        List<ItemStack> consumed = new ArrayList<>();
        List<ItemStack> knownTools = new ArrayList<>();
        RecipeItemInput[] required = recipe.getCachedCombinedItemInputs();
        for (ItemStack item : inputs) {
            boolean tool = Arrays.stream(required)
                .anyMatch(input -> input.inputAmount == 0 && matches(input, item));
            if (tool) knownTools.add(item);
            else {
                // Zero-count metadata cannot turn into a consumable. Unmatched positive inputs must be consumed below.
                if (item.stackSize <= 0) return null;
                consumed.add(item);
            }
        }
        List<ItemStack> virtual = new ArrayList<>();
        List<ItemStack> simulation = new ArrayList<>(consumed);
        boolean exact = true;
        for (RecipeItemInput input : required) if (input.inputAmount == 0) {
            ItemStack supplied = knownTools.stream()
                .filter(tool -> matches(input, tool))
                .findFirst()
                .orElse(null);
            if (supplied == null) exact = false;
            ItemStack representative = supplied == null ? input.unifiedStack : supplied;
            if (representative == null) return null;
            ItemStack tool = representative.copy();
            tool.stackSize = 1;
            simulation.add(tool);
            if (isVirtual(tool)) virtual.add(tool);
        }
        List<FluidStack> consumedLiquid = new ArrayList<>();
        List<FluidStack> simulationLiquid = new ArrayList<>();
        for (FluidStack fluid : fluids) {
            boolean tool = Arrays.stream(recipe.mFluidInputs)
                .anyMatch(input -> input != null && input.amount == 0 && input.isFluidEqual(fluid));
            if (!tool && fluid.amount > 0) consumedLiquid.add(fluid);
        }
        simulationLiquid.addAll(consumedLiquid);
        for (FluidStack input : recipe.mFluidInputs) if (input != null && input.amount == 0) {
            if (Arrays.stream(fluids)
                .noneMatch(input::isFluidEqual)) exact = false;
            FluidStack tool = input.copy();
            tool.amount = 1;
            simulationLiquid.add(tool);
        }
        ItemStack[] consumedItems = consumed.toArray(new ItemStack[0]);
        FluidStack[] consumedFluids = consumedLiquid.toArray(new FluidStack[0]);
        long batches = CircuitRecipeInputs.quantityBatches(recipe, consumedItems, consumedFluids);
        if (batches <= 0 || batches > Integer.MAX_VALUE || !outputsMatch(recipe, details, batches)) return null;
        ItemStack[] copiedItems = simulation.stream()
            .map(ItemStack::copy)
            .toArray(ItemStack[]::new);
        FluidStack[] copiedFluids = simulationLiquid.stream()
            .map(FluidStack::copy)
            .toArray(FluidStack[]::new);
        // Recipe identity must not sample probabilistic consumption. Execution still uses the original recipe.
        GTRecipe check = recipe.copy();
        check.mInputChances = null;
        check.mFluidInputChances = null;
        check.mAltFluidInputs = recipe.mAltFluidInputs;
        if (!check.isRecipeInputEqual(true, false, (int) batches, copiedFluids, copiedItems)) return null;
        for (int i = 0; i < consumed.size(); i++) if (copiedItems[i].stackSize > 0) return null;
        for (int i = 0; i < consumedLiquid.size(); i++) if (copiedFluids[i].amount > 0) return null;
        return new ProxyPatternRecipe(recipe, consumedItems, consumedFluids, virtual.toArray(new ItemStack[0]), exact);
    }

    /** Virtual support is the GTNR catalog, and only zero-quantity recipe requirements reach this method. */
    private static boolean isVirtual(ItemStack tool) {
        if (GTUtility.isAnyIntegratedCircuit(tool)) return true;
        for (ItemStack mold : MoldDataManager.getMolds()) if (GTUtility.areStacksEqual(mold, tool, false)) return true;
        return false;
    }

    private static boolean matches(RecipeItemInput input, ItemStack item) {
        return input.matchesRecipe(GTOreDictUnificator.getAssociation(item), item);
    }

    /** Every advertised output must exist, but recipes may produce additional outputs. */
    private static boolean outputsMatch(GTRecipe recipe, ICraftingPatternDetails pattern, long batches) {
        boolean any = false;
        for (IAEStack<?> output : pattern.getCondensedAEOutputs()) {
            if (output == null || output.getStackSize() <= 0) continue;
            any = true;
            long amount = 0;
            if (output instanceof IAEItemStack item) {
                for (ItemStack product : recipe.mOutputs)
                    if (product != null && GTUtility.areStacksEqual(product, item.getItemStack(), false))
                        amount += product.stackSize;
            } else if (output instanceof IAEFluidStack fluid) {
                for (FluidStack product : recipe.mFluidOutputs)
                    if (product != null && product.isFluidEqual(fluid.getFluidStack())) amount += product.amount;
            }
            if (amount == 0 || output.getStackSize() % batches != 0 || amount != output.getStackSize() / batches)
                return false;
        }
        return any;
    }
}
