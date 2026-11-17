package com.gtnhadjust.mixins.gtnr;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

@Mixin(value = SuperMTEHatchCraftingInputME.PatternSlot.class, remap = false)
public interface PatternHostAccessor {

    @Accessor("parentMTE")
    IMetaTileEntity gtnhadjust$getPatternHost();
}
