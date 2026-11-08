package com.futureresourceprotocol.integration.ae2;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import appeng.api.implementations.tiles.IChestOrDrive;
import appeng.api.storage.ICellHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEStackType;
import appeng.util.item.AEItemStackType;

public final class AdvanceCellHandler implements ICellHandler {

    @Override
    public boolean isCell(ItemStack stack) {
        return stack != null && stack.getItem() instanceof AdvanceCell;
    }

    @Override
    public IMEInventoryHandler getCellInventory(ItemStack stack, ISaveProvider host, IAEStackType<?> type) {
        return isCell(stack) && type == AEItemStackType.ITEM_STACK_TYPE ? new AdvanceCellInventory(stack, host) : null;
    }

    @Override
    public IIcon getTopTexture_Light() {
        return null;
    }

    @Override
    public IIcon getTopTexture_Medium() {
        return null;
    }

    @Override
    public IIcon getTopTexture_Dark() {
        return null;
    }

    @Override
    public void openChestGui(EntityPlayer player, IChestOrDrive chest, ICellHandler handler,
        IMEInventoryHandler inventory, ItemStack stack, StorageChannel channel) {}

    @Override
    public int getStatusForCell(ItemStack stack, IMEInventory inventory) {
        return 2;
    }

    @Override
    public double cellIdleDrain(ItemStack stack, IMEInventory inventory) {
        return 0;
    }
}
