package com.gtnhadjust.terminal;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientNotice {

    private ClientNotice() {}

    public static void show(String message) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.func_152344_a(
            () -> {
                if (minecraft.ingameGUI != null) minecraft.ingameGUI.func_110326_a("§b[样板终端] §r" + message, false);
            });
    }
}
