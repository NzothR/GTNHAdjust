package com.gtnhadjust.mixins.fix;

import net.minecraft.inventory.IInventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.diagnostic.MatrixPatternRecovery;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/** Retry after the machine's first second, when dependent chunks have had time to load. */
@Mixin(targets = "com.xyp.gtneverreset.common.machine.multiblock.AssemblerMatrix", remap = false)
public abstract class MatrixPatternRetryMixin {

    @Shadow
    public abstract void rebuildPatterns();

    @Shadow
    public abstract IInventory getPatterns();

    @Inject(method = "onPostTick", at = @At("RETURN"), remap = false)
    private void gtnhadjust$retry(IGregTechTileEntity tile, long tick, CallbackInfo ci) {
        if (tick == 20 && tile.isServerSide()) {
            if (MatrixPatternRecovery.recover(getPatterns(), tile.getWorld()) > 0) tile.markDirty();
            rebuildPatterns();
        }
    }
}
