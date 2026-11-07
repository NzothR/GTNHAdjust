package com.gtnhadjust.mixins.terminal;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.terminal.SmartIngredients;

import codechicken.nei.recipe.IRecipeHandler;

@Mixin(targets = "com.xyp.gtneverreset.compat.nei.QuickTerminalRecipeTransferHandler", remap = false)
public abstract class SmartIngredientTransferMixin {

    @ModifyVariable(method = "overlayRecipe", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private int gtnhadjust$selectRecipe(int index, GuiContainer gui, IRecipeHandler recipe, int originalIndex,
        boolean shift) {
        return SmartIngredients.begin(gui, recipe, index);
    }

    @Inject(method = "overlayRecipe", at = @At("RETURN"), remap = false)
    private void gtnhadjust$finish(GuiContainer gui, IRecipeHandler recipe, int index, boolean shift, CallbackInfo ci) {
        SmartIngredients.end();
    }
}
