package com.gtnhadjust.mixins.diagnostic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketMEInventoryUpdate;
import codechicken.nei.recipe.GuiRecipe;

@Mixin(value = PacketMEInventoryUpdate.class, remap = false)
public abstract class InventoryPacketAuditMixin {

    @Shadow
    @Final
    private List<IAEStack<?>> list;
    @Unique
    private static final Map<IAEStack<?>, Boolean> gtnhadjust$craftStates = new HashMap<>();
    @Unique
    private static GuiScreen gtnhadjust$lastTerminal;

    @Inject(method = "clientPacketData", at = @At("HEAD"), remap = false)
    private void gtnhadjust$packet(INetworkInfo network, AppEngPacket packet, EntityPlayer player, CallbackInfo ci) {
        if (list == null) return;
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        GuiScreen terminal = screen instanceof GuiRecipe<?> ? ((GuiRecipe<?>) screen).firstGui : screen;
        if (!(terminal instanceof GuiMEMonitorable)) return;
        if (terminal != gtnhadjust$lastTerminal) {
            gtnhadjust$craftStates.clear();
            gtnhadjust$lastTerminal = terminal;
        }
        for (IAEStack<?> stack : list) {
            if (stack == null) continue;
            Boolean previous = gtnhadjust$craftStates.put(stack.copy(), stack.isCraftable());
            if ((previous == null && stack.isCraftable()) || (previous != null && previous != stack.isCraftable())) {
                GTNHAdjust.LOG.info(
                    "[craft-audit] client craftable screen={} stack={} craftable={} previous={}",
                    screen == null ? null
                        : screen.getClass()
                            .getName(),
                    stack.toNBTGeneric(),
                    stack.isCraftable(),
                    previous);
            }
        }
    }
}
