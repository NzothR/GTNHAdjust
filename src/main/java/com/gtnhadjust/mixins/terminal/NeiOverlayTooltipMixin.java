package com.gtnhadjust.mixins.terminal;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.terminal.OverlayIngredientTooltip;
import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.ItemsTooltipLineHandler;
import codechicken.nei.recipe.GuiOverlayButton;

@Mixin(value = GuiOverlayButton.class, remap = false)
public abstract class NeiOverlayTooltipMixin {

    @Shadow
    protected ItemsTooltipLineHandler missedMaterialsTooltipLineHandler;

    @Inject(method = "handleTooltip", at = @At("RETURN"))
    private void gtnhadjust$groupIngredients(List<String> current, CallbackInfoReturnable<List<String>> cir) {
        GuiOverlayButton button = (GuiOverlayButton) (Object) this;
        if (!(button.firstGui instanceof QuickEncodingTerminalGui terminal)) return;
        List<String> tooltip = cir.getReturnValue();
        tooltip.removeIf(
            line -> line.startsWith(GuiDraw.TOOLTIP_HANDLER)
                && GuiDraw.getTipLine(line) == missedMaterialsTooltipLineHandler);
        OverlayIngredientTooltip.append(button, terminal, tooltip);
    }
}
