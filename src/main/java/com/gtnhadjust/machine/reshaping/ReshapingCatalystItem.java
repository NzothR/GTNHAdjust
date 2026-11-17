package com.gtnhadjust.machine.reshaping;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Dedicated dust, hot ingot and ingot preserve the upstream Harmony material recipe chain. */
public final class ReshapingCatalystItem extends Item {

    private IIcon dust;
    private IIcon ingot;

    public ReshapingCatalystItem() {
        setUnlocalizedName("gtnhadjust.reshaping_catalyst");
        setHasSubtypes(true);
        setCreativeTab(CreativeTabs.tabMaterials);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + stack.getItemDamage();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        dust = register.registerIcon("gregtech:materialicons/SHINY/dust");
        ingot = register.registerIcon("gregtech:materialicons/SHINY/ingot");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int meta) {
        return meta == 0 ? dust : ingot;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return 0x802DC8;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> list) {
        for (int meta = 0; meta < 3; meta++) list.add(new ItemStack(item, 1, meta));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        lines.add(StatCollector.translateToLocal("gtnhadjust.reshaping.catalyst.tooltip"));
    }
}
