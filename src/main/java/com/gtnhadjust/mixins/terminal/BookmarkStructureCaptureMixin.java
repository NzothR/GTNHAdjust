package com.gtnhadjust.mixins.terminal;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.terminal.BookmarkMachineStructure;

import gregtech.api.metatileentity.implementations.MTEEnhancedMultiBlockBase;

/** During a bookmark measurement, record construction pieces before any world or hologram access. */
@Mixin(value = MTEEnhancedMultiBlockBase.class, remap = false)
public abstract class BookmarkStructureCaptureMixin {

    @Inject(method = "buildPiece", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$capture(String piece, ItemStack trigger, boolean hints, int x, int y, int z,
        CallbackInfoReturnable<Boolean> cir) {
        if (BookmarkMachineStructure.capture((MTEEnhancedMultiBlockBase<?>) (Object) this, piece, x, y, z))
            cir.setReturnValue(true);
    }
}
