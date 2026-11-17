// Derived from GT-Never-Reset 0.1.34 RecipeThreadOutputs.
package com.gtnhadjust.machine.proxy;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import com.gtnhadjust.GTNHAdjust;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.FluidEjectionHelper;
import gregtech.api.util.GTUtility;
import gregtech.api.util.ItemEjectionHelper;

/** Compact output ledger: stack objects describe types, while quantities never pass through int or long. */
public final class ProxyThreadOutputs {

    final ItemStack[] items;
    final FluidStack[] fluids;
    final BigInteger[] itemAmounts;
    final BigInteger[] fluidAmounts;

    public ProxyThreadOutputs(ItemStack[] items, FluidStack[] fluids) {
        this.items = items == null ? new ItemStack[0] : new ItemStack[items.length];
        this.fluids = fluids == null ? new FluidStack[0] : new FluidStack[fluids.length];
        itemAmounts = new BigInteger[this.items.length];
        fluidAmounts = new BigInteger[this.fluids.length];
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = items[i] == null ? null : items[i].copy();
            itemAmounts[i] = BigInteger.valueOf(items[i] == null ? 0 : Math.max(0, items[i].stackSize));
            if (this.items[i] != null) this.items[i].stackSize = 1;
        }
        for (int i = 0; i < this.fluids.length; i++) {
            this.fluids[i] = fluids[i] == null ? null : fluids[i].copy();
            fluidAmounts[i] = BigInteger.valueOf(fluids[i] == null ? 0 : Math.max(0, fluids[i].amount));
            if (this.fluids[i] != null) this.fluids[i].amount = 1;
        }
    }

    private BigInteger largest() {
        BigInteger largest = BigInteger.ZERO;
        for (BigInteger n : itemAmounts) largest = largest.max(n);
        for (BigInteger n : fluidAmounts) largest = largest.max(n);
        return largest;
    }

    /** Adds one bounded preview per output type; the full quantity remains in the authoritative ledger. */
    public void appendDisplayItems(List<ItemStack> itemOutputs) {
        BigInteger stackLimit = BigInteger.valueOf(Integer.MAX_VALUE);
        for (int i = 0; i < items.length; i++) {
            if (itemAmounts[i].signum() > 0) {
                ItemStack stack = items[i].copy();
                stack.stackSize = itemAmounts[i].min(stackLimit)
                    .intValueExact();
                itemOutputs.add(stack);
            }
        }
    }

    public void appendDisplayFluids(List<FluidStack> fluidOutputs) {
        BigInteger stackLimit = BigInteger.valueOf(Integer.MAX_VALUE);
        for (int i = 0; i < fluids.length; i++) {
            if (fluidAmounts[i].signum() > 0) {
                FluidStack stack = fluids[i].copy();
                stack.amount = fluidAmounts[i].min(stackLimit)
                    .intValueExact();
                fluidOutputs.add(stack);
            }
        }
    }

    /** Finished output stays in the ledger until the controller network accepts it. */
    public boolean flush(MTEMultiBlockBase machine) {
        return machine instanceof ProxyFactory factory && flushAe(machine, factory.getProxy());
    }

    /** Commit long-sized AE transfers directly; reduce each balance only by the accepted amount. */
    public boolean flushAe(MTEMultiBlockBase machine, appeng.me.helpers.AENetworkProxy network) {
        if (largest().signum() == 0) return true;
        if (network == null || !network.isActive()) return false;
        try {
            MachineSource source = new MachineSource((IActionHost) machine.getBaseMetaTileEntity());
            for (int i = 0; i < items.length; i++) {
                if (itemAmounts[i].signum() <= 0) continue;
                long amount = itemAmounts[i].min(BigInteger.valueOf(Long.MAX_VALUE))
                    .longValue();
                IAEItemStack request = AEItemStack.create(items[i]);
                request.setStackSize(amount);
                IAEItemStack remaining = network.getStorage()
                    .getItemInventory()
                    .injectItems(request, Actionable.MODULATE, source);
                long accepted = amount - (remaining == null ? 0 : remaining.getStackSize());
                if (accepted > 0) {
                    itemAmounts[i] = itemAmounts[i].subtract(BigInteger.valueOf(accepted));
                    machine.markDirty();
                    if (GTNHAdjust.debug) GTNHAdjust.LOG
                        .info("[proxy-ae] output item={} accepted={} remaining={}", items[i], accepted, itemAmounts[i]);
                }
            }
            for (int i = 0; i < fluids.length; i++) {
                if (fluidAmounts[i].signum() <= 0) continue;
                long amount = fluidAmounts[i].min(BigInteger.valueOf(Long.MAX_VALUE))
                    .longValue();
                IAEFluidStack request = AEFluidStack.create(fluids[i]);
                request.setStackSize(amount);
                IAEFluidStack remaining = network.getStorage()
                    .getFluidInventory()
                    .injectItems(request, Actionable.MODULATE, source);
                long accepted = amount - (remaining == null ? 0 : remaining.getStackSize());
                if (accepted > 0) {
                    fluidAmounts[i] = fluidAmounts[i].subtract(BigInteger.valueOf(accepted));
                    machine.markDirty();
                    if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                        "[proxy-ae] output fluid={} accepted={} remaining={}",
                        fluids[i],
                        accepted,
                        fluidAmounts[i]);
                }
            }
        } catch (GridAccessException e) {
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info("[proxy-ae] network unavailable during output");
        }
        return largest().signum() == 0;
    }

    /** Native transactional item/fluid bus fallback for machines without an active AE node. */
    public boolean flushBuses(MTEMultiBlockBase machine) {
        BigInteger largest = largest();
        if (largest.signum() == 0) return true;
        int high = largest.min(BigInteger.valueOf(Integer.MAX_VALUE))
            .intValueExact();
        if (transfer(machine, largest, high, false)) {
            transfer(machine, largest, high, true);
            return largest().signum() == 0;
        }
        if (high == 1 || !transfer(machine, largest, 1, false)) return false;
        int low = 1;
        high--;
        while (low < high) {
            int middle = (int) (((long) low + high + 1) / 2);
            if (transfer(machine, largest, middle, false)) low = middle;
            else high = middle - 1;
        }
        if (low > 0) transfer(machine, largest, low, true);
        return largest().signum() == 0;
    }

    private static int slice(BigInteger amount, BigInteger largest, int units) {
        if (amount.signum() == 0) return 0;
        return amount.multiply(BigInteger.valueOf(units))
            .divide(largest)
            .max(BigInteger.ONE)
            .intValueExact();
    }

    private boolean transfer(MTEMultiBlockBase machine, BigInteger largest, int units, boolean commit) {
        ItemEjectionHelper itemHelper = new ItemEjectionHelper(machine.getOutputBusses(), machine.protectsExcessItem());
        FluidEjectionHelper fluidHelper = new FluidEjectionHelper(
            machine.getOutputHatches(),
            machine.protectsExcessFluid());
        int[] itemSlice = new int[items.length];
        int[] fluidSlice = new int[fluids.length];
        for (int i = 0; i < items.length; i++) {
            itemSlice[i] = slice(itemAmounts[i], largest, units);
            if (itemSlice[i] > 0
                && itemHelper.ejectItems(Collections.singletonList(items[i]), itemSlice[i]) != itemSlice[i])
                return false;
        }
        for (int i = 0; i < fluids.length; i++) {
            fluidSlice[i] = slice(fluidAmounts[i], largest, units);
            if (fluidSlice[i] > 0
                && fluidHelper.ejectFluids(Collections.singletonList(fluids[i]), fluidSlice[i]) != fluidSlice[i])
                return false;
        }
        if (commit) {
            itemHelper.commit();
            fluidHelper.commit();
            for (int i = 0; i < items.length; i++)
                itemAmounts[i] = itemAmounts[i].subtract(BigInteger.valueOf(itemSlice[i]));
            for (int i = 0; i < fluids.length; i++)
                fluidAmounts[i] = fluidAmounts[i].subtract(BigInteger.valueOf(fluidSlice[i]));
            machine.markDirty();
        }
        return true;
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList itemTags = new NBTTagList();
        for (int i = 0; i < items.length; i++) if (items[i] != null && itemAmounts[i].signum() > 0) {
            NBTTagCompound entry = new NBTTagCompound();
            GTUtility.saveItem(entry, "stack", items[i]);
            entry.setString("amount", itemAmounts[i].toString());
            itemTags.appendTag(entry);
        }
        tag.setTag("items", itemTags);
        NBTTagList fluidTags = new NBTTagList();
        for (int i = 0; i < fluids.length; i++) if (fluids[i] != null && fluidAmounts[i].signum() > 0) {
            NBTTagCompound entry = fluids[i].writeToNBT(new NBTTagCompound());
            entry.setString("bigAmount", fluidAmounts[i].toString());
            fluidTags.appendTag(entry);
        }
        tag.setTag("fluids", fluidTags);
        return tag;
    }

    public static ProxyThreadOutputs load(NBTTagCompound tag) {
        NBTTagList itemTags = tag.getTagList("items", 10), fluidTags = tag.getTagList("fluids", 10);
        ItemStack[] items = new ItemStack[itemTags.tagCount()];
        FluidStack[] fluids = new FluidStack[fluidTags.tagCount()];
        for (int i = 0; i < items.length; i++) items[i] = GTUtility.loadItem(itemTags.getCompoundTagAt(i), "stack");
        for (int i = 0; i < fluids.length; i++)
            fluids[i] = FluidStack.loadFluidStackFromNBT(fluidTags.getCompoundTagAt(i));
        ProxyThreadOutputs result = new ProxyThreadOutputs(items, fluids);
        for (int i = 0; i < items.length; i++) if (itemTags.getCompoundTagAt(i)
            .hasKey("amount", 8))
            result.itemAmounts[i] = new BigInteger(
                itemTags.getCompoundTagAt(i)
                    .getString("amount"));
        for (int i = 0; i < fluids.length; i++) if (fluidTags.getCompoundTagAt(i)
            .hasKey("bigAmount", 8))
            result.fluidAmounts[i] = new BigInteger(
                fluidTags.getCompoundTagAt(i)
                    .getString("bigAmount"));
        return result;
    }
}
