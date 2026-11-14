package com.gtnhadjust.machine.proxy;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/** A detached location and controller identity; no live tile references or placement bindings are retained. */
public final class MachineLocatorRecord {

    public final int dimension, x, y, z;
    private final ItemStack controller;
    private final String machineName;

    private MachineLocatorRecord(int dimension, int x, int y, int z, ItemStack controller, String machineName) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.controller = controller;
        this.machineName = machineName;
    }

    public static MachineLocatorRecord capture(IGregTechTileEntity tile) {
        IMetaTileEntity machine = tile.getMetaTileEntity();
        return new MachineLocatorRecord(
            tile.getWorld().provider.dimensionId,
            tile.getXCoord(),
            tile.getYCoord(),
            tile.getZCoord(),
            machine.getStackForm(1),
            machine.getMetaName());
    }

    public ItemStack controller() {
        return controller.copy();
    }

    /** Match item registry identity, metadata, item NBT and MTE name, while ignoring live machine state. */
    public boolean matches(IMetaTileEntity machine) {
        ItemStack actual = machine.getStackForm(1);
        return machineName.equals(machine.getMetaName()) && controller.isItemEqual(actual)
            && ItemStack.areItemStackTagsEqual(controller, actual);
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("dimension", dimension);
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setString("controllerRegistry", (String) Item.itemRegistry.getNameForObject(controller.getItem()));
        tag.setInteger("controllerMeta", controller.getItemDamage());
        if (controller.hasTagCompound()) tag.setTag(
            "controllerNbt",
            controller.getTagCompound()
                .copy());
        tag.setString("machineName", machineName);
        return tag;
    }

    /** Invalid or incomplete item records cannot supply proxy recipe maps. */
    public static MachineLocatorRecord load(NBTTagCompound tag) {
        if (!tag.hasKey("dimension", 3) || !tag.hasKey("x", 3)
            || !tag.hasKey("y", 3)
            || !tag.hasKey("z", 3)
            || !tag.hasKey("machineName", 8)
            || !tag.hasKey("controllerRegistry", 8)
            || !tag.hasKey("controllerMeta", 3)) return null;
        Item item = (Item) Item.itemRegistry.getObject(tag.getString("controllerRegistry"));
        if (item == null) return null;
        ItemStack controller = new ItemStack(item, 1, tag.getInteger("controllerMeta"));
        if (tag.hasKey("controllerNbt", 10)) controller.setTagCompound(
            (NBTTagCompound) tag.getCompoundTag("controllerNbt")
                .copy());
        return new MachineLocatorRecord(
            tag.getInteger("dimension"),
            tag.getInteger("x"),
            tag.getInteger("y"),
            tag.getInteger("z"),
            controller,
            tag.getString("machineName"));
    }
}
