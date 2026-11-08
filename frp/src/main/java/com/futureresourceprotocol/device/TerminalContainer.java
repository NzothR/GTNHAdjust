package com.futureresourceprotocol.device;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.futureresourceprotocol.FutureResourceProtocol;

public final class TerminalContainer extends Container {

    public static final String SESSION_TAG = "frpSession";
    public final boolean marking;
    public final int heldSlot;
    private final String session;

    public TerminalContainer(InventoryPlayer inventory, int slot, boolean marking) {
        this.marking = marking;
        this.heldSlot = slot;
        ItemStack terminal = slot >= 0 && slot < 9 ? inventory.getStackInSlot(slot) : null;
        this.session = terminal != null && terminal.hasTagCompound() ? terminal.getTagCompound()
            .getString(SESSION_TAG) : "";
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 153 + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 211));
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        ItemStack current = player.inventory.getStackInSlot(heldSlot);
        boolean valid = current != null && current.getItem() == FutureResourceProtocol.terminal
            && player.inventory.currentItem == heldSlot
            && current.hasTagCompound()
            && sameSession(
                session,
                current.getTagCompound()
                    .getString(SESSION_TAG));
        if (!valid && !player.worldObj.isRemote) FutureResourceProtocol.debug(
            "terminal session closed player={} expectedSlot={} selectedSlot={} hasTerminal={} hasSession={}",
            player.getCommandSenderName(),
            heldSlot,
            player.inventory.currentItem,
            current != null && current.getItem() == FutureResourceProtocol.terminal,
            current != null && current.hasTagCompound()
                && !current.getTagCompound()
                    .getString(SESSION_TAG)
                    .isEmpty());
        return valid;
    }

    static boolean sameSession(String expected, String actual) {
        return expected != null && !expected.isEmpty() && expected.equals(actual);
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
