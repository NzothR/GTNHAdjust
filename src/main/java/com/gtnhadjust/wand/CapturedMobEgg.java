package com.gtnhadjust.wand;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Facing;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gtnhadjust.GTNHAdjust;

/** A registered entity name, not the defeated entity's state. */
public final class CapturedMobEgg extends Item {

    private static final String ENTITY = "entity";

    public CapturedMobEgg() {
        setUnlocalizedName("gtnhadjust.captured_mob_egg");
        setTextureName("minecraft:spawn_egg");
    }

    public static ItemStack forEntity(EntityLivingBase victim) {
        String name = EntityList.getEntityString(victim);
        if (name == null || EntityList.stringToClassMapping.get(name) != victim.getClass()) return null;
        ItemStack stack = new ItemStack(GTNHAdjust.capturedMobEgg);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(ENTITY, name);
        stack.setTagCompound(tag);
        return stack;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String name = stack.hasTagCompound() ? stack.getTagCompound()
            .getString(ENTITY) : "";
        return StatCollector.translateToLocal("item.gtnhadjust.captured_mob_egg.name") + " "
            + StatCollector.translateToLocal("entity." + name + ".name");
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (side < 0 || side >= 6 || !stack.hasTagCompound()) return false;
        int nx = x + Facing.offsetsXForSide[side];
        int ny = y + Facing.offsetsYForSide[side];
        int nz = z + Facing.offsetsZForSide[side];
        if (!player.canPlayerEdit(nx, ny, nz, side, stack)) return false;
        if (world.isRemote) return true;
        String name = stack.getTagCompound()
            .getString(ENTITY);
        Class<?> type = EntityList.stringToClassMapping.get(name);
        if (type == null || !EntityLivingBase.class.isAssignableFrom(type) || EntityPlayer.class.isAssignableFrom(type))
            return false;
        Entity entity = EntityList.createEntityByName(name, world);
        if (!(entity instanceof EntityLivingBase)) return false;
        entity.setLocationAndAngles(nx + 0.5, ny, nz + 0.5, world.rand.nextFloat() * 360.0F, 0.0F);
        if (entity instanceof EntityLiving living) living.onSpawnWithEgg(null);
        if (!world.spawnEntityInWorld(entity)) return false;
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        return true;
    }
}
