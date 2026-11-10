package com.gtnhadjust.mixins.quest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.quest.QuestCraftSlots;

import betterquesting.api2.client.gui.panels.IGuiCanvas;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.client.gui.panels.content.PanelFluidSlot;
import bq_standard.client.gui.tasks.PanelTaskFluid;
import bq_standard.tasks.TaskFluid;

@Mixin(value = PanelTaskFluid.class, remap = false)
public abstract class QuestFluidPanelMixin {

    @Shadow
    private TaskFluid task;

    @Inject(method = "initPanel", at = @At("RETURN"))
    private void gtnhadjust$register(CallbackInfo ci) {
        int index = 0;
        for (IGuiPanel panel : ((IGuiCanvas) this).getChildren()) {
            if (panel instanceof PanelFluidSlot) {
                while (index < task.requiredFluids.size() && task.requiredFluids.get(index) == null) index++;
                QuestCraftSlots.registerFluid((PanelFluidSlot) panel, task, index++);
            }
        }
    }
}
