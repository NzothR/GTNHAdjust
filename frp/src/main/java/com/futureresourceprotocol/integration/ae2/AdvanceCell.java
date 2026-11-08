package com.futureresourceprotocol.integration.ae2;

import java.util.UUID;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.device.TerminalContainer;

/** The cell keeps its own mark list and uses the portable terminal's marking screen. */
public final class AdvanceCell extends Item {

    public AdvanceCell() {
        setUnlocalizedName("frp.advance_cell");
        setTextureName("appliedenergistics2:ItemCreativeStorageCell");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return 0xffd45a;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound()
                .setString(
                    TerminalContainer.SESSION_TAG,
                    UUID.randomUUID()
                        .toString());
            FutureResourceProtocol.debug(
                "open advance cell marks player={} slot={}",
                player.getCommandSenderName(),
                player.inventory.currentItem);
            player.openGui(FutureResourceProtocol.instance, 1, world, player.inventory.currentItem, 0, 0);
        }
        return stack;
    }
}
