package com.futureresourceprotocol.recipe;

import net.minecraft.entity.player.EntityPlayerMP;

import com.futureresourceprotocol.FutureResourceProtocol;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public final class HullLockEvents {

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP) || !FutureResourceProtocol.enabled) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        boolean enabled = FutureResourceProtocol.hullRecipeLocksEnabled;
        int mask = enabled ? HullRecipeLocks.serverMask() : 0;
        FutureResourceProtocol.NETWORK.sendTo(new HullLockMessage(enabled, mask), player);
        FutureResourceProtocol
            .debug("hull snapshot sent player={} enabled={} mask={}", player.getCommandSenderName(), enabled, mask);
    }
}
