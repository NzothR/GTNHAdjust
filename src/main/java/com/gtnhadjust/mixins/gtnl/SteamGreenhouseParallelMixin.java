package com.gtnhadjust.mixins.gtnl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.GTNHAdjust;
import com.science.gtnl.common.machine.multiMachineBase.SteamMultiMachineBase;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.check.CheckRecipeResult;

@Mixin(targets = "com.science.gtnl.common.machine.multiblock.module.steamElevator.SteamGreenhouseModule", remap = false)
public abstract class SteamGreenhouseParallelMixin {

    @Shadow
    public int maxSeedCount;

    @Shadow
    public abstract int getMachineMode();

    @Shadow
    public abstract int getMaxProgressTime();

    @Shadow
    public abstract void setMaxProgressTime(int time);

    @Shadow
    public abstract void setLEUt(long value);

    @Shadow
    public abstract long getIndustrialFarmEUt();

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

    @Inject(method = "supportsSteamOC", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtnhadjust$enableOverclock(CallbackInfoReturnable<Boolean> result) {
        result.setReturnValue(true);
    }

    @Inject(method = "checkProcessing", at = @At("RETURN"), remap = false)
    private void gtnhadjust$applyOverclock(CallbackInfoReturnable<CheckRecipeResult> result) {
        if (!result.getReturnValue()
            .wasSuccessful() || getMachineMode() != 1) return;
        int count = Math.max(0, Math.min(4, ((SteamMultiMachineBase<?>) (Object) this).recipeOcCount));
        setMaxProgressTime(Math.max(1, getMaxProgressTime() / (1 << count)));
        long steamPerTick = getIndustrialFarmEUt() * (1L << (2 * count));
        setLEUt(-steamPerTick);
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "Steam greenhouse overclocks {}, duration {} ticks, steam/t {}",
            count,
            getMaxProgressTime(),
            steamPerTick);
    }
}
