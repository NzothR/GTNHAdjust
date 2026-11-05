package com.gtnhadjust.mixins.terminal;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.terminal.BookmarkIngredients;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

@Mixin(targets = "com.xyp.gtneverreset.compat.nei.QuickTerminalRecipeTransferHandler", remap = false)
public abstract class QuickTerminalRecipeMixin {

    @Inject(method = "overlayRecipe", at = @At("HEAD"), remap = false)
    private void gtnhadjust$begin(GuiContainer gui, IRecipeHandler recipe, int index, boolean shift, CallbackInfo ci) {
        BookmarkIngredients.begin(true);
    }

    @Inject(method = "overlayRecipe", at = @At("RETURN"), remap = false)
    private void gtnhadjust$end(GuiContainer gui, IRecipeHandler recipe, int index, boolean shift, CallbackInfo ci) {
        BookmarkIngredients.end();
    }

    @Redirect(
        method = "overlayRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lcom/xyp/gtneverreset/compat/nei/QuickTerminalRecipeTransferHandler;hasInterchangeableInputs(Lcodechicken/nei/recipe/IRecipeHandler;IZ)Z"),
        remap = false)
    private boolean gtnhadjust$allowAlternatives(IRecipeHandler recipe, int index, boolean crafting) {
        return false;
    }

    @Redirect(
        method = "overlayRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/recipe/IRecipeHandler;getIngredientStacks(I)Ljava/util/List;"),
        remap = false)
    private List<PositionedStack> gtnhadjust$overlayIngredients(IRecipeHandler recipe, int index) {
        return BookmarkIngredients.get(recipe, index);
    }

    @Redirect(
        method = "collectCraftingInputs",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/recipe/IRecipeHandler;getIngredientStacks(I)Ljava/util/List;"),
        remap = false)
    private static List<PositionedStack> gtnhadjust$ingredients(IRecipeHandler recipe, int index) {
        return BookmarkIngredients.get(recipe, index);
    }
}
