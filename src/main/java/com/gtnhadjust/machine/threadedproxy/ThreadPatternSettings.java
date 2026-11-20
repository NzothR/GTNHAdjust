package com.gtnhadjust.machine.threadedproxy;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import gregtech.api.util.GTUtility;

/** Extra controller parameters live alongside, rather than inside, the encoded recipe data. */
final class ThreadPatternSettings {

    static final String KEY = "gtnhadjustThread";
    double target = -1;
    long power = -1;
    int parallel = -1, oc = -1;

    static ThreadPatternSettings read(ItemStack stack) {
        ThreadPatternSettings settings = new ThreadPatternSettings();
        if (stack == null || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(KEY, 10))
            return settings;
        NBTTagCompound tag = stack.getTagCompound()
            .getCompoundTag(KEY);
        settings.target = tag.getDouble("target");
        settings.power = tag.getLong("power");
        settings.parallel = tag.getInteger("parallel");
        settings.oc = tag.getInteger("oc");
        return settings;
    }

    void write(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound tag = stack.getTagCompound()
            .getCompoundTag(KEY);
        tag.setDouble("target", target);
        tag.setLong("power", power);
        tag.setInteger("parallel", parallel);
        tag.setInteger("oc", oc);
        stack.getTagCompound()
            .setTag(KEY, tag);
    }

    /** One-shot estimate: prefer parallel up to the global reference, then OC, then excess parallel. */
    void tune(ProxyPatternRecipe match, int parallelReference) {
        if (target <= 0) return;
        double yield = expectedOutput(match);
        if (yield <= 0) return;
        int limit = 31 - Integer.numberOfLeadingZeros(match.recipe.mDuration);
        double best = -1;
        for (int pass = 0; pass < 2; pass++) for (int step = 0; step <= limit; step++) {
            ThreadedProxyAllocation.Plan plan = ThreadedProxyAllocation
                .plan(match.recipe.mEUt, match.recipe.mDuration, Long.MAX_VALUE, step);
            if (plan.overclocks != step || plan.eut == Long.MAX_VALUE) break;
            long budgetLimit = power < 0 || plan.eut == 0 ? Integer.MAX_VALUE : power / plan.eut;
            int wanted = (int) Math
                .min(Integer.MAX_VALUE, Math.max(1, Math.ceil(target * plan.duration / (20 * yield))));
            int count = (int) Math
                .min(wanted, Math.min(pass == 0 ? parallelReference : Integer.MAX_VALUE, budgetLimit));
            double rate = count * yield * 20 / plan.duration;
            if (count > 0 && rate > best) {
                best = rate;
                parallel = count;
                oc = step;
            }
            if (count == wanted) {
                parallel = count;
                oc = step;
                return;
            }
        }
        // At the OC ceiling, an over-limit result is still useful as an estimate.
    }

    /** Expected amount of the first encoded output per native recipe operation. */
    static double expectedOutput(ProxyPatternRecipe match) {
        double amount = 0;
        if (match.displayOutput instanceof IAEItemStack item) {
            for (int i = 0; i < match.recipe.mOutputs.length; i++) {
                ItemStack product = match.recipe.mOutputs[i];
                if (product != null && GTUtility.areStacksEqual(product, item.getItemStack(), false))
                    amount += product.stackSize * (match.recipe.getOutputChance(i) / 10000.0);
            }
        } else if (match.displayOutput instanceof IAEFluidStack fluid) {
            for (int i = 0; i < match.recipe.mFluidOutputs.length; i++) {
                FluidStack product = match.recipe.mFluidOutputs[i];
                if (product != null && product.isFluidEqual(fluid.getFluidStack()))
                    amount += product.amount * (match.recipe.getFluidOutputChance(i) / 10000.0);
            }
        }
        return amount;
    }
}
