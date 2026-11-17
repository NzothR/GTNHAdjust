package com.gtnhadjust.machine.proxy;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.api.me.RecipeMapPatternInventory;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.enums.GTValues;
import gregtech.api.objects.ItemData;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

public final class ProxyFactoryProcessingLogic extends RecipeThreadProcessingLogic {

    private final ProxyFactory controller;
    private ICraftingPatternDetails pattern;
    private RecipeMap<?> patternRecipeMap;
    private CheckRecipeResult patternFailure;
    private CheckRecipeResult sourceFailureResult;
    private IDualInputInventoryWithPattern traceInventory;
    private IDualInputInventoryWithPattern activePatternInventory;
    private String tracePattern;
    private int wirelessDuration;
    private double wirelessEUtModifier;
    private int wirelessOverclocks;
    private ProxyThreadOutputs wirelessOutputs;

    public ProxyFactoryProcessingLogic(ProxyFactory controller) {
        this.controller = controller;
    }

    @Override
    public boolean tryCachePossibleRecipesFromPattern(IDualInputInventoryWithPattern inventory) {
        resetPatternInventory();
        traceInventory = inventory;
        activePatternInventory = inventory;
        tracePattern = null;
        pattern = inventory instanceof SuperMTEHatchCraftingInputME.PatternSlot<?>slot ? slot.getPatternDetails()
            : inventory instanceof MTEHatchCraftingInputME.PatternSlot<?>slot ? slot.getPatternDetails() : null;
        patternRecipeMap = null;
        patternFailure = null;
        String requestedName = inventory instanceof RecipeMapPatternInventory mapped ? mapped.getRecipeMapName() : null;
        RecipeMap<?> requested = PatternRecipeMap.resolve(requestedName);
        if (requested == null) requested = ProxyExtremeCrafting.resolve(controller, requestedName, pattern);
        if (requested == null && (requestedName == null || requestedName.isEmpty())) {
            patternFailure = gregtech.api.recipe.check.SimpleCheckRecipeResult
                .ofFailure("gtnhadjust_proxy_missing_map");
            return true;
        }
        if (requested == null || !controller.isRecipeMapUnlocked(requested)) {
            patternFailure = gregtech.api.recipe.check.SimpleCheckRecipeResult
                .ofFailure("gtnhadjust_proxy_missing_locator");
            return true;
        }
        if (ProxyExtremeCrafting.isMap(requested) && !GTNHAdjust.proxyExtremeCrafting) {
            patternFailure = CheckRecipeResultRegistry.NO_RECIPE;
            return true;
        }
        patternRecipeMap = requested;
        if (GTNHAdjust.debug && ProxyExtremeCrafting.isMap(requested)) GTNHAdjust.LOG.info(
            "[proxy-extreme] pattern map={} inferred={} outputs={}",
            requested.unlocalizedName,
            requestedName == null,
            pattern == null ? "none" : Arrays.toString(pattern.getCondensedAEOutputs()));
        tracePattern = pattern == null ? "none" : Arrays.toString(pattern.getCondensedAEOutputs());
        // Match current accumulated inputs, including virtual tools and fractional AE deliveries.
        // The native input-only cache cannot distinguish recipes with identical inputs in different maps.
        return true;
    }

    @Override
    @Nonnull
    protected CheckRecipeResult processRecipe() {
        CheckRecipeResult result = CheckRecipeResultRegistry.NO_RECIPE;
        sourceFailureResult = null;
        try {
            result = patternFailure != null ? patternFailure
                : pattern == null || patternRecipeMap == null ? CheckRecipeResultRegistry.NO_RECIPE
                    : super.processRecipe();
            return result;
        } finally {
            if (activePatternInventory != null) controller.recordPatternResult(
                activePatternInventory,
                pattern,
                patternRecipeMap,
                result,
                result.equals(sourceFailureResult));
            activePatternInventory = null;
            pattern = null;
            patternRecipeMap = null;
            patternFailure = null;
            resetPatternInventory();
            controller.setProcessingRecipeMap(null);
        }
    }

    @Override
    @Nonnull
    protected Stream<GTRecipe> findRecipeMatches(@Nullable RecipeMap<?> selected) {
        RecipeMap<?> target = patternRecipeMap;
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
                .filter(this::matchesPatternOutputs)
                .filter(recipe -> consumesPatternInputs(recipe, target));
        }
        return (target == null ? Stream.<RecipeMap<?>>empty() : Stream.of(target)).flatMap(
            map -> map.findRecipeQuery()
                .items(lookupItems)
                .fluids(inputFluids)
                .specialSlot(specialSlotItem)
                .findAll()
                .peek(recipe -> controller.setProcessingRecipeMap(map)))
            .filter(this::matchesPatternOutputs)
            .filter(recipe -> consumesPatternInputs(recipe, target));
    }

    @Override
    @Nonnull
    protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
        RecipeMap<?> map = controller.getRecipeMap();
        double factor = ProxyRecipeDuration
            .factor(map == null ? null : map.unlocalizedName, controller.getRecipeGlassTier());
        // TST conversion recipes are zero-EU work: voltage must not create free electrical overclocks or undertick
        // parallels.
        if (ProxyExtremeCrafting.isMap(map) && recipe.mEUt == 0)
            return super.createOverclockCalculator(recipe).setDurationModifier(speedBoost * factor)
                .setNoOverclock(true);
        if (!controller.isWirelessMode())
            return super.createOverclockCalculator(recipe).setDurationModifier(speedBoost * factor);
        planWirelessOverclocks(recipe, factor);
        return super.createOverclockCalculator(recipe).setDuration(wirelessDuration)
            .setDurationModifier(1)
            .setEUtDiscount(wirelessEUtModifier)
            .setNoOverclock(true);
    }

    /**
     * Reserve overclock power before choosing parallels, without converting surplus voltage into undertick parallels.
     */
    @Override
    @Nonnull
    protected ParallelHelper createParallelHelper(@Nonnull GTRecipe recipe) {
        ParallelHelper helper = super.createParallelHelper(recipe);
        if (!controller.isWirelessMode()) return helper;
        RecipeMap<?> map = controller.getRecipeMap();
        planWirelessOverclocks(
            recipe,
            ProxyRecipeDuration.factor(map == null ? null : map.unlocalizedName, controller.getRecipeGlassTier()));
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "[proxy-wireless] recipeEUt={} overclocks={} duration={} modifier={} budget={}",
            recipe.mEUt,
            wirelessOverclocks,
            wirelessDuration,
            wirelessEUtModifier,
            controller.availableThreadPower());
        wirelessOutputs = new ProxyThreadOutputs(
            recipe.mOutputs == null ? null
                : Arrays.copyOf(recipe.mOutputs, Math.min(recipe.mOutputs.length, controller.getItemOutputLimit())),
            recipe.mFluidOutputs == null ? null
                : Arrays.copyOf(
                    recipe.mFluidOutputs,
                    Math.min(recipe.mFluidOutputs.length, controller.getFluidOutputLimit())));
        return helper.setEUtModifier(wirelessEUtModifier)
            .setCustomItemOutputCalculation(parallels -> {
                for (int i = 0; i < wirelessOutputs.items.length; i++) {
                    if (recipe.mOutputs[i] != null)
                        wirelessOutputs.itemAmounts[i] = BigInteger.valueOf(recipe.mOutputs[i].stackSize)
                            .multiply(chancedMultiplier(recipe.getOutputChance(i), parallels));
                }
                return GTValues.emptyItemStackArray;
            })
            .setCustomFluidOutputCalculation(parallels -> {
                for (int i = 0; i < wirelessOutputs.fluids.length; i++) {
                    if (recipe.mFluidOutputs[i] != null)
                        wirelessOutputs.fluidAmounts[i] = BigInteger.valueOf(recipe.mFluidOutputs[i].amount)
                            .multiply(chancedMultiplier(recipe.getFluidOutputChance(i), parallels));
                }
                return GTValues.emptyFluidStackArray;
            });
    }

    /** Keep guaranteed chance multiples out of the native int multiplication at MAX parallel. */
    private static BigInteger chancedMultiplier(int chance, int parallels) {
        long guaranteed = (long) (chance / 10000) * parallels;
        return BigInteger
            .valueOf(guaranteed + ParallelHelper.calculateIntegralChancedOutputMultiplier(chance % 10000, parallels));
    }

    /** The compact ledger is committed with the same transaction as the consumed inputs. */
    ProxyThreadOutputs getPreparedOutputs() {
        return controller.isWirelessMode() ? wirelessOutputs
            : new ProxyThreadOutputs(getOutputItems(), getOutputFluids());
    }

    /** Stop at one tick, the configured limit, or the current thread's representable power budget. */
    private void planWirelessOverclocks(GTRecipe recipe, double durationFactor) {
        double time = recipe.mDuration * speedBoost * durationFactor;
        double modifier = euModifier;
        double reduction = controller.getOverclockTimeReduction();
        double increase = controller.getOverclockPowerIncrease();
        int overclocks = 0;
        int limit = ProxyExtremeCrafting.isMap(patternRecipeMap) && recipe.mEUt == 0 ? 0
            : controller.getWirelessMaxOverclocks();
        while (overclocks < limit && time >= 2 && reduction > 1) {
            double nextModifier = modifier * increase;
            if (Math.ceil(recipe.mEUt * nextModifier) > controller.availableThreadPower()) break;
            modifier = nextModifier;
            time /= reduction;
            overclocks++;
        }
        wirelessDuration = (int) Math.max(1, time);
        wirelessEUtModifier = modifier;
        wirelessOverclocks = overclocks;
    }

    @Override
    @Nonnull
    protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
        RecipeMap<?> current = patternRecipeMap;
        if (ProxyFactoryEssentia.requiresEssentiaCell(current) && !controller.hasCreativeEssentiaCell())
            return CheckRecipeResultRegistry.NO_RECIPE;
        if (!matchesPatternOutputs(recipe) || !consumesPatternInputs(recipe, current))
            return CheckRecipeResultRegistry.NO_RECIPE;
        CheckRecipeResult sourceResult = controller.checkLocatorRequirements(current, recipe);
        if (!sourceResult.wasSuccessful()) sourceFailureResult = sourceResult;
        return sourceResult.wasSuccessful() ? super.validateRecipe(recipe) : sourceResult;
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

    private boolean consumesPatternInputs(GTRecipe recipe, RecipeMap<?> map) {
        if (pattern == null) return true;
        if (!com.gtnhadjust.compat.GtnrToolPatterns.supplied(recipe, pattern, inputItems)) return false;
        IAEStack<?>[] inputs = pattern.getAEInputs();
        if (inputs == null || inputs.length == 0) return true;
        GTRecipe.RecipeItemInput[] itemInputs = recipe.getCachedCombinedItemInputs();
        for (IAEStack<?> input : inputs) {
            if (input == null || input.getStackSize() <= 0) continue;
            if (input instanceof IAEItemStack item) {
                ItemStack stack = item.getItemStack();
                if (ProxyFactoryEssentia.requiresEssentiaCell(map) && ProxyFactoryEssentia.isAspectDisplay(stack))
                    continue;
                ItemData association = GTOreDictUnificator.getAssociation(stack);
                boolean matched = false;
                for (GTRecipe.RecipeItemInput candidate : itemInputs) {
                    if (candidate.matchesRecipe(association, stack)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) return false;
            } else if (input instanceof IAEFluidStack fluid && !consumesFluid(recipe, fluid.getFluidStack())) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumesFluid(GTRecipe recipe, FluidStack fluid) {
        if (recipe.mFluidInputs == null || fluid == null) return false;
        for (int i = 0; i < recipe.mFluidInputs.length; i++) {
            FluidStack[] alternatives = recipe.mAltFluidInputs != null && i < recipe.mAltFluidInputs.length
                && recipe.mAltFluidInputs[i] != null ? recipe.mAltFluidInputs[i]
                    : recipe.mFluidInputs[i] == null ? null : new FluidStack[] { recipe.mFluidInputs[i] };
            if (alternatives == null) continue;
            for (FluidStack candidate : alternatives) {
                if (candidate != null && candidate.isFluidEqual(fluid)) return true;
            }
        }
        return false;
    }

    private boolean matchesPatternOutputs(GTRecipe recipe) {
        if (ProxyExtremeCrafting.isMap(patternRecipeMap)) return ProxyExtremeCrafting.matches(recipe, pattern);
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
