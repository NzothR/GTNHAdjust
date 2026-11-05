package com.gtnhadjust.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "com.xyp.gtneverreset.common.machine.multiblock.crop.CropBreedingRecipeRules", remap = false)
public class CropBreedingRulesMixin {

    @ModifyConstant(method = "requiredVoltageTier", constant = @Constant(intValue = 3), require = 1, remap = false)
    private static int gtnhadjust$lowerTier(int original) {
        return 0;
    }
}
