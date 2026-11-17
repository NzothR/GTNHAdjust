package com.gtnhadjust.mixins.gtnr;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.compat.GtnrToolPatterns;
import com.xyp.gtneverreset.common.machine.multiMachineBase.processing.RecipeThreadProcessingLogic;
import com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactoryProcessingLogic;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import gregtech.api.util.GTRecipe;

/** Apply complete new-format NC identity checks to GTNR's own cross-recipe factory as well. */
@Mixin(value = UniversalJointFactoryProcessingLogic.class, remap = false)
public abstract class JointFactoryToolsMixin extends RecipeThreadProcessingLogic {

    @Shadow
    private ICraftingPatternDetails pattern;

    @Inject(method = "consumesPatternInputs", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtnhadjust$checkTools(GTRecipe recipe, CallbackInfoReturnable<Boolean> result) {
        if (pattern != null && !GtnrToolPatterns.supplied(recipe, pattern, inputItems)) result.setReturnValue(false);
    }
}
