package com.gtnhadjust.mixins.gtnr;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.compat.GtnrToolPatterns;
import com.xyp.gtneverreset.common.me.CircuitPatternMolds;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import gregtech.api.util.GTRecipe;

/** Identify NCs from the same complete recipe even when non-catalog catalysts appear alongside catalog molds. */
@Mixin(value = CircuitPatternMolds.class, remap = false)
public abstract class PatternToolResolverMixin {

    @Inject(method = "resolve", at = @At("HEAD"), cancellable = true, remap = false)
    private static void gtnhadjust$resolve(ICraftingPatternDetails pattern,
        CallbackInfoReturnable<ItemStack[]> result) {
        GTRecipe recipe = GtnrToolPatterns.recipe(pattern);
        if (recipe != null) result.setReturnValue(GtnrToolPatterns.catalogTools(recipe, pattern));
    }
}
