package com.gtnhadjust.mixins.terminal;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.terminal.BookmarkIngredients;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

@Mixin(targets = "com.xyp.gtneverreset.compat.nei.TerminalRecipeIngredients", remap = false)
public abstract class TerminalIngredientsMixin {

    @Inject(method = "outputs", at = @At("RETURN"), cancellable = true, remap = false)
    private static void gtnhadjust$outputs(IRecipeHandler recipe, int index, boolean includeOther,
        CallbackInfoReturnable<List<?>> cir) {
        cir.setReturnValue(BookmarkIngredients.outputs(recipe, index, cir.getReturnValue()));
    }

    @Redirect(
        method = "inputs",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/recipe/IRecipeHandler;getIngredientStacks(I)Ljava/util/List;"),
        remap = false)
    private static List<PositionedStack> gtnhadjust$ingredients(IRecipeHandler recipe, int index) {
        return BookmarkIngredients.get(recipe, index);
    }
}
