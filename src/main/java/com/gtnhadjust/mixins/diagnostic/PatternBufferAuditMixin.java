package com.gtnhadjust.mixins.diagnostic;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.GTNHAdjust;

import appeng.api.networking.crafting.ICraftingProviderHelper;

@Mixin(targets = "com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME", remap = false)
public abstract class PatternBufferAuditMixin {

    @Shadow
    private boolean needPatternSync;

    @Inject(method = "onPatternChange", at = @At("RETURN"), remap = false)
    private void gtnhadjust$changed(int slot, ItemStack pattern, CallbackInfo ci) {
        GTNHAdjust.LOG.info(
            "[craft-audit] buffer={} pattern slot={} value={} pending={}",
            System.identityHashCode(this),
            slot,
            pattern,
            needPatternSync);
    }

    @Inject(method = "postMEPatternChange", at = @At("RETURN"), remap = false)
    private void gtnhadjust$post(CallbackInfoReturnable<Boolean> cir) {
        GTNHAdjust.LOG.info("[craft-audit] buffer={} posted={}", System.identityHashCode(this), cir.getReturnValue());
    }

    @Inject(method = "provideCrafting", at = @At("HEAD"), remap = false)
    private void gtnhadjust$provide(ICraftingProviderHelper helper, CallbackInfo ci) {
        GTNHAdjust.LOG
            .info("[craft-audit] buffer={} provideCrafting pending={}", System.identityHashCode(this), needPatternSync);
    }
}
