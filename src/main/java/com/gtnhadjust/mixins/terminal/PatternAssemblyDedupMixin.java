package com.gtnhadjust.mixins.terminal;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.terminal.PatternDuplicates;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;

import gregtech.api.metatileentity.CommonMetaTileEntity;

/** Both the local GUI and AE's interface inventory ask the MTE whether a slot accepts an item. */
@Mixin(value = CommonMetaTileEntity.class, remap = false)
public abstract class PatternAssemblyDedupMixin {

    @Dynamic("The target uses the SRG name at runtime and the MCP name in development")
    @Inject(
        method = { "isItemValidForSlot", "func_94041_b" },
        at = @At("RETURN"),
        cancellable = true,
        require = 1,
        remap = false)
    private void gtnhadjust$rejectDuplicate(int index, ItemStack stack, CallbackInfoReturnable<Boolean> result) {
        if (!result.getReturnValue() || !((Object) this instanceof SuperMTEHatchCraftingInputME)) return;
        SuperMTEHatchCraftingInputME assembly = (SuperMTEHatchCraftingInputME) (Object) this;
        if (index >= 0 && index < assembly.getPatternCount()
            && PatternDuplicates.contains(assembly.getPatterns(), index, stack)) result.setReturnValue(false);
    }
}
