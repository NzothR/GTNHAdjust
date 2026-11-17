package com.gtnhadjust.mixins.gtnr;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.xyp.gtneverreset.compat.nei.QuickTerminalRecipeTransferHandler;

/** Preserve every NEI NC ingredient until recipe-aware encoding classifies catalog and external tools. */
@Mixin(value = QuickTerminalRecipeTransferHandler.class, remap = false)
public abstract class NonConsumedImportMixin {

    /** Called only for zero-count non-circuit items while the terminal's retain-NC option is enabled. */
    @Redirect(
        method = "retainSupportedNonConsumables",
        at = @At(
            value = "INVOKE",
            target = "Lcom/xyp/gtneverreset/compat/nei/TerminalRecipeIngredients;isVirtualMold(Lnet/minecraft/item/ItemStack;)Z"),
        remap = false)
    private static boolean gtnhadjust$retainExternalTool(ItemStack item) {
        return true;
    }
}
