package com.gtnhadjust.mixins.fix;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.items.misc.ItemEncodedPattern;

/** AE2 marks failed patterns invalid in-place; decode a copy so temporary failures cannot poison stored patterns. */
@Mixin(targets = "com.xyp.gtneverreset.common.me.AssemblerMatrixPatternState", remap = false)
public abstract class MatrixPatternDecodeMixin {

    @Redirect(
        method = "decodePattern",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/implementations/ICraftingPatternItem;getPatternForItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;)Lappeng/api/networking/crafting/ICraftingPatternDetails;"),
        remap = false)
    private ICraftingPatternDetails gtnhadjust$decodeCopy(ICraftingPatternItem item, ItemStack stack, World world) {
        return item.getPatternForItem(item instanceof ItemEncodedPattern ? stack.copy() : stack, world);
    }
}
