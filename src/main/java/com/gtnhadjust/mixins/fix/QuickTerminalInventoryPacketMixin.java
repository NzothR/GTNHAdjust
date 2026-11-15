package com.gtnhadjust.mixins.fix;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.terminal.RecipePresenceRefresh;
import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;

import appeng.api.storage.data.IAEStack;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketMEInventoryUpdate;
import codechicken.nei.recipe.GuiRecipe;

/** AE2 normally discards storage updates while NEI's recipe screen covers the terminal. */
@Mixin(value = PacketMEInventoryUpdate.class, remap = false)
public abstract class QuickTerminalInventoryPacketMixin {

    @Shadow
    @Final
    private List<IAEStack<?>> list;

    @Inject(method = "clientPacketData", at = @At("TAIL"), remap = false)
    private void gtnhadjust$forwardToCoveredTerminal(INetworkInfo network, AppEngPacket packet, EntityPlayer player,
        CallbackInfo ci) {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (!(screen instanceof GuiRecipe<?>recipe) || !(recipe.firstGui instanceof QuickEncodingTerminalGui terminal)
            || player == null
            || list == null) return;
        terminal.postUpdate(list);
        ((RecipePresenceRefresh) recipe).gtnhadjust$invalidatePresence();
        if (GTNHAdjust.terminalDebug)
            GTNHAdjust.LOG.info("Forwarded {} ME updates to terminal behind NEI", list.size());
    }
}
