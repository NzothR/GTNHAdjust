package com.gtnhadjust.machine.collector;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Stable metadata 0–9 identifies the corresponding dimension tier. */
public final class TierDimensionItem extends Item {

    static final String[] REPRESENTATIVES = { "Ow", "Mo", "De", "As", "Io", "En", "Pr", "Ha", "BC", "An" };
    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public TierDimensionItem() {
        setHasSubtypes(true);
        setMaxDamage(0);
        setUnlocalizedName("gtnhadjust.tier_dimension");
        setCreativeTab(CreativeTabs.tabMisc);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item.gtnhadjust.tier_dimension." + stack.getItemDamage();
    }

    @SideOnly(Side.CLIENT)
    public void registerRenderer() {
        net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(this, new TierDimensionRenderer());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        icons = new IIcon[10];
        for (int i = 0; i < icons.length; i++)
            icons[i] = register.registerIcon("gtnhadjust:tier_dimension/" + REPRESENTATIVES[i]);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return icons[Math.max(0, Math.min(9, damage))];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return 0xFFD875;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubItems(Item item, CreativeTabs tab, List items) {
        for (int tier = 0; tier < 10; tier++) items.add(new ItemStack(item, 1, tier));
    }
}
