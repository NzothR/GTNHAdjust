package com.gtnhadjust.mixins.quest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.quest.QuestAeNetwork;

import betterquesting.api.questing.IQuest;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.api2.utils.Tuple2;
import bq_standard.tasks.QuestAeDetector;
import bq_standard.tasks.TaskRetrieval;
import bq_standard.tasks.base.TaskProgressableBase;

@Mixin(value = TaskRetrieval.class, remap = false)
public abstract class TaskRetrievalMixin extends TaskProgressableBase<int[]> {

    @Shadow
    private void checkAndComplete(ParticipantInfo info, Map.Entry<UUID, IQuest> quest, boolean updated,
        List<Tuple2<UUID, int[]>> progress) {}

    @Inject(method = "detect", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$detect(ParticipantInfo info, Map.Entry<UUID, IQuest> quest, CallbackInfo ci) {
        if (QuestAeNetwork.current() != info.PLAYER) return;
        TaskRetrieval task = (TaskRetrieval) (Object) this;
        if (task.isComplete(info.UUID)) {
            ci.cancel();
            return;
        }
        QuestAeDetector.Result result = QuestAeDetector.detect(task, info);
        if (result.updated) setBulkProgress(result.progress);
        checkAndComplete(info, quest, result.updated, result.progress);
        ci.cancel();
    }
}
