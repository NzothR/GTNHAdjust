package com.gtnhadjust.mixins.diagnostic;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.container.implementations.ContainerMEMonitorable;
import appeng.core.sync.packets.PacketMEInventoryUpdate;

@Mixin(value = ContainerMEMonitorable.class, remap = false)
public abstract class MonitorContainerAuditMixin {

    @Shadow
    private boolean needListUpdate;
    @Shadow
    @Final
    private Map<IAEStackType<?>, Set<IAEStack<?>>> updateQueue;
    @Unique
    private final Set<IAEStack<?>> gtnhadjust$trackedOutputs = new HashSet<>();

    @Inject(method = { "detectAndSendChanges", "func_75142_b" }, at = @At("HEAD"), require = 1, remap = false)
    private void gtnhadjust$beforeSync(CallbackInfo ci) {
        if (gtnhadjust$trackedOutputs.isEmpty()) return;
        for (Set<IAEStack<?>> queue : updateQueue.values()) {
            for (IAEStack<?> stack : queue) {
                if (gtnhadjust$trackedOutputs.contains(stack)) GTNHAdjust.LOG.info(
                    "[craft-audit] server sync pending container={} stack={} fullUpdate={}",
                    getClass().getName(),
                    stack.toNBTGeneric(),
                    needListUpdate);
            }
        }
    }

    @Inject(method = "postChange", at = @At("HEAD"), remap = false)
    private void gtnhadjust$queue(IBaseMonitor<IAEStack<?>> monitor, Iterable<IAEStack<?>> changes,
        BaseActionSource source, CallbackInfo ci) {
        if (monitor != null) return;
        for (IAEStack<?> stack : changes) {
            if (stack != null) gtnhadjust$trackedOutputs.add(stack.copy());
        }
        GTNHAdjust.LOG.info(
            "[craft-audit] container={} monitor={} queue={} fullUpdate={}",
            getClass().getName(),
            monitor == null ? null : System.identityHashCode(monitor),
            changes,
            needListUpdate);
    }

    @Redirect(
        method = { "detectAndSendChanges", "func_75142_b" },
        at = @At(
            value = "INVOKE",
            target = "Lappeng/core/sync/packets/PacketMEInventoryUpdate;appendItem(Lappeng/api/storage/data/IAEStack;)V"),
        require = 1,
        remap = false)
    private void gtnhadjust$sentStack(PacketMEInventoryUpdate packet, IAEStack<?> stack) throws IOException {
        if (stack != null && gtnhadjust$trackedOutputs.contains(stack)) GTNHAdjust.LOG.info(
            "[craft-audit] server packet container={} stack={} craftable={} fullUpdate={}",
            getClass().getName(),
            stack.toNBTGeneric(),
            stack.isCraftable(),
            needListUpdate);
        packet.appendItem(stack);
    }

    @Redirect(
        method = "queueInventoryList",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/core/sync/packets/PacketMEInventoryUpdate;appendItem(Lappeng/api/storage/data/IAEStack;)V"),
        require = 1,
        remap = false)
    private void gtnhadjust$initialStack(PacketMEInventoryUpdate packet, IAEStack<?> stack) throws IOException {
        gtnhadjust$sentStack(packet, stack);
    }
}
