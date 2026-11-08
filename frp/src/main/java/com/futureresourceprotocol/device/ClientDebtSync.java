package com.futureresourceprotocol.device;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientDebtSync {

    private ClientDebtSync() {}

    public static void receive(DebtSyncMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.func_152344_a(() -> {
            if (mc.currentScreen instanceof TerminalGui) {
                TerminalGui gui = (TerminalGui) mc.currentScreen;
                if (gui.inventorySlots.windowId == message.windowId) gui.receiveDebts(message);
            }
        });
    }

    public static void receiveMarks(MarkSyncMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.func_152344_a(() -> {
            if (mc.currentScreen instanceof TerminalGui) {
                TerminalGui gui = (TerminalGui) mc.currentScreen;
                if (gui.inventorySlots.windowId == message.windowId) gui.receiveMarks(message);
            }
        });
    }
}
