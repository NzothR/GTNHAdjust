package com.futureresourceprotocol.device;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.futureresourceprotocol.FutureResourceProtocol;

public final class TerminalContainer extends Container {

    public final boolean marking;
    public final int heldSlot;
    private final ItemStack terminal;

    public TerminalContainer(InventoryPlayer inventory, int slot, boolean marking) {
        this.marking = marking;
        this.heldSlot = slot;
        this.terminal = slot >= 0 && slot < 9 ? inventory.getStackInSlot(slot) : null;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 153 + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 211));
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return terminal != null && terminal.getItem() == FutureResourceProtocol.terminal
            && player.inventory.currentItem == heldSlot
            && player.inventory.getStackInSlot(heldSlot) == terminal;
    }

    @Override
    public ItemStack slotClick(int slot, int button, int mode, EntityPlayer player) {
        if (slot >= 0 && slot < inventorySlots.size() && ((Slot) inventorySlots.get(slot)).getSlotIndex() == heldSlot)
            return null;
        if (mode == 2 && button == heldSlot) return null;
        return super.slotClick(slot, button, mode, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
        return null;
    }
}
