package com.gtnhadjust.mixins.diagnostic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.machine.proxy.ProxyDispatchTrace;
import com.xyp.gtneverreset.common.crafting.quantum.ae2.QuantumCraftingCpuCluster;

import appeng.api.networking.crafting.ICraftingLink;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.util.inv.MEInventoryCrafting;

/** Connects a CPU dispatch with the exact GTNR pattern-slot instance. */
@Mixin(targets = "com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer$CircuitSlot", remap = false)
public abstract class ProxyCircuitSlotAuditMixin {

    @Inject(method = "insert", at = @At("RETURN"), remap = false)
    private void gtnhadjust$observeSlot(@Coerce Object recipe, MEInventoryCrafting inventory,
        CallbackInfoReturnable<Boolean> result) {
        CraftingCPUCluster cpu = ProxyDispatchTrace.current();
        if (cpu == null) return;
        ICraftingLink link = cpu.getLastCraftingLink();
        GTNHAdjust.LOG.info(
            "[proxy-slot] cpu={} quantum={} job={} inventory={}@{} accepted={}",
            Integer.toHexString(System.identityHashCode(cpu)),
            cpu instanceof QuantumCraftingCpuCluster,
            link == null ? "none" : link.getCraftingID(),
            this.getClass()
                .getName(),
            Integer.toHexString(System.identityHashCode(this)),
            result.getReturnValue());
    }
}
