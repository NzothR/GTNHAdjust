package com.gtnhadjust.diagnostic;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import com.gtnhadjust.GTNHAdjust;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.PatternHelper;
import appeng.items.misc.ItemEncodedPattern;
import appeng.util.Platform;

/** Recover only normal crafting patterns whose currently resolved output exactly matches the saved output. */
public final class MatrixPatternRecovery {

    private MatrixPatternRecovery() {}

    public static int recover(IInventory inventory, World world) {
        if (inventory == null || world == null || world.isRemote) return 0;
        int repaired = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack saved = inventory.getStackInSlot(slot);
            ItemStack candidate = validatedCopy(saved, world);
            if (candidate == null) continue;
            inventory.setInventorySlotContents(slot, candidate);
            repaired++;
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info("Recovered validated matrix pattern in slot {}", slot);
        }
        if (repaired > 0) {
            inventory.markDirty();
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info("Validated and recovered {} matrix crafting patterns", repaired);
        }
        return repaired;
    }

    static ItemStack validatedCopy(ItemStack saved, World world) {
        if (saved == null || saved.getItem()
            .getClass() != ItemEncodedPattern.class || !saved.hasTagCompound()) return null;
        NBTTagCompound original = saved.getTagCompound();
        if (!original.getBoolean("InvalidPattern") || !original.getBoolean("crafting")) return null;
        NBTTagList input = original.getTagList("in", 10), output = original.getTagList("out", 10);
        if (input.tagCount() != 9 || output.tagCount() != 1) return null;
        NBTTagCompound outputTag = output.getCompoundTagAt(0);
        ItemStack expected = Platform.loadItemStackFromNBT(outputTag);
        if (expected == null) return null;
        long expectedCount = expected.stackSize == 0 ? outputTag.getLong("Cnt") : expected.stackSize;
        if (expectedCount <= 0) return null;
        ItemStack probe = saved.copy();
        probe.getTagCompound()
            .removeTag("InvalidPattern");
        try {
            ICraftingPatternDetails details = new PatternHelper(probe, world);
            IAEItemStack[] actual = details.getOutputs();
            if (!details.isCraftable() || actual == null
                || actual.length != 1
                || actual[0] == null
                || actual[0].getStackSize() != expectedCount) return null;
            ItemStack actualStack = actual[0].getItemStack();
            if (actualStack == null || !expected.isItemEqual(actualStack)
                || !ItemStack.areItemStackTagsEqual(expected, actualStack)) return null;
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
        // Keep every original tag, including author, date and recipe data; change only the stale flag.
        ItemStack repaired = saved.copy();
        repaired.getTagCompound()
            .removeTag("InvalidPattern");
        return repaired;
    }
}
