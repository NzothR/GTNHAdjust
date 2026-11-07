package com.gtnhadjust.mixins.fix;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Use the same looting level for every crusher kill, including ritual and batch modes. */
@Mixin(targets = "kubatech.loaders.MobHandlerLoader$MobEECRecipe", remap = false)
public abstract class IndustrialCrusherLootingMixin {

    @ModifyVariable(method = "generateOutputs", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private int gtnhadjust$fixedLooting(int original) {
        return 10;
    }
}
