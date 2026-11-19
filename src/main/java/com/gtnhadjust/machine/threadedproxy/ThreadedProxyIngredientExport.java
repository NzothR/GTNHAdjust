package com.gtnhadjust.machine.threadedproxy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import gregtech.api.util.GTUtility;
import gregtech.api.util.GTUtility.FluidId;
import gregtech.api.util.GTUtility.ItemId;
import gregtech.common.tileentities.machines.MTEHatchInputBusME;
import gregtech.common.tileentities.machines.MTEHatchInputME;

final class ThreadedProxyIngredientExport {

    private ThreadedProxyIngredientExport() {}

    /** Append encoded inputs to the attached ME hatches in structure order. */
    static void append(ThreadedJointProxy machine) {
        if (!GTNHAdjust.threadedProxyExportIngredients || !machine.getBaseMetaTileEntity()
            .isServerSide()) return;
        Set<ItemId> markedItems = new LinkedHashSet<>();
        Set<FluidId> markedFluids = new LinkedHashSet<>();
        List<Consumer<ItemStack>> freeItemSlots = new ArrayList<>();
        List<Consumer<FluidStack>> freeFluidSlots = new ArrayList<>();
        for (var input : GTUtility.validMTEList(machine.mInputBusses)) {
            if (!(input instanceof MTEHatchInputBusME bus)) continue;
            for (int i = 0; i < MTEHatchInputBusME.SLOT_COUNT; i++) {
                ItemStack existing = bus.getSlotConfig(i);
                if (existing != null) markedItems.add(ItemId.createNoCopy(existing));
                else if (!bus.isAutoPullItemList()) {
                    int slot = i;
                    freeItemSlots.add(
                        stack -> {
                            if (bus.setSlotConfigAndUpdate(slot, stack)) bus.getBaseMetaTileEntity()
                                .markDirty();
                        });
                }
            }
        }
        for (var input : GTUtility.validMTEList(machine.mInputHatches)) {
            if (!(input instanceof MTEHatchInputME hatch)) continue;
            for (int i = 0; i < MTEHatchInputME.SLOT_COUNT; i++) {
                FluidStack existing = hatch.getSlotConfig(i);
                if (existing != null) markedFluids.add(FluidId.create(existing));
                else if (!hatch.isAutoPullFluidList()) {
                    int slot = i;
                    freeFluidSlots.add(
                        stack -> {
                            if (hatch.setSlotConfigAndUpdate(slot, stack)) hatch.getBaseMetaTileEntity()
                                .markDirty();
                        });
                }
            }
        }

        var items = freeItemSlots.iterator();
        var fluids = freeFluidSlots.iterator();
        var patterns = machine.patterns();
        var world = machine.getBaseMetaTileEntity()
            .getWorld();
        for (int i = 0; i < patterns.getSlots() && (items.hasNext() || fluids.hasNext()); i++) {
            ItemStack pattern = patterns.getStackInSlot(i);
            if (pattern == null) continue;
            var details = CircuitPatternCodec.decode(pattern.copy(), world);
            if (details == null || details.isCraftable() || details.isInputOnly()) continue;
            for (var ingredient : details.getAEInputs()) {
                if (ingredient == null || ingredient.getStackSize() <= 0) continue;
                if (ingredient instanceof IAEItemStack item && items.hasNext()) {
                    ItemStack stack = item.getItemStack();
                    if (markedItems.add(ItemId.createNoCopy(stack))) items.next()
                        .accept(GTUtility.copyAmount(1, stack));
                } else if (ingredient instanceof IAEFluidStack fluid && fluids.hasNext()) {
                    FluidStack stack = fluid.getFluidStack();
                    if (markedFluids.add(FluidId.create(stack))) fluids.next()
                        .accept(GTUtility.copyAmount(1, stack));
                }
            }
        }
    }
}
