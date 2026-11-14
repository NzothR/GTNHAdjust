package com.gtnhadjust.machine.proxy;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.gtnhadjust.GTNHAdjust;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

/** Stackable recording medium. Recording one item never mutates the rest of a held stack. */
public final class MachineLocatorItem extends Item {

    private static final String RECORD = "gtnhadjustMachineLocator";
    @SideOnly(Side.CLIENT)
    private ItemStack flashIcon;

    public MachineLocatorItem() {
        setUnlocalizedName("gtnhadjust.machine_locator");
        setMaxStackSize(64);
        setCreativeTab(CreativeTabs.tabMisc);
    }

    public static MachineLocatorRecord read(ItemStack stack) {
        return stack != null && stack.getItem() instanceof MachineLocatorItem
            && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey(RECORD, 10) ? MachineLocatorRecord.load(
                    stack.getTagCompound()
                        .getCompoundTag(RECORD))
                    : null;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!GTNHAdjust.proxyFactoryEnabled || !player.isSneaking()) return false;
        // Let the client send the interaction packet; only the server edits the recorded item/inventory.
        if (world.isRemote) return false;
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof IGregTechTileEntity gt) || !(gt.getMetaTileEntity() instanceof MTEMultiBlockBase))
            return false;
        ItemStack recorded = stack.copy();
        recorded.stackSize = 1;
        if (!recorded.hasTagCompound()) recorded.setTagCompound(new NBTTagCompound());
        recorded.getTagCompound()
            .setTag(
                RECORD,
                MachineLocatorRecord.capture(gt)
                    .save());
        if (stack.stackSize == 1) player.inventory.setInventorySlotContents(player.inventory.currentItem, recorded);
        else {
            // Reserve inventory space before consuming a blank/previously recorded locator.
            if (!insertRecorded(player, recorded)) {
                player.addChatMessage(new ChatComponentTranslation("gtnhadjust.locator.inventory_full"));
                return true;
            }
            stack.stackSize--;
        }
        player.inventory.markDirty();
        player.addChatMessage(new ChatComponentTranslation("gtnhadjust.locator.recorded"));
        return true;
    }

    /** Insert exactly one item, including in creative mode where the native helper can discard on a full inventory. */
    private static boolean insertRecorded(EntityPlayer player, ItemStack recorded) {
        int empty = -1;
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack existing = player.inventory.mainInventory[i];
            if (existing == null) {
                if (empty < 0) empty = i;
            } else if (existing.isItemEqual(recorded) && ItemStack.areItemStackTagsEqual(existing, recorded)
                && existing.stackSize < existing.getMaxStackSize()) {
                    existing.stackSize++;
                    return true;
                }
        }
        if (empty < 0) return false;
        player.inventory.setInventorySlotContents(empty, recorded);
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        // The native flash item supplies its own atlas icon.
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        if (flashIcon == null) flashIcon = ItemList.Tool_DataStick.get(1);
        return flashIcon.getIconIndex();
    }

    @SideOnly(Side.CLIENT)
    public void registerRenderer() {
        net.minecraftforge.client.MinecraftForgeClient.registerItemRenderer(this, new MachineLocatorRenderer());
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add(StatCollector.translateToLocal("gtnhadjust.locator.usage"));
        MachineLocatorRecord record = read(stack);
        if (record == null) lines.add(StatCollector.translateToLocal("gtnhadjust.locator.empty"));
        else if (GuiScreen.isShiftKeyDown()) {
            lines.add(
                record.controller()
                    .getDisplayName());
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "gtnhadjust.locator.position",
                    record.dimension,
                    record.x,
                    record.y,
                    record.z));
        } else lines.add(StatCollector.translateToLocal("gtnhadjust.locator.shift"));
    }
}
