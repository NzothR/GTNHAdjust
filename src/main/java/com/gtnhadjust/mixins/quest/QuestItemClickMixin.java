package com.gtnhadjust.mixins.quest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.quest.QuestCraftSlots;

import betterquesting.api2.client.gui.controls.PanelButton;

@Mixin(value = PanelButton.class, remap = false)
public abstract class QuestItemClickMixin {

    @Inject(method = "drawPanel", at = @At("RETURN"))
    private void gtnhadjust$draw(int x, int y, float tick, CallbackInfo ci) {
        QuestCraftSlots.draw(this);
    }
}
