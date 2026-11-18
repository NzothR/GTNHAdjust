package com.gtnhadjust.mixins.goodgenerator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.gtnhadjust.GTNHAdjust;

import goodgenerator.blocks.tileEntity.AntimatterGenerator;

@Mixin(value = AntimatterGenerator.class, remap = false)
public abstract class AntimatterCycleMixin {

    /** Set how often the matrix checks for an annihilation cycle. */
    @ModifyConstant(method = "shouldCheckRecipeThisTick", constant = @Constant(longValue = 100L))
    private long gtnhadjust$cycleInterval(long originalTicks) {
        return GTNHAdjust.antimatterCycleTicks;
    }
}
