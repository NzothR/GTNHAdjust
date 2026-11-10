package com.gtnhadjust.mixins.quest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.quest.QuestCraftSlots;

import betterquesting.api2.client.gui.controls.PanelButton;

@Mixin(value = PanelButton.class, remap = false)
public abstract class QuestItemClickMixin {

    @Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$craft(int x, int y, int button, CallbackInfoReturnable<Boolean> ci) {
        if (QuestCraftSlots.click(this, x, y, button)) ci.setReturnValue(true);
    }
}
