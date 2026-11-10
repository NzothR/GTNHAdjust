package com.gtnhadjust.quest;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;

import betterquesting.api2.client.gui.panels.content.PanelItemSlot;
import bq_standard.tasks.TaskRetrieval;

public final class QuestCraftSlots {

    private static final Map<PanelItemSlot, Selection> SLOTS = new WeakHashMap<>();

    private QuestCraftSlots() {}

    public static void register(PanelItemSlot[] slots, TaskRetrieval task) {
        for (int i = 0; i < slots.length; i++) if (slots[i] != null) SLOTS.put(slots[i], new Selection(task, i));
    }

    public static boolean click(Object button, int x, int y, int mouse) {
        if (mouse != 2 || !(Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU))
            || !(button instanceof PanelItemSlot)) return false;
        PanelItemSlot slot = (PanelItemSlot) button;
        Selection selection = SLOTS.get(slot);
        if (selection == null || !slot.isActive()
            || !slot.getTransform()
                .contains(x, y))
            return false;
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (!(screen instanceof QuestScreenAccess)) return false;
        QuestScreenAccess quest = (QuestScreenAccess) screen;
        if (quest.gtnhadjust$quest() == null || quest.gtnhadjust$questId() == null) return false;
        int taskId = quest.gtnhadjust$quest()
            .getTasks()
            .getID(selection.task);
        if (taskId < 0) return false;
        QuestAeNetwork.craft(quest.gtnhadjust$questId(), taskId, selection.index);
        return true;
    }

    private static final class Selection {

        final TaskRetrieval task;
        final int index;

        Selection(TaskRetrieval task, int index) {
            this.task = task;
            this.index = index;
        }
    }
}
