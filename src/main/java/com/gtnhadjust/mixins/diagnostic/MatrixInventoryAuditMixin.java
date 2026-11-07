package com.gtnhadjust.mixins.diagnostic;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.diagnostic.MatrixPatternAudit;

@Mixin(targets = "com.xyp.gtneverreset.common.me.AssemblerMatrixPatternInventory", remap = false)
public abstract class MatrixInventoryAuditMixin {

    @Inject(method = "loadNBTData", at = @At("HEAD"), remap = false)
    private void gtnhadjust$begin(NBTTagCompound data, CallbackInfo ci) {
        MatrixPatternAudit.beginLoad();
    }

    @Inject(method = "loadNBTData", at = @At("RETURN"), remap = false)
    private void gtnhadjust$end(NBTTagCompound data, CallbackInfo ci) {
        MatrixPatternAudit.endLoad();
    }
}
