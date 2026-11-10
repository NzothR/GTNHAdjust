package com.gtnhadjust.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.util.ChatComponentTranslation;

import appeng.container.implementations.ContainerCraftConfirm;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.storage.DBEntry;
import betterquesting.questing.QuestDatabase;
import bq_standard.tasks.QuestAeDetector;
import bq_standard.tasks.TaskFluid;
import bq_standard.tasks.TaskRetrieval;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public final class QuestCraftQueue {

    private static final Map<UUID, Queue> QUEUES = new HashMap<>();

    public static void start(EntityPlayerMP player, UUID questId, IQuest quest) {
        UUID uuid = player.getUniqueID();
        if (QUEUES.containsKey(uuid)) return;
        List<Candidate> candidates = new ArrayList<>();
        UUID participant = QuestingAPI.getQuestingUUID(player);
        for (DBEntry<ITask> entry : quest.getTasks()
            .getEntries()) {
            ITask task = entry.getValue();
            if (task.ignored(participant) || task.isComplete(participant)) continue;
            int count = task instanceof TaskRetrieval ? ((TaskRetrieval) task).requiredItems.size()
                : task instanceof TaskFluid ? ((TaskFluid) task).requiredFluids.size() : 0;
            for (int i = 0; i < count; i++) candidates.add(new Candidate(task, i));
        }
        if (candidates.isEmpty()) {
            player.addChatMessage(new ChatComponentTranslation("gtnhadjust.quest.craft_queue_done"));
            return;
        }
        QUEUES.put(uuid, new Queue(player, questId, candidates));
    }

    public static void submitted(EntityPlayer player, Container container) {
        Queue queue = QUEUES.get(player.getUniqueID());
        if (queue != null && queue.container == container) queue.submitted = true;
    }

    public static void closed(EntityPlayer player, Container container) {
        Queue queue = QUEUES.get(player.getUniqueID());
        if (queue != null && queue.container == container) {
            if (queue.submitted && queue.active.task instanceof TaskRetrieval
                && ((TaskRetrieval) queue.active.task).requireOnlyOneItem) queue.skip = queue.active.task;
            queue.container = null;
            queue.delay = 2;
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUES.isEmpty()) return;
        for (Queue queue : new ArrayList<>(QUEUES.values())) {
            if (queue.container != null || --queue.delay > 0) continue;
            IQuest quest = QuestDatabase.INSTANCE.get(queue.questId);
            if (quest == null || !quest.canSubmit(queue.player)) {
                QUEUES.remove(queue.player.getUniqueID());
                continue;
            }
            boolean opened = false;
            while (queue.index < queue.candidates.size()) {
                Candidate next = queue.candidates.get(queue.index++);
                if (next.task == queue.skip || next.task.ignored(QuestingAPI.getQuestingUUID(queue.player))
                    || next.task.isComplete(QuestingAPI.getQuestingUUID(queue.player))) continue;
                boolean ok = next.task instanceof TaskRetrieval
                    ? QuestAeDetector.openCraft(queue.player, (TaskRetrieval) next.task, next.index)
                    : QuestAeDetector.openFluidCraft(queue.player, (TaskFluid) next.task, next.index);
                if (ok && queue.player.openContainer instanceof ContainerCraftConfirm) {
                    queue.active = next;
                    queue.submitted = false;
                    queue.container = queue.player.openContainer;
                    opened = true;
                    break;
                }
            }
            if (!opened) {
                QUEUES.remove(queue.player.getUniqueID());
                queue.player.addChatMessage(new ChatComponentTranslation("gtnhadjust.quest.craft_queue_done"));
            }
        }
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        QUEUES.remove(event.player.getUniqueID());
    }

    private static final class Candidate {

        final ITask task;
        final int index;

        Candidate(ITask task, int index) {
            this.task = task;
            this.index = index;
        }
    }

    private static final class Queue {

        final EntityPlayerMP player;
        final UUID questId;
        final List<Candidate> candidates;
        int index;
        int delay = 1;
        ITask skip;
        Candidate active;
        Container container;
        boolean submitted;

        Queue(EntityPlayerMP player, UUID questId, List<Candidate> candidates) {
            this.player = player;
            this.questId = questId;
            this.candidates = candidates;
        }
    }
}
