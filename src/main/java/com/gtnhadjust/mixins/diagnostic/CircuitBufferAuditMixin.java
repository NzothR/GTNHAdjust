package com.gtnhadjust.mixins.diagnostic;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;

@Mixin(targets = "com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer", remap = false)
public abstract class CircuitBufferAuditMixin {

    @Shadow
    private boolean craftingSlotsDirty;
    @Shadow
    private Map<ICraftingPatternDetails, ?> craftingSlots;
    @Unique
    private boolean gtnhadjust$dirtyBefore;

    @Inject(method = "provideCrafting", at = @At("HEAD"), remap = false)
    private void gtnhadjust$before(ICraftingProviderHelper helper, CallbackInfo ci) {
        gtnhadjust$dirtyBefore = craftingSlotsDirty;
    }

    @Inject(method = "provideCrafting", at = @At("RETURN"), remap = false)
    private void gtnhadjust$after(ICraftingProviderHelper helper, CallbackInfo ci) {
        if (gtnhadjust$dirtyBefore) GTNHAdjust.LOG.info(
            "[craft-audit] circuit buffer={} dirty-before=true dirty-after={} patterns={}",
            System.identityHashCode(this),
            craftingSlotsDirty,
            craftingSlots.size());
    }
}
