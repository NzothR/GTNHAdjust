package com.gtnhadjust.mixins.nei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;

import codechicken.nei.recipe.TemplateRecipeHandler;
import gregtech.nei.GTNEIDefaultHandler;
import gregtech.nei.NEIGTConfig;

/** Hide only the client GTNR disassembler handler; actual disassembly recipes and machines remain unchanged. */
@Mixin(value = NEIGTConfig.class, remap = false)
public abstract class HideGtnrDisassemblerRecipesMixin {

    @Inject(method = "addHandler", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void gtnhadjust$hideDisassembler(TemplateRecipeHandler handler, CallbackInfo ci) {
        if (handler instanceof GTNEIDefaultHandler gt
            && "gtnr.recipe.disassembler".equals(gt.getRecipeMap().unlocalizedName)) {
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info("[nei-hide] Skipping GTNR disassembler recipe display handler");
            ci.cancel();
        }
    }
}
