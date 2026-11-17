package com.gtnhadjust.machine.reshaping;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.GregTechAPI;

/** Fourteen metadata tiers using EyeOfHarmonyBuffer's original PhotonControllerUpgrades textures. */
public final class ReshapingRingBlock extends Block {

    private IIcon[] icons;

    public ReshapingRingBlock() {
        super(Material.iron);
        setBlockName("gtnhadjust.reshaping_ring");
        setHardness(9);
        setResistance(5);
        setHarvestLevel("wrench", 1);
        setCreativeTab(CreativeTabs.tabBlock);
        GregTechAPI.registerMachineBlock(this, -1);
    }

    @Override
    public int damageDropped(int meta) {
        return meta;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[14];
        for (int meta = 0; meta < icons.length; meta++)
            icons[meta] = register.registerIcon("gtnhadjust:reshaping/rings/" + meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return icons[meta];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
        for (int meta = 0; meta < 14; meta++) list.add(new ItemStack(item, 1, meta));
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        GregTechAPI.causeMachineUpdate(world, x, y, z);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int meta) {
        GregTechAPI.causeMachineUpdate(world, x, y, z);
        super.breakBlock(world, x, y, z, block, meta);
    }

    public static final class RingItem extends ItemBlock {

        public RingItem(Block block) {
            super(block);
            setHasSubtypes(true);
        }

        @Override
        public int getMetadata(int meta) {
            return meta;
        }

        @Override
        public String getUnlocalizedName(ItemStack stack) {
            return super.getUnlocalizedName() + "." + stack.getItemDamage();
        }
    }
}
