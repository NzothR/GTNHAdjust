package com.gtnhadjust.terminal;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;

import appeng.api.implementations.ICraftingPatternItem;

public final class PatternDuplicates {

    private PatternDuplicates() {}

    public static boolean contains(IInventory inventory, int excludeSlot, ItemStack candidate) {
        if (inventory == null || candidate == null || !(candidate.getItem() instanceof ICraftingPatternItem))
            return false;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            if (i == excludeSlot) continue;
            ItemStack existing = inventory.getStackInSlot(i);
            if (existing != null && existing.isItemEqual(candidate)
                && ItemStack.areItemStackTagsEqual(existing, candidate)) return true;
        }
        return false;
    }

    public static boolean isAssemblyDuplicate(IInventory inventory, int slot, ItemStack candidate) {
        try {
            Object source = Reflection.field(inventory, "src");
            return source instanceof SuperMTEHatchCraftingInputME && contains(inventory, slot, candidate);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }
}
