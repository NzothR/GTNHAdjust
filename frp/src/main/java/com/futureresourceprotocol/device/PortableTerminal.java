package com.futureresourceprotocol.device;

import java.util.UUID;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.futureresourceprotocol.FutureResourceProtocol;

public final class PortableTerminal extends Item {

    public PortableTerminal() {
        setUnlocalizedName("frp.portable_terminal");
        setTextureName("appliedenergistics2:ToolWirelessTerminal");
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
            int mode = player.isSneaking() ? 1 : 0;
            FutureResourceProtocol.debug(
                "open terminal player={} mode={} slot={}",
                player.getCommandSenderName(),
                mode,
                player.inventory.currentItem);
            player.openGui(FutureResourceProtocol.instance, mode, world, player.inventory.currentItem, 0, 0);
        }
        return stack;
    }
}
