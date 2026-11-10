package com.gtnhadjust.mixins.quest;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StatCollector;

import org.lwjgl.util.vector.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.quest.QuestAeNetwork;
import com.gtnhadjust.quest.QuestScreenAccess;

import betterquesting.api.questing.IQuest;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.misc.GuiPadding;
import betterquesting.api2.client.gui.misc.GuiTransform;
import betterquesting.api2.client.gui.panels.CanvasEmpty;

@Mixin(targets = "betterquesting.client.gui2.GuiQuest", remap = false)
public abstract class QuestScreenMixin implements QuestScreenAccess {

    @Shadow
    private UUID questID;
    @Shadow
    private IQuest quest;
    @Shadow
    private CanvasEmpty cvInner;
    @Unique
    private PanelButton gtnhadjust$aeButton;

    @Override
    public UUID gtnhadjust$questId() {
        return questID;
    }

    @Override
    public IQuest gtnhadjust$quest() {
        return quest;
    }

    @Inject(method = "initPanel", at = @At("RETURN"))
    private void gtnhadjust$button(CallbackInfo ci) {
        if (quest == null || cvInner == null) return;
        gtnhadjust$aeButton = new PanelButton(
            new GuiTransform(new Vector4f(0.5F, 1F, 1F, 1F), new GuiPadding(8, -34, 0, 18), 0),
            918215,
            StatCollector.translateToLocal("gtnhadjust.quest.ae_detect"));
        gtnhadjust$aeButton.setClickAction(button -> QuestAeNetwork.request(questID));
        cvInner.addPanel(gtnhadjust$aeButton);
        gtnhadjust$update(null);
    }

    @Inject(method = "updateButtons", at = @At("RETURN"))
    private void gtnhadjust$update(CallbackInfo ci) {
        if (gtnhadjust$aeButton != null && quest != null)
            gtnhadjust$aeButton.setActive(quest.canSubmit(Minecraft.getMinecraft().thePlayer));
    }
}
