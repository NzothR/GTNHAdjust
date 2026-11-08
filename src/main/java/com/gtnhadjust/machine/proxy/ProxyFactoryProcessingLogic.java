package com.gtnhadjust.machine.proxy;

import java.util.Arrays;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.api.me.RecipeMapPatternInventory;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.OverclockCalculator;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

public final class ProxyFactoryProcessingLogic extends RecipeThreadProcessingLogic {

    private final ProxyFactory controller;
    private ICraftingPatternDetails pattern;
    private RecipeMap<?> patternRecipeMap;
    private IDualInputInventoryWithPattern traceInventory;
    private String tracePattern;

    public ProxyFactoryProcessingLogic(ProxyFactory controller) {
        this.controller = controller;
    }

    @Override
    public boolean tryCachePossibleRecipesFromPattern(IDualInputInventoryWithPattern inventory) {
        resetPatternInventory();
        traceInventory = null;
        tracePattern = null;
        pattern = null;
        patternRecipeMap = null;
        if (inventory instanceof RecipeMapPatternInventory mapped && mapped.getRecipeMapName() != null) {
            RecipeMap<?> requested = PatternRecipeMap.resolve(mapped.getRecipeMapName());
            if (requested == null || !controller.isRecipeMapUnlocked(requested)) return false;
            patternRecipeMap = requested;
        }
        pattern = inventory instanceof SuperMTEHatchCraftingInputME.PatternSlot<?>slot ? slot.getPatternDetails()
            : inventory instanceof MTEHatchCraftingInputME.PatternSlot<?>slot ? slot.getPatternDetails() : null;
        traceInventory = inventory;
        tracePattern = pattern == null ? "none" : Arrays.toString(pattern.getCondensedAEOutputs());
        // Match current accumulated inputs, including virtual tools and fractional AE deliveries.
        // The native input-only cache cannot distinguish recipes with identical inputs in different maps.
        return true;
    }

    @Override
    @Nonnull
    protected CheckRecipeResult processRecipe() {
        try {
            return super.processRecipe();
        } finally {
            pattern = null;
            patternRecipeMap = null;
            resetPatternInventory();
            controller.setProcessingRecipeMap(null);
        }
    }

    @Override
    @Nonnull
    protected Stream<GTRecipe> findRecipeMatches(@Nullable RecipeMap<?> selected) {
        RecipeMap<?> target = patternRecipeMap == null ? selected : patternRecipeMap;
        if (ProxyFactoryEssentia.requiresEssentiaCell(target) && !controller.hasCreativeEssentiaCell())
            return Stream.empty();
        ItemStack[] lookupItems = ProxyFactoryEssentia.requiresEssentiaCell(target) && inputItems != null
            ? Arrays.stream(inputItems)
                .filter(stack -> !ProxyFactoryEssentia.isAspectDisplay(stack))
                .toArray(ItemStack[]::new)
            : inputItems;
        if (target == RecipeMaps.furnaceRecipes) {
            if (lookupItems == null) return Stream.empty();
            return Arrays.stream(lookupItems)
                .filter(item -> item != null && item.stackSize > 0)
                .flatMap(
                    item -> target.findRecipeQuery()
                        .items(item)
                        .fluids(inputFluids)
                        .findAll())
                .peek(recipe -> controller.setProcessingRecipeMap(target))
                .filter(this::matchesPatternOutputs);
        }
        return (target == null ? Stream.<RecipeMap<?>>empty() : Stream.of(target)).flatMap(
            map -> map.findRecipeQuery()
                .items(lookupItems)
                .fluids(inputFluids)
                .specialSlot(specialSlotItem)
                .findAll()
                .peek(recipe -> controller.setProcessingRecipeMap(map)))
            .filter(this::matchesPatternOutputs);
    }

    @Override
    @Nonnull
    protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
        RecipeMap<?> map = controller.getRecipeMap();
        double factor = ProxyRecipeDuration
            .factor(map == null ? null : map.unlocalizedName, controller.getRecipeGlassTier());
        return super.createOverclockCalculator(recipe).setDurationModifier(speedBoost * factor);
    }

    @Override
    @Nonnull
    protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
        // Also guard the native single-recipe-lock path, which bypasses findRecipeMatches.
        if (isRecipeLocked && recipeLockableMachine != null && recipeLockableMachine.getSingleRecipeCheck() != null) {
            RecipeMap<?> requested = patternRecipeMap == null ? controller.getRecipeMap() : patternRecipeMap;
            if (requested != recipeLockableMachine.getSingleRecipeCheck()
                .getRecipeMap()) {
                return CheckRecipeResultRegistry.NO_RECIPE;
            }
            controller.setProcessingRecipeMap(
                recipeLockableMachine.getSingleRecipeCheck()
                    .getRecipeMap());
        }
        RecipeMap<?> current = patternRecipeMap == null ? controller.getRecipeMap() : patternRecipeMap;
        if (ProxyFactoryEssentia.requiresEssentiaCell(current) && !controller.hasCreativeEssentiaCell())
            return CheckRecipeResultRegistry.NO_RECIPE;
        return matchesPatternOutputs(recipe) ? super.validateRecipe(recipe) : CheckRecipeResultRegistry.NO_RECIPE;
    }

    /** Describes the exact copies that will be committed, before their source amounts change. */
    String describePlannedInputs() {
        StringBuilder result = new StringBuilder();
        result.append("inventory=")
            .append(
                traceInventory == null ? "none"
                    : traceInventory.getClass()
                        .getName())
            .append("@")
            .append(traceInventory == null ? "none" : Integer.toHexString(System.identityHashCode(traceInventory)))
            .append(" patternOutputs=")
            .append(tracePattern)
            .append(" items=[");
        if (inputItems != null) for (ItemStack item : inputItems) {
            if (item != null && getPlannedConsumption(item) > 0) result.append(item)
                .append(" consume=")
                .append(getPlannedConsumption(item))
                .append(';');
        }
        result.append("] fluids=[");
        if (inputFluids != null) for (FluidStack fluid : inputFluids) {
            if (fluid != null && getPlannedConsumption(fluid) > 0) result.append(fluid)
                .append(" consume=")
                .append(getPlannedConsumption(fluid))
                .append(';');
        }
        return result.append(']')
            .toString();
    }

    private boolean matchesPatternOutputs(GTRecipe recipe) {
        if (pattern == null) return true;
        IAEStack<?>[] outputs = pattern.getAEOutputs();
        if (outputs == null || outputs.length == 0) return false;
        for (IAEStack<?> output : outputs) {
            if (output == null || output.getStackSize() <= 0) continue;
            boolean matched = false;
            if (output instanceof IAEItemStack item && recipe.mOutputs != null) {
                ItemStack expected = item.getItemStack();
                for (ItemStack actual : recipe.mOutputs) {
                    if (actual != null && actual.isItemEqual(expected)
                        && ItemStack.areItemStackTagsEqual(actual, expected)) {
                        matched = true;
                        break;
                    }
                }
            } else if (output instanceof IAEFluidStack fluid && recipe.mFluidOutputs != null) {
                for (FluidStack actual : recipe.mFluidOutputs) {
                    if (actual != null && actual.isFluidEqual(fluid.getFluidStack())) {
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) return false;
        }
        return true;
    }
}
