package com.futureresourceprotocol.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import com.futureresourceprotocol.FutureResourceProtocol;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.GTMod;

@SideOnly(Side.CLIENT)
public final class ClientHullLocks {

    private static int mask;
    private static boolean remoteApplied;

    private ClientHullLocks() {}

    public static void register() {
        ClientHullLocks handler = new ClientHullLocks();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
    }

    public static void receive(HullLockMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.func_152344_a(() -> {
            mask = message.enabled ? message.mask : 0;
            if (!mc.isIntegratedServerRunning()) {
                HullRecipeLocks.applyMask(mask);
                remoteApplied = true;
            }
            GTMod.proxy.reloadNEICache();
            FutureResourceProtocol.debug(
                "client hull snapshot enabled={} mask={} integrated={}",
                message.enabled,
                mask,
                mc.isIntegratedServerRunning());
        });
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        int tier = HullRecipeLocks.tier(event.itemStack);
        if ((tier & mask) == 0) return;
        event.toolTip.add(EnumChatFormatting.RED + I18n.format("frp.tooltip.hull_debt_locked"));
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                mask = 0;
                if (remoteApplied) {
                    HullRecipeLocks.restore();
                    remoteApplied = false;
                    GTMod.proxy.reloadNEICache();
                    FutureResourceProtocol.debug("restored client hull recipes on disconnect");
                }
            });
    }
}
