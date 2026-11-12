package com.gtnhadjust.machine.threadedproxy;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.modularui.common.internal.network.NetworkUtils;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadTask;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTUtility;

/** Saved launch metadata and client-localized rows; display order is supplied by the physical pattern slot list. */
final class ThreadedProxyDisplay {

    private ThreadedProxyDisplay() {}

    static final class RunInfo {

        final ItemStack item;
        final FluidStack fluid;
        final String amount;
        final boolean estimated;
        final int parallel, overclocks;

        private RunInfo(ItemStack item, FluidStack fluid, String amount, boolean estimated, int parallel,
            int overclocks) {
            this.item = item;
            this.fluid = fluid;
            this.amount = amount;
            this.estimated = estimated;
            this.parallel = parallel;
            this.overclocks = overclocks;
        }

        /**
         * Show only the first encoded product, including all same-type nominal recipe outputs and their chance flag.
         */
        static RunInfo create(ProxyPatternRecipe match, RecipeThreadTask task, int parallel, int overclocks) {
            ItemStack item = match.displayOutput instanceof IAEItemStack stack ? stack.getItemStack() : null;
            FluidStack fluid = match.displayOutput instanceof IAEFluidStack stack ? stack.getFluidStack() : null;
            long amount = 0;
            boolean estimated = false;
            if (item != null) {
                item.stackSize = 1;
                for (int i = 0; i < match.recipe.mOutputs.length; i++) {
                    ItemStack product = match.recipe.mOutputs[i];
                    int chance = match.recipe.getOutputChance(i);
                    if (product == null || chance <= 0 || !GTUtility.areStacksEqual(product, item, false)) continue;
                    amount += product.stackSize * ((chance + 9999L) / 10000);
                    estimated |= chance % 10000 != 0;
                }
            } else {
                fluid.amount = 1;
                for (int i = 0; i < match.recipe.mFluidOutputs.length; i++) {
                    FluidStack product = match.recipe.mFluidOutputs[i];
                    int chance = match.recipe.getFluidOutputChance(i);
                    if (product == null || chance <= 0 || !product.isFluidEqual(fluid)) continue;
                    amount += product.amount * ((chance + 9999L) / 10000);
                    estimated |= chance % 10000 != 0;
                }
            }
            return new RunInfo(
                item,
                fluid,
                BigInteger.valueOf(amount)
                    .multiply(BigInteger.valueOf(task.getParallels()))
                    .toString(),
                estimated,
                parallel,
                overclocks);
        }

        /**
         * Older tasks lack launch metadata. Infer it only when power, total operations and GT batch duration agree
         * uniquely.
         */
        static RunInfo recover(ProxyPatternRecipe match, RecipeThreadTask task) {
            int parallel = -1, overclocks = -1;
            if (match.recipe.mEUt > 0)
                for (int oc = 0; oc <= 31 - Integer.numberOfLeadingZeros(match.recipe.mDuration); oc++) {
                    ThreadedProxyAllocation.Plan plan = ThreadedProxyAllocation
                        .plan(match.recipe.mEUt, match.recipe.mDuration, Long.MAX_VALUE, oc);
                    if (plan.overclocks != oc || plan.eut <= 0 || task.getEUt() % plan.eut != 0) continue;
                    long concurrent = task.getEUt() / plan.eut;
                    if (concurrent < 1 || concurrent > task.getParallels()) continue;
                    float multiplier = 1f + (float) (task.getParallels() - concurrent) / concurrent;
                    if ((int) (plan.duration * (double) multiplier) != task.getDuration()) continue;
                    if (overclocks >= 0) return create(match, task, -1, -1);
                    parallel = (int) concurrent;
                    overclocks = oc;
                }
            return create(match, task, parallel, overclocks);
        }

        NBTTagCompound save() {
            NBTTagCompound tag = new NBTTagCompound();
            if (item != null) GTUtility.saveItem(tag, "item", item);
            if (fluid != null) tag.setTag("fluid", fluid.writeToNBT(new NBTTagCompound()));
            tag.setString("amount", amount);
            tag.setBoolean("estimated", estimated);
            tag.setInteger("parallel", parallel);
            tag.setInteger("overclocks", overclocks);
            return tag;
        }

        static RunInfo load(NBTTagCompound tag) {
            return new RunInfo(
                GTUtility.loadItem(tag, "item"),
                tag.hasKey("fluid") ? FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("fluid")) : null,
                tag.getString("amount"),
                tag.getBoolean("estimated"),
                tag.getInteger("parallel"),
                tag.getInteger("overclocks"));
        }

        private boolean same(RunInfo other) {
            return other != null && ItemStack.areItemStacksEqual(item, other.item)
                && (fluid == null ? other.fluid == null : other.fluid != null && fluid.isFluidEqual(other.fluid))
                && amount.equals(other.amount)
                && estimated == other.estimated
                && parallel == other.parallel
                && overclocks == other.overclocks;
        }
    }

    /** A typed row keeps product names and native recipe failure messages localized on the viewing client. */
    static final class Row {

        final int index;
        final String reason;
        final CheckRecipeResult result;
        final RunInfo info;
        final int progress, duration;
        final long eut;

        Row(int index, String reason, CheckRecipeResult result, RunInfo info, int progress, int duration, long eut) {
            this.index = index;
            this.reason = reason;
            this.result = result;
            this.info = info;
            this.progress = progress;
            this.duration = duration;
            this.eut = eut;
        }

        static Row idle(int index, String reason, CheckRecipeResult result) {
            return new Row(index, reason, result, null, 0, 0, 0);
        }

        static Row running(int index, RecipeThreadTask task, RunInfo info, boolean paused) {
            return new Row(
                index,
                task.isFinished() ? "output_blocked" : paused ? "paused" : "",
                null,
                info,
                task.getProgress(),
                task.getDuration(),
                task.getEUt());
        }

        String text() {
            String state = result != null ? result.getDisplayString()
                : reason.isEmpty() ? "" : StatCollector.translateToLocal("gtnhadjust.threaded_proxy.reason." + reason);
            if (duration == 0)
                return StatCollector.translateToLocalFormatted("gtnhadjust.threaded_proxy.idle", index, state);
            String product = info == null ? StatCollector.translateToLocal("gtnhadjust.threaded_proxy.reason.legacy")
                : (info.estimated ? "~" : "") + info.amount
                    + (info.item != null ? "x" + info.item.getDisplayName() : "L " + info.fluid.getLocalizedName());
            String row = String.format(
                Locale.ROOT,
                "%s %.1fs/%.1fs %deu/t %sx %s",
                product,
                progress / 20.0,
                duration / 20.0,
                eut,
                info == null || info.parallel < 0 ? "?" : Integer.toString(info.parallel),
                info == null || info.overclocks < 0 ? "?" : Integer.toString(info.overclocks));
            return state.isEmpty() ? row : row + " (" + state + ")";
        }

        static void write(PacketBuffer buffer, Row row) {
            buffer.writeInt(row.index);
            NetworkUtils.writeStringSafe(buffer, row.reason);
            buffer.writeBoolean(row.result != null);
            if (row.result != null) {
                NetworkUtils.writeStringSafe(buffer, row.result.getID());
                row.result.encode(buffer);
            }
            buffer.writeBoolean(row.info != null);
            if (row.info != null) {
                NetworkUtils.writeItemStack(buffer, row.info.item);
                NetworkUtils.writeFluidStack(buffer, row.info.fluid);
                NetworkUtils.writeStringSafe(buffer, row.info.amount);
                buffer.writeBoolean(row.info.estimated);
                buffer.writeInt(row.info.parallel);
                buffer.writeInt(row.info.overclocks);
            }
            buffer.writeInt(row.progress);
            buffer.writeInt(row.duration);
            buffer.writeLong(row.eut);
        }

        static Row read(PacketBuffer buffer) {
            int index = buffer.readInt();
            String reason = NetworkUtils.readStringSafe(buffer);
            CheckRecipeResult result = null;
            if (buffer.readBoolean()) {
                result = CheckRecipeResultRegistry.getSampleFromRegistry(NetworkUtils.readStringSafe(buffer))
                    .newInstance();
                result.decode(buffer);
            }
            RunInfo info = buffer.readBoolean()
                ? new RunInfo(
                    NetworkUtils.readItemStack(buffer),
                    NetworkUtils.readFluidStack(buffer),
                    NetworkUtils.readStringSafe(buffer),
                    buffer.readBoolean(),
                    buffer.readInt(),
                    buffer.readInt())
                : null;
            return new Row(index, reason, result, info, buffer.readInt(), buffer.readInt(), buffer.readLong());
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Row row && index == row.index
                && reason.equals(row.reason)
                && Objects.equals(result, row.result)
                && (info == null ? row.info == null : info.same(row.info))
                && progress == row.progress
                && duration == row.duration
                && eut == row.eut;
        }

        @Override
        public int hashCode() {
            return Objects.hash(index, reason, progress, duration, eut);
        }
    }
}
