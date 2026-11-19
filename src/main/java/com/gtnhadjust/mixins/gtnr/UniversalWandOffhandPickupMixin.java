package com.gtnhadjust.mixins.gtnr;

import net.minecraft.item.ItemStack;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.xyp.gtneverreset.common.wand.UniversalWandDropBuffer", remap = false)
public abstract class UniversalWandOffhandPickupMixin {

    /** Backhand appends its offhand slot after the 36 vanilla main-inventory slots. */
    @Redirect(
        method = "insertIntoInventory",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/player/InventoryPlayer;mainInventory:[Lnet/minecraft/item/ItemStack;",
            opcode = Opcodes.GETFIELD,
            args = "array=length",
            ordinal = 0,
            remap = true))
    private static int gtnhadjust$skipEmptyOffhand(ItemStack[] inventory) {
        return inventory.length == 37 && inventory[36] == null ? 36 : inventory.length;
    }
}
