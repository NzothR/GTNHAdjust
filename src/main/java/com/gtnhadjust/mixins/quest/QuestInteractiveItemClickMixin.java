package com.gtnhadjust.mixins.quest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.quest.QuestCraftSlots;

import bq_standard.client.gui.panels.content.PanelInteractiveItemSlot;

@Mixin(value = PanelInteractiveItemSlot.class, remap = false)
public abstract class QuestInteractiveItemClickMixin {

    @Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$craft(int x, int y, int button, CallbackInfoReturnable<Boolean> ci) {
        if (QuestCraftSlots.click(this, x, y, button)) ci.setReturnValue(true);
    }
}
