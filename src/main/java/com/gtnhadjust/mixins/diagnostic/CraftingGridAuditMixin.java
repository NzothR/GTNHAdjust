package com.gtnhadjust.mixins.diagnostic;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.storage.data.IAEStack;
import appeng.me.cache.CraftingGridCache;

@Mixin(value = CraftingGridCache.class, remap = false)
public abstract class CraftingGridAuditMixin {

    @Shadow
    protected IGrid grid;
    @Shadow
    protected Set<ICraftingProvider> craftingProviders;
    @Shadow
    protected Map<IAEStack<?>, ?> craftableItems;
    @Shadow
    protected static int pauseRebuilds;
    @Unique
    private final Set<IAEStack<?>> gtnhadjust$previousCraftables = new HashSet<>();

    @Inject(
        method = "updateCPUClusters(Lappeng/api/networking/events/MENetworkCraftingPatternChange;)V",
        at = @At("HEAD"),
        remap = false)
    private void gtnhadjust$event(MENetworkCraftingPatternChange event, CallbackInfo ci) {
        GTNHAdjust.LOG.info(
            "[craft-audit] grid event grid={} provider={} node={} pause={}",
            System.identityHashCode(grid),
            event.provider,
            event.node,
            pauseRebuilds);
    }

    @Inject(method = "updatePatterns", at = @At("HEAD"), remap = false)
    private void gtnhadjust$before(CallbackInfo ci) {
        GTNHAdjust.LOG.info(
            "[craft-audit] grid rebuild begin grid={} paused={} providers={}",
            System.identityHashCode(grid),
            pauseRebuilds,
            craftingProviders.size());
    }

    @Inject(method = "updatePatterns", at = @At("RETURN"), remap = false)
    private void gtnhadjust$after(CallbackInfo ci) {
        GTNHAdjust.LOG.info(
            "[craft-audit] grid rebuild end grid={} paused={} craftables={}",
            System.identityHashCode(grid),
            pauseRebuilds,
            craftableItems.size());
        if (pauseRebuilds != 0) return;
        Set<IAEStack<?>> current = new HashSet<>();
        for (IAEStack<?> stack : craftableItems.keySet()) current.add(stack.copy());
        Set<IAEStack<?>> added = new HashSet<>(current);
        added.removeAll(gtnhadjust$previousCraftables);
        Set<IAEStack<?>> removed = new HashSet<>(gtnhadjust$previousCraftables);
        removed.removeAll(current);
        if (!gtnhadjust$previousCraftables.isEmpty() && (!added.isEmpty() || !removed.isEmpty())) GTNHAdjust.LOG
            .info("[craft-audit] grid={} craftable added={} removed={}", System.identityHashCode(grid), added, removed);
        gtnhadjust$previousCraftables.clear();
        gtnhadjust$previousCraftables.addAll(current);
    }
}
