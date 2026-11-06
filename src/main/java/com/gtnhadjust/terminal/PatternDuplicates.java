package com.gtnhadjust.terminal;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.gtnhadjust.GTNHAdjust;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEStack;

public final class PatternDuplicates {

    private PatternDuplicates() {}

    /** Returns null when a pattern cannot be decoded; empty output sets are never considered duplicates. */
    public static Set<NBTTagCompound> outputKinds(ItemStack pattern, World world) {
        if (pattern == null || world == null || !(pattern.getItem() instanceof ICraftingPatternItem)) return null;
        try {
            ICraftingPatternDetails details = ((ICraftingPatternItem) pattern.getItem())
                .getPatternForItem(pattern, world);
            if (details == null || details.getAEOutputs() == null) return null;
            Set<NBTTagCompound> kinds = new HashSet<>();
            for (IAEStack<?> output : details.getAEOutputs()) {
                if (output == null || output.getStackSize() <= 0) continue;
                IAEStack<?> normalized = output.copy()
                    .reset()
                    .setStackSize(1);
                kinds.add(normalized.toNBTGeneric());
            }
            return kinds.isEmpty() ? null : kinds;
        } catch (RuntimeException failure) {
            if (GTNHAdjust.terminalDebug)
                GTNHAdjust.LOG.info("Quick terminal: could not decode pattern outputs for duplicate check", failure);
            return null;
        }
    }

    public static boolean contains(IInventory inventory, int excludeSlot, ItemStack candidate, World world) {
        if (inventory == null) return false;
        Set<NBTTagCompound> wanted = outputKinds(candidate, world);
        if (wanted == null) return false;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            if (i == excludeSlot) continue;
            Set<NBTTagCompound> existing = outputKinds(inventory.getStackInSlot(i), world);
            if (wanted.equals(existing)) return true;
        }
        return false;
    }
}
