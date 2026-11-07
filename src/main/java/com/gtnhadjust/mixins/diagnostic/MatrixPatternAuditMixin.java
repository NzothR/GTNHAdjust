package com.gtnhadjust.mixins.diagnostic;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.diagnostic.MatrixPatternAudit;
import com.xyp.gtneverreset.common.me.DireCraftingPatternDetails;

@Mixin(targets = "com.xyp.gtneverreset.common.me.AssemblerMatrixPatternState", remap = false)
public abstract class MatrixPatternAuditMixin {

    @Inject(method = "decodePattern", at = @At("RETURN"), remap = false)
    private void gtnhadjust$audit(ItemStack stack, World world,
        CallbackInfoReturnable<DireCraftingPatternDetails> result) {
        MatrixPatternAudit.observe(stack, world, result.getReturnValue());
    }
}
