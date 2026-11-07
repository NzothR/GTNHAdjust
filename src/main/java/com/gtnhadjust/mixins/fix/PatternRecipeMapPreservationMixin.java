package com.gtnhadjust.mixins.fix;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.terminal.PatternRecipeMapPreservation;
import com.xyp.gtneverreset.common.me.PatternRecipeMap;
import com.xyp.gtneverreset.common.terminal.DualTerminalGuiObject;

/** Preserve the source pattern's map when manually editing invalidates GTNR's recipe snapshot. */
@Mixin(targets = "com.xyp.gtneverreset.common.terminal.DualTerminalGuiObject", remap = false)
public abstract class PatternRecipeMapPreservationMixin {

    @Redirect(
        method = "encode",
        at = @At(
            value = "INVOKE",
            target = "Lcom/xyp/gtneverreset/common/terminal/DualTerminalGuiObject;getRecipeMapName()Ljava/lang/String;"),
        remap = false)
    private String gtnhadjust$preserveEditedMap(DualTerminalGuiObject terminal) {
        String current = terminal.getRecipeMapName();
        if (current != null) return current;
        ItemStack terminalItem = terminal.getItemStack();
        NBTTagCompound data = terminalItem == null ? null : terminalItem.getTagCompound();
        String draft = data != null && data.hasKey("gtnrQuickRecipeMap", 10) ? data.getCompoundTag("gtnrQuickRecipeMap")
            .getString("Name") : null;
        IInventory patterns = terminal.getInventoryByName("pattern");
        ItemStack existing = patterns == null ? null : patterns.getStackInSlot(1);
        String result = PatternRecipeMapPreservation.select(current, draft, PatternRecipeMap.name(existing));
        if (result != null && GTNHAdjust.debug) GTNHAdjust.LOG.info("Preserved edited pattern recipe map {}", result);
        return result;
    }
}
