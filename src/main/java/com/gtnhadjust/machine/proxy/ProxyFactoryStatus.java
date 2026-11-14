package com.gtnhadjust.machine.proxy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.network.NetworkUtils;

import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.util.GTUtility;

/** Detached, client-localized status data. Equal products from different threads deliberately remain separate. */
final class ProxyFactoryStatus {

    private static final CheckRecipeResult SOURCE_UNFORMED = gregtech.api.recipe.check.SimpleCheckRecipeResult
        .ofFailure("gtnhadjust_proxy_unformed");

    private ProxyFactoryStatus() {}

    static final class Output {

        final int thread, progress, duration;
        final ItemStack item;
        final FluidStack fluid;
        final BigInteger amount;

        Output(int thread, int progress, int duration, ItemStack item, FluidStack fluid, BigInteger amount) {
            this.thread = thread;
            this.progress = progress;
            this.duration = duration;
            this.item = item;
            this.fluid = fluid;
            this.amount = amount;
        }

        String name() {
            return item != null ? item.getDisplayName() : fluid.getLocalizedName();
        }

        String amountAndProgress() {
            boolean liquid = fluid != null;
            String count = StatCollector.translateToLocalFormatted(
                liquid ? "GT5U.gui.text.fluid_amount_display" : "GT5U.gui.text.item_amount_display",
                "",
                shortened(amount));
            String rate = amount.bitLength() < 63 ? GTUtility.appendRate(liquid, amount.longValue(), true, duration)
                : "(" + shortened(
                    amount.multiply(BigInteger.valueOf(20))
                        .divide(BigInteger.valueOf(duration)))
                    + (liquid ? " L/s)" : "/s)");
            return count + rate + String.format(Locale.ROOT, "  %.1fs/%.1fs", progress / 20D, duration / 20D);
        }

        boolean sameShape(Output other) {
            return thread == other.thread && ItemStack.areItemStacksEqual(item, other.item)
                && sameFluid(fluid, other.fluid);
        }

        boolean same(Output other) {
            return sameShape(other) && progress == other.progress
                && duration == other.duration
                && amount.equals(other.amount);
        }

        static void write(PacketBuffer buffer, Output row) {
            buffer.writeInt(row.thread);
            buffer.writeInt(row.progress);
            buffer.writeInt(row.duration);
            NetworkUtils.writeItemStack(buffer, row.item);
            NetworkUtils.writeFluidStack(buffer, row.fluid);
            NetworkUtils.writeStringSafe(buffer, row.amount.toString());
        }

        static Output read(PacketBuffer buffer) {
            return new Output(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                NetworkUtils.readItemStack(buffer),
                NetworkUtils.readFluidStack(buffer),
                new BigInteger(NetworkUtils.readStringSafe(buffer)));
        }
    }

    /** This is a failed launch, not the state of a previously committed running thread. */
    static final class Issue {

        final int dimension, x, y, z, slot;
        final ItemStack product, source;
        final FluidStack fluidProduct;
        final CheckRecipeResult result;

        Issue(int dimension, int x, int y, int z, int slot, ItemStack product, FluidStack fluidProduct,
            ItemStack source, CheckRecipeResult result) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.slot = slot;
            this.product = product;
            this.fluidProduct = fluidProduct;
            this.source = source;
            this.result = result;
        }

        String text() {
            String output = product != null ? product.getDisplayName()
                : fluidProduct != null ? fluidProduct.getLocalizedName()
                    : StatCollector.translateToLocal("gtnhadjust.proxy.status.unknown_output");
            String reason = result.equals(SOURCE_UNFORMED)
                ? StatCollector.translateToLocal("gtnhadjust.proxy.status.unformed")
                : result.getDisplayString();
            return source == null
                ? StatCollector
                    .translateToLocalFormatted("gtnhadjust.proxy.status.pattern_failure", slot + 1, output, reason)
                : StatCollector.translateToLocalFormatted(
                    "gtnhadjust.proxy.status.source_failure",
                    slot + 1,
                    output,
                    source.getDisplayName(),
                    reason);
        }

        boolean same(Issue other) {
            return dimension == other.dimension && x == other.x
                && y == other.y
                && z == other.z
                && slot == other.slot
                && ItemStack.areItemStacksEqual(product, other.product)
                && sameFluid(fluidProduct, other.fluidProduct)
                && ItemStack.areItemStacksEqual(source, other.source)
                && Objects.equals(result, other.result);
        }

        static void write(PacketBuffer buffer, Issue issue) {
            buffer.writeInt(issue.dimension);
            buffer.writeInt(issue.x);
            buffer.writeInt(issue.y);
            buffer.writeInt(issue.z);
            buffer.writeInt(issue.slot);
            NetworkUtils.writeItemStack(buffer, issue.product);
            NetworkUtils.writeFluidStack(buffer, issue.fluidProduct);
            NetworkUtils.writeItemStack(buffer, issue.source);
            NetworkUtils.writeStringSafe(buffer, issue.result.getID());
            issue.result.encode(buffer);
        }

        static Issue read(PacketBuffer buffer) {
            int dimension = buffer.readInt(), x = buffer.readInt(), y = buffer.readInt(), z = buffer.readInt(),
                slot = buffer.readInt();
            ItemStack product = NetworkUtils.readItemStack(buffer);
            FluidStack fluid = NetworkUtils.readFluidStack(buffer);
            ItemStack source = NetworkUtils.readItemStack(buffer);
            CheckRecipeResult result = CheckRecipeResultRegistry
                .getSampleFromRegistry(NetworkUtils.readStringSafe(buffer))
                .newInstance();
            result.decode(buffer);
            return new Issue(dimension, x, y, z, slot, product, fluid, source, result);
        }
    }

    static List<Output> outputs(ProxyFactory machine) {
        List<Output> rows = new ArrayList<>();
        for (ProxyThreadTask task : machine.getProxyThreadTasks()) {
            ProxyThreadOutputs outputs = task.getOutputs();
            for (int i = 0; i < outputs.items.length; i++) {
                if (outputs.itemAmounts[i].signum() > 0) rows.add(
                    new Output(
                        task.getThread(),
                        task.getProgress(),
                        task.getDuration(),
                        outputs.items[i].copy(),
                        null,
                        outputs.itemAmounts[i]));
            }
            for (int i = 0; i < outputs.fluids.length; i++) {
                if (outputs.fluidAmounts[i].signum() > 0) rows.add(
                    new Output(
                        task.getThread(),
                        task.getProgress(),
                        task.getDuration(),
                        null,
                        outputs.fluids[i].copy(),
                        outputs.fluidAmounts[i]));
            }
        }
        return rows;
    }

    private static boolean sameFluid(FluidStack left, FluidStack right) {
        return left == null ? right == null : right != null && left.isFluidEqual(right);
    }

    private static String shortened(BigInteger amount) {
        return amount.bitLength() < 63 ? GTUtility.formatShortenedLong(amount.longValue())
            : new BigDecimal(amount).round(new MathContext(3))
                .toEngineeringString();
    }
}
