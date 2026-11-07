package com.gtnhadjust.mixins.terminal;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiOverlayButton.ItemOverlayState;

@Mixin(value = GuiOverlayButton.class, remap = false)
public interface NeiOverlayButtonAccessor {

    @Invoker("ingredientsOverlay")
    List<ItemOverlayState> gtnhadjust$ingredientsOverlay();
}
