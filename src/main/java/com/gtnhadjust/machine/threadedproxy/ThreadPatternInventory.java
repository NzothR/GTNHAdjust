package com.gtnhadjust.machine.threadedproxy;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.gtnhadjust.GTNHAdjust;

import appeng.api.implementations.ICraftingPatternItem;

/** Physical single-pattern slots, serialized with the controller rather than dropped as loose items. */
final class ThreadPatternInventory extends ItemStackHandler {

    private final ThreadedJointProxy owner;

    ThreadPatternInventory(ThreadedJointProxy owner) {
        super(GTNHAdjust.threadedProxyPatternSlots);
        this.owner = owner;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return owner.canEditLocators() && slot < GTNHAdjust.threadedProxyPatternSlots
            && stack != null
            && stack.getItem() instanceof ICraftingPatternItem;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return owner.canEditLocators() ? super.extractItem(slot, amount, simulate) : null;
    }

    @Override
    protected void onContentsChanged(int slot) {
        ItemStack stack = getStackInSlot(slot);
        if (stack != null && (!stack.hasTagCompound() || !stack.getTagCompound()
            .hasKey(ThreadPatternSettings.KEY))) new ThreadPatternSettings().write(stack);
        owner.patternsChanged();
    }

    /** Capacity increases apply on reload; decreases preserve filled saved slots for extraction. */
    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        NBTTagCompound sized = (NBTTagCompound) tag.copy();
        sized.setInteger("Size", Math.max(tag.getInteger("Size"), GTNHAdjust.threadedProxyPatternSlots));
        super.deserializeNBT(sized);
    }
}
