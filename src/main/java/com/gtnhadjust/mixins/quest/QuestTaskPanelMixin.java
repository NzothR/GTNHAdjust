package com.gtnhadjust.mixins.quest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.quest.QuestCraftSlots;

import betterquesting.api2.client.gui.panels.content.PanelItemSlot;
import bq_standard.client.gui.tasks.PanelTaskItemBase;
import bq_standard.tasks.TaskRetrieval;
import bq_standard.tasks.base.TaskProgressableBase;

@Mixin(value = PanelTaskItemBase.class, remap = false)
public abstract class QuestTaskPanelMixin {

    @Shadow
    private PanelItemSlot[] itemSlots;
    @Shadow
    protected TaskProgressableBase<int[]> task;

    @Inject(method = "initPanel", at = @At("RETURN"))
    private void gtnhadjust$register(CallbackInfo ci) {
        if (task instanceof TaskRetrieval) QuestCraftSlots.register(itemSlots, (TaskRetrieval) task);
    }
}
