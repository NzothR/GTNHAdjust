package com.gtnhadjust.mixins.diagnostic;

import java.util.Collection;
import java.util.Deque;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;

import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.me.cache.NetworkMonitor;

@Mixin(value = NetworkMonitor.class, remap = false)
public abstract class NetworkMonitorAuditMixin {

    @Shadow
    @Final
    private static Deque<NetworkMonitor<?>> GLOBAL_DEPTH;
    @Shadow
    @Final
    private IAEStackType<?> stackType;
    @Shadow
    private int localDepthSemaphore;

    @Inject(method = "postChange", at = @At("HEAD"), remap = false)
    private void gtnhadjust$notice(boolean add, Iterable<IAEStack<?>> changes, BaseActionSource source,
        CallbackInfo ci) {
        if (!(changes instanceof Collection) || ((Collection<?>) changes).size() < 2) return;
        GTNHAdjust.LOG.info(
            "[craft-audit] monitor batch monitor={} type={} add={} suppressed={} count={}",
            System.identityHashCode(this),
            stackType,
            add,
            localDepthSemaphore > 0 || GLOBAL_DEPTH.contains(this),
            ((Collection<?>) changes).size());
    }
}
