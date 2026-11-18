package com.gtnhadjust.machine.collector;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.Constants.NBT;

import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;
import appeng.items.AEBaseInfiniteCell;
import appeng.me.storage.CreativeCellInventory;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Fixed-content creative storage. Only the compression recipe writes the contents tag. */
final class MatterCompressionCell<T extends IAEStack<T>> extends AEBaseInfiniteCell {

    private static final String CONTENTS = "compressionContents";
    private final IAEStackType<T> type;

    MatterCompressionCell(String name, String texture, IAEStackType<T> type) {
        this.type = type;
        setUnlocalizedName("gtnhadjust." + name);
        setTextureName(texture);
        setCreativeTab(CreativeTabs.tabMisc);
        setMaxStackSize(1);
    }

    @Override
    public boolean isEditable(ItemStack stack) {
        return false;
    }

    @Override
    public IAEStackType<T> getStackType() {
        return type;
    }

    @Override
    public int getTotalTypes(ItemStack stack) {
        return contents(stack).tagCount();
    }

    @Override
    public IAEStackInventory getConfigAEInventory(ItemStack stack) {
        NBTTagList saved = contents(stack);
        IAEStackInventory config = new IAEStackInventory(null, saved.tagCount());
        for (int i = 0; i < saved.tagCount(); i++) {
            config.putAEStackInSlot(i, Platform.readStackNBT(saved.getCompoundTagAt(i)));
        }
        return config;
    }

    @Override
    public IMEInventoryHandler<T> getCellInventory(ItemStack stack) {
        return new CreativeCellInventory<T>(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        lines.add(StatCollector.translateToLocalFormatted("gtnhadjust.compression_cell.types", getTotalTypes(stack)));
        lines.add(StatCollector.translateToLocal("gtnhadjust.compression_cell.behavior"));
    }

    /** Preserve previous entries and write each exact AE resource identity at most once. */
    ItemStack append(ItemStack existing, IItemList<T> additions) {
        IItemList<T> combined = type.createPrimitiveList();
        if (existing != null) {
            NBTTagList saved = contents(existing);
            for (int i = 0; i < saved.tagCount(); i++) {
                IAEStack<?> entry = Platform.readStackNBT(saved.getCompoundTagAt(i));
                if (entry != null && entry.getStackType() == type) {
                    @SuppressWarnings("unchecked")
                    T resource = (T) entry;
                    combined.add(resource);
                }
            }
        }
        boolean changed = false;
        for (T addition : additions) {
            if (combined.findPrecise(addition) != null) continue;
            combined.add(addition);
            changed = true;
        }
        if (!changed) return null;
        NBTTagList saved = new NBTTagList();
        for (T resource : combined) {
            saved.appendTag(
                Platform.writeStackNBT(
                    resource.copy()
                        .setStackSize(1),
                    new NBTTagCompound()));
        }
        ItemStack result = new ItemStack(this);
        result.setTagCompound(new NBTTagCompound());
        result.getTagCompound()
            .setTag(CONTENTS, saved);
        return result;
    }

    private static NBTTagList contents(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound()
            .getTagList(CONTENTS, NBT.TAG_COMPOUND) : new NBTTagList();
    }
}
