package com.gtnhadjust.terminal;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import com.xyp.gtneverreset.client.gui.GuiQuickEncodingTerminal;

import codechicken.nei.recipe.GuiRecipe;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientNotice {

    private static final long DISPLAY_MILLIS = 5000;
    private static String text;
    private static long expiresAt;

    private ClientNotice() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ClientNotice());
    }

    public static void show(String message) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.func_152344_a(() -> {
            text = TerminalNotifications.format(message);
            expiresAt = System.currentTimeMillis() + DISPLAY_MILLIS;
            if (!isTerminalScreen(minecraft.currentScreen) && minecraft.ingameGUI != null)
                minecraft.ingameGUI.func_110326_a(text, false);
        });
    }

    private static boolean isTerminalScreen(GuiScreen screen) {
        return screen instanceof GuiQuickEncodingTerminal
            || screen instanceof GuiRecipe<?> && ((GuiRecipe<?>) screen).firstGui instanceof GuiQuickEncodingTerminal;
    }

    @SubscribeEvent
    public void draw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (text == null || System.currentTimeMillis() >= expiresAt || !isTerminalScreen(event.gui)) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        int maxWidth = Math.max(1, event.gui.width - 20);
        @SuppressWarnings("unchecked")
        List<String> lines = minecraft.fontRenderer.listFormattedStringToWidth(text, maxWidth - 8);
        int width = 0;
        for (String line : lines) width = Math.max(width, minecraft.fontRenderer.getStringWidth(line));
        int left = (event.gui.width - width) / 2;
        int top = Math.max(8, event.gui.height * 85 / 100 - lines.size() * 10);
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glTranslatef(0, 0, 500);
            Gui.drawRect(left - 4, top - 3, left + width + 4, top + lines.size() * 10 + 2, 0xD0101010);
            for (String line : lines) {
                minecraft.fontRenderer.drawStringWithShadow(line, left, top, 0xFFFFFF);
                top += 10;
            }
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
