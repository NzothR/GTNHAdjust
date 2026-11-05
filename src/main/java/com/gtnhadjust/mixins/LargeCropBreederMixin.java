package com.gtnhadjust.mixins;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.common.machine.multiblock.crop.ArchivedSeedStats;

@Mixin(targets = "com.xyp.gtneverreset.common.machine.multiblock.LargeCropBreeder", remap = false)
public class LargeCropBreederMixin {

    @ModifyConstant(
        method = "checkProcessing",
        constant = @Constant(longValue = 512L),
        require = 3,
        allow = 3,
        remap = false)
    private long gtnhadjust$breedingEnergy(long original) {
        return 8L;
    }

    @ModifyConstant(method = "getMaxParallelRecipes", constant = @Constant(intValue = 3), require = 1, remap = false)
    private int gtnhadjust$parallelBase(int original) {
        return 0;
    }

    @Redirect(
        method = "checkProcessing",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"),
        require = 1,
        remap = false)
    private Object gtnhadjust$outputStats(Map<?, ?> archive, Object crop) {
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info("Crop breeder producing max-stat seed: {}", crop);
        return new ArchivedSeedStats((byte) 31, (byte) 31, (byte) 31, true);
    }

    @Redirect(
        method = "collectSamples",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
        require = 1,
        remap = false)
    private Object gtnhadjust$archiveStats(Map<Object, Object> archive, Object crop, Object stats) {
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info("Crop breeder archiving max-stat seed: {}", crop);
        return archive.put(crop, new ArchivedSeedStats((byte) 31, (byte) 31, (byte) 31, true));
    }

    @Inject(method = "getTooltipPath", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtnhadjust$tooltip(CallbackInfoReturnable<String> result) {
        result.setReturnValue("gtnhadjust-large-crop-breeder");
    }
}
