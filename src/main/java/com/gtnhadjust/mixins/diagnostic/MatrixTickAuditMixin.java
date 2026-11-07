package com.gtnhadjust.mixins.diagnostic;

import net.minecraft.inventory.IInventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.diagnostic.MatrixPatternAudit;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

@Mixin(targets = "com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix", remap = false)
public abstract class MatrixTickAuditMixin {

    @Shadow
    public abstract IInventory getPatterns();

    @Inject(method = "onPostTick", at = @At("RETURN"), remap = false)
    private void gtnhadjust$inspect(IGregTechTileEntity tile, long tick, CallbackInfo ci) {
        if (tick == 20 && tile.isServerSide()) MatrixPatternAudit
            .inspect(tile.getWorld(), getPatterns(), tile.getXCoord(), tile.getYCoord(), tile.getZCoord());
    }
}
