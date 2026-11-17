package com.gtnhadjust.mixins.gtnr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.compat.GtnrToolNetwork;
import com.gtnhadjust.compat.GtnrToolPatterns;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import gregtech.api.enums.GTValues;
import gregtech.api.util.GTUtility;

/** External NCs are visible to GT only while backed by a physical/shared tool or the same AE network. */
@Mixin(targets = "com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer$CircuitSlot", remap = false)
public abstract class RequiredToolsMixin {

    @Shadow
    public abstract ICraftingPatternDetails getPatternDetails();

    @Shadow
    public abstract FluidStack[] getFluidInputs();

    @Inject(method = "getItemInputs", at = @At("RETURN"), cancellable = true, remap = false)
    private void gtnhadjust$supply(CallbackInfoReturnable<ItemStack[]> result) {
        ICraftingPatternDetails pattern = getPatternDetails();
        if (pattern == null || !GtnrToolPatterns.hasRecord(pattern.getPattern())) return;
        // Preserve GTNR's empty-buffer guard; virtual tools alone are not a delivered AE batch.
        if (result.getReturnValue().length == 0 && getFluidInputs().length == 0) return;
        SuperMTEHatchCraftingInputME host = (SuperMTEHatchCraftingInputME) ((PatternHostAccessor) this)
            .gtnhadjust$getPatternHost();
        List<ItemStack> inputs = new ArrayList<>(Arrays.asList(result.getReturnValue()));
        for (ItemStack tool : GtnrToolPatterns.recorded(pattern.getPattern())) {
            if (GtnrToolPatterns.builtIn(tool)) continue;
            if (inputs.stream()
                .anyMatch(item -> GTUtility.areStacksEqual(tool, item, false))) continue;
            if (!GtnrToolNetwork.available(
                tool,
                host.getSharedItems(),
                host.getProxy(),
                host.getBaseMetaTileEntity()
                    .getTimer())) {
                result.setReturnValue(GTValues.emptyItemStackArray);
                return;
            }
            inputs.add(GTUtility.copyAmount(0, tool));
        }
        result.setReturnValue(inputs.toArray(new ItemStack[0]));
    }
}
