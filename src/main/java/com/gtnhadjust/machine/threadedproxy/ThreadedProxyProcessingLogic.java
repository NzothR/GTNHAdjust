package com.gtnhadjust.machine.threadedproxy;

import java.math.BigInteger;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import com.gtnhadjust.machine.threadedproxy.ThreadedJointProxy.Definition;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadOutputs;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;

/** Transactional ordinary-input processing for one previously resolved pattern. */
final class ThreadedProxyProcessingLogic extends RecipeThreadProcessingLogic {

    private Definition selected;
    private int overclockDuration;
    private RecipeThreadOutputs pendingOutputs;
    private int concurrentParallel;

    /** Applies this slot's bounded OC and parallel; native batching runs after the launch parameters are fixed. */
    void configure(Definition definition, long budget, long voltage, int parallel, ThreadedProxyAllocation.Plan plan) {
        clear();
        selected = definition;
        pendingOutputs = null;
        concurrentParallel = 0;
        overclockDuration = plan.duration;
        long unitPower = plan.eut;
        int recipePower = definition.execution.mEUt;
        double multiplier = recipePower == 0 ? 1 : (double) unitPower / recipePower;
        setEuModifier(multiplier);
        setAvailableVoltage(voltage);
        setAvailableAmperage(1);
        setAmperageOC(false);
        setMaxParallel(parallel);
        prepareRecipe(budget);
    }

    @Override
    @Nonnull
    protected Stream<GTRecipe> findRecipeMatches(RecipeMap<?> map) {
        return Stream.of(selected.execution);
    }

    @Override
    @Nonnull
    protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
        // OC has already been budgeted. GT's batch helper runs after this fixed duration/power calculation.
        return new OverclockCalculator() {

            @Override
            public OverclockCalculator setCurrentParallel(int parallel) {
                concurrentParallel = parallel;
                return super.setCurrentParallel(parallel);
            }
        }.setRecipeEUt(recipe.mEUt)
            .setEUt(availableVoltage)
            .setAmperage(availableAmperage)
            .setEUtDiscount(euModifier)
            .setDuration(overclockDuration)
            .setAmperageOC(false)
            .setNoOverclock(true);
    }

    @Override
    @Nonnull
    protected ParallelHelper createParallelHelper(@Nonnull GTRecipe recipe) {
        return super.createParallelHelper(recipe).setCustomItemOutputCalculation(parallel -> new ItemStack[0])
            .setCustomFluidOutputCalculation(parallel -> new FluidStack[0]);
    }

    @Override
    @Nonnull
    protected CheckRecipeResult applyRecipe(@Nonnull GTRecipe recipe, @Nonnull ParallelHelper helper,
        @Nonnull OverclockCalculator calculator, @Nonnull CheckRecipeResult result) {
        CheckRecipeResult applied = super.applyRecipe(recipe, helper, calculator, result);
        if (applied.wasSuccessful()) pendingOutputs = outputs(selected, helper.getCurrentParallel());
        return applied;
    }

    RecipeThreadOutputs pendingOutputs() {
        return pendingOutputs;
    }

    /** GT's total operation count also includes batches; this is the simultaneous count before batching. */
    int concurrentParallel() {
        return concurrentParallel;
    }

    /** Drop all prepared references when power trips or a manual controller/pattern refresh invalidates the cache. */
    void discardPreparedRecipe() {
        finishRecipe();
        clear();
        resetPatternInventory();
        selected = null;
        pendingOutputs = null;
        concurrentParallel = 0;
    }

    /** Snapshot the retained yields when the thread starts so later pattern changes cannot affect its delivery. */
    private static RecipeThreadOutputs outputs(Definition definition, int parallels) {
        GTRecipe recipe = definition.execution;
        NBTTagCompound saved = new NBTTagCompound();
        NBTTagList items = new NBTTagList(), fluids = new NBTTagList();
        for (int i = definition.itemOutputs.nextSetBit(0); i >= 0; i = definition.itemOutputs.nextSetBit(i + 1)) {
            ItemStack product = recipe.mOutputs[i];
            if (product == null || product.stackSize <= 0) continue;
            long rolls = outputRolls(recipe.getOutputChance(i), parallels);
            if (rolls == 0) continue;
            NBTTagCompound entry = new NBTTagCompound();
            GTUtility.saveItem(entry, "stack", product);
            entry.setString(
                "amount",
                BigInteger.valueOf(product.stackSize)
                    .multiply(BigInteger.valueOf(rolls))
                    .toString());
            items.appendTag(entry);
        }
        for (int i = definition.fluidOutputs.nextSetBit(0); i >= 0; i = definition.fluidOutputs.nextSetBit(i + 1)) {
            FluidStack product = recipe.mFluidOutputs[i];
            if (product == null || product.amount <= 0) continue;
            long rolls = outputRolls(recipe.getFluidOutputChance(i), parallels);
            if (rolls == 0) continue;
            NBTTagCompound entry = product.writeToNBT(new NBTTagCompound());
            entry.setString(
                "bigAmount",
                BigInteger.valueOf(product.amount)
                    .multiply(BigInteger.valueOf(rolls))
                    .toString());
            fluids.appendTag(entry);
        }
        saved.setTag("items", items);
        saved.setTag("fluids", fluids);
        return RecipeThreadOutputs.load(saved);
    }

    /** Keeps guaranteed multiple outputs in long arithmetic before using GT's fractional-chance sampling. */
    private static long outputRolls(int chance, int parallels) {
        return (long) (chance / 10000) * parallels
            + ParallelHelper.calculateIntegralChancedOutputMultiplier(chance % 10000, parallels);
    }
}
