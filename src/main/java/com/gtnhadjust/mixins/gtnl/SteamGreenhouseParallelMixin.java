package com.gtnhadjust.mixins.gtnl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

@Mixin(targets = "com.science.gtnl.common.machine.multiblock.module.steamElevator.SteamGreenhouseModule", remap = false)
public abstract class SteamGreenhouseParallelMixin {

    @Shadow
    public int maxSeedCount;

    @Inject(method = "onFirstTick", at = @At("HEAD"), remap = false)
    private void gtnhadjust$seedCapacity(IGregTechTileEntity tile, CallbackInfo ci) {
        if (!tile.isServerSide()) return;
        maxSeedCount = GTNHAdjust.steamGreenhouseMaxSeeds;
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "Steam greenhouse max seeds {} at {}, {}, {}",
            maxSeedCount,
            tile.getXCoord(),
            tile.getYCoord(),
            tile.getZCoord());
    }

}
