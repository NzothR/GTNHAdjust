package com.gtnhadjust.mixins.animation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.animation.YsmuAnimatedArm;

@Mixin(targets = "com.xyp.gtnrlib.client.render.animation.FirstPersonArmRenderer", remap = false)
public abstract class GtnrAnimatedArmMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void gtnhadjust$renderModelArm(boolean left, float shoulderRoll, CallbackInfo ci) {
        if (YsmuAnimatedArm.render(left, shoulderRoll)) ci.cancel();
    }
}
