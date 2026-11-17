package com.gtnhadjust.mixins.gtnr;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.compat.GtnrToolPatterns;
import com.xyp.gtneverreset.common.me.CircuitPatternCodec;

import appeng.api.networking.crafting.ICraftingPatternDetails;

/** Preserve all NC identities on new processing patterns and restore them when edited. */
@Mixin(value = CircuitPatternCodec.class, remap = false)
public abstract class PatternToolsMixin {

    @Inject(
        method = "runtime(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lappeng/api/networking/crafting/ICraftingPatternDetails;[Lnet/minecraft/item/ItemStack;)Lappeng/api/networking/crafting/ICraftingPatternDetails;",
        at = @At("RETURN"),
        cancellable = true,
        remap = false)
    private static void gtnhadjust$encode(ItemStack pattern, World world, ICraftingPatternDetails original,
        ItemStack[] molds, CallbackInfoReturnable<ICraftingPatternDetails> result) {
        result.setReturnValue(GtnrToolPatterns.encode(pattern, world, original, result.getReturnValue()));
    }

    @Inject(method = "editablePattern", at = @At("RETURN"), cancellable = true, remap = false)
    private static void gtnhadjust$edit(ItemStack pattern, CallbackInfoReturnable<ItemStack> result) {
        result.setReturnValue(GtnrToolPatterns.editable(pattern, result.getReturnValue()));
    }
}
