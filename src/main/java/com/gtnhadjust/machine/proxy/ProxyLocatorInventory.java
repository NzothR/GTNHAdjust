package com.gtnhadjust.machine.proxy;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.gtnhadjust.GTNHAdjust;

/** Real single-item slots; saved slot indices include holes and capacity-reduction overflow. */
public final class ProxyLocatorInventory extends ItemStackHandler {

    private final ProxyFactory owner;

    ProxyLocatorInventory(ProxyFactory owner) {
        super(GTNHAdjust.proxyFactoryLocatorSlots);
        this.owner = owner;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!owner.canEditLocators() || slot >= GTNHAdjust.proxyFactoryLocatorSlots) return false;
        MachineLocatorRecord candidate = MachineLocatorItem.read(stack);
        if (candidate == null) return false;
        for (int i = 0; i < getSlots(); i++) {
            if (i == slot) continue;
            MachineLocatorRecord existing = MachineLocatorItem.read(getStackInSlot(i));
            if (existing != null && candidate.sameRecord(existing)) return false;
        }
        return true;
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
        owner.onLocatorInventoryChanged();
    }

    /** Config reductions retain every saved slot; only the configured prefix accepts new insertions. */
    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        NBTTagCompound sized = (NBTTagCompound) tag.copy();
        sized.setInteger("Size", Math.max(tag.getInteger("Size"), GTNHAdjust.proxyFactoryLocatorSlots));
        super.deserializeNBT(sized);
    }
}
