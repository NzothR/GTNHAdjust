package com.gtnhadjust.mixins.fix;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gtnhadjust.terminal.RecipePresenceRefresh;

import codechicken.nei.Widget;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.widget.RecipeWidget;
import codechicken.nei.scroll.ScrollContainer;

/** NEI caches ingredient presence until its buttons are explicitly invalidated. */
@Mixin(value = GuiRecipe.class, remap = false)
public abstract class QuickTerminalRecipeScreenMixin implements RecipePresenceRefresh {

    @Shadow
    private ScrollContainer container;

    @Override
    public void gtnhadjust$invalidatePresence() {
        for (Widget widget : container.getWidgets()) {
            if (widget instanceof RecipeWidget recipe) {
                for (GuiRecipeButton button : recipe.getRecipeButtons()) button.onPermutationsChanged();
            }
        }
    }
}
