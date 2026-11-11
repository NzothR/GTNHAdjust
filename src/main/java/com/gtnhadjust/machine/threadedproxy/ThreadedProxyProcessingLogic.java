package com.gtnhadjust.machine.threadedproxy;

import java.math.BigInteger;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

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

    private GTRecipe selected;
    private int overclockDuration;
    private RecipeThreadOutputs pendingOutputs;

    /** Chooses each thread's OC first; a 1-tick recipe receives no additional overclocks. */
    void configure(GTRecipe recipe, long budget, long voltage, int maxOverclocks) {
        clear();
        selected = recipe;
        pendingOutputs = null;
        int untilOneTick = 31 - Integer.numberOfLeadingZeros(recipe.mDuration);
        OverclockCalculator plan = new OverclockCalculator().setRecipeEUt(recipe.mEUt)
            .setDuration(recipe.mDuration)
            .setEUt(Math.min(budget, voltage))
            .setAmperage(1)
            .setAmperageOC(false)
            .setMaxOverclocks(Math.min(maxOverclocks, untilOneTick))
            .calculate();
        overclockDuration = plan.getDuration();
        long unitPower = plan.getConsumption();
        double multiplier = recipe.mEUt == 0 ? 1 : (double) unitPower / recipe.mEUt;
        setEuModifier(multiplier);
        setAvailableVoltage(voltage);
        setAvailableAmperage(1);
        setAmperageOC(false);
        setMaxParallel(unitPower == 0 ? Integer.MAX_VALUE : (int) Math.min(Integer.MAX_VALUE, budget / unitPower));
        prepareRecipe(budget);
    }

    @Override
    @Nonnull
    protected Stream<GTRecipe> findRecipeMatches(RecipeMap<?> map) {
        return Stream.of(selected);
    }

    @Override
    @Nonnull
    protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
        // OC has already been budgeted. GT's batch helper runs after this fixed duration/power calculation.
        return super.createOverclockCalculator(recipe).setDuration(overclockDuration)
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
        if (applied.wasSuccessful()) pendingOutputs = outputs(recipe, helper.getCurrentParallel());
        return applied;
    }

    RecipeThreadOutputs pendingOutputs() {
        return pendingOutputs;
    }

    /** Stores large yields compactly, avoiding an array entry for each int-sized slice of a 2.1G-parallel yield. */
    private static RecipeThreadOutputs outputs(GTRecipe recipe, int parallels) {
        NBTTagCompound saved = new NBTTagCompound();
        NBTTagList items = new NBTTagList(), fluids = new NBTTagList();
        for (int i = 0; i < recipe.mOutputs.length; i++) {
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
        for (int i = 0; i < recipe.mFluidOutputs.length; i++) {
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
