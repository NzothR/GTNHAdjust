package com.futureresourceprotocol.integration.ae2;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** AE cell which reads the shared world mark list. */
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

}
