package com.gtnhadjust.quest;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.lwjgl.opengl.GL11;

import appeng.api.AEApi;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.panels.content.PanelFluidSlot;
import betterquesting.api2.client.gui.panels.content.PanelItemSlot;
import bq_standard.tasks.TaskRetrieval;
import codechicken.nei.guihook.GuiContainerManager;

public final class QuestCraftSlots {

    private static final Map<Object, Selection> SLOTS = new WeakHashMap<>();
    private static NBTTagCompound snapshot = new NBTTagCompound();
    private static UUID current;
    private static long nonce;

    private QuestCraftSlots() {}

    public static void open(UUID quest) {
        current = quest;
        nonce++;
        snapshot = new NBTTagCompound();
        QuestAeNetwork.requestSnapshot(quest, nonce);
    }

    public static void acceptSnapshot(NBTTagCompound data) {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (!(screen instanceof QuestScreenAccess) || current == null || data.getLong("nonce") != nonce) return;
        UUID id = new UUID(data.getLong("most"), data.getLong("least"));
        if (!current.equals(id) || !id.equals(((QuestScreenAccess) screen).gtnhadjust$questId())) return;
        snapshot = data;
    }

    public static void register(PanelItemSlot[] slots, TaskRetrieval task) {
        for (int i = 0; i < slots.length; i++) if (slots[i] != null) SLOTS.put(slots[i], new Selection(task, i));
    }

    public static void registerFluid(PanelFluidSlot slot, ITask task, int index) {
        SLOTS.put(slot, new Selection(task, index));
    }

    public static void draw(Object slot) {
        Selection selection = SLOTS.get(slot);
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (selection == null || !(screen instanceof QuestScreenAccess) || !(slot instanceof PanelButton)) return;
        QuestScreenAccess context = (QuestScreenAccess) screen;
        if (context.gtnhadjust$quest() == null || !context.gtnhadjust$questId()
            .equals(current)) return;
        int id = context.gtnhadjust$quest()
            .getTasks()
            .getID(selection.task);
        int[] flags = snapshot.getIntArray(Integer.toString(id));
        if (selection.index < 0 || selection.index >= flags.length || flags[selection.index] == 0) return;
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .orNull();
        if (pattern == null) return;
        PanelButton panel = (PanelButton) slot;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(
                panel.getTransform()
                    .getX() + 1,
                panel.getTransform()
                    .getY() + 1,
                200);
            GL11.glScalef(0.4F, 0.4F, 1);
            GuiContainerManager.drawItem(0, 0, pattern);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static final class Selection {

        final ITask task;
        final int index;

        Selection(ITask task, int index) {
            this.task = task;
            this.index = index;
        }
    }
}
