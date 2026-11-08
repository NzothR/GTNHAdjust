package com.gtnhadjust.mixins.diagnostic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.machine.proxy.ProxyFactory;
import com.xyp.gtneverreset.common.crafting.quantum.ae2.QuantumCraftingCpuCluster;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.data.IAEStack;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/** Records actual CPU acceptance from the controller without altering the transfer. */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class ProxyCpuReceiveAuditMixin {

    @Inject(method = "cancel", at = @At("HEAD"), remap = false)
    private void gtnhadjust$observeCancellation(CallbackInfo callback) {
        CraftingCPUCluster cpu = (CraftingCPUCluster) (Object) this;
        if (!(cpu instanceof QuantumCraftingCpuCluster)) return;
        ICraftingLink link = cpu.getLastCraftingLink();
        GTNHAdjust.LOG.info(
            "[proxy-cpu-cancel] cpu={} job={}",
            Integer.toHexString(System.identityHashCode(cpu)),
            link == null ? "none" : link.getCraftingID());
    }

    @Inject(method = "injectItems", at = @At("RETURN"), remap = false)
    private void gtnhadjust$observeReceive(IAEStack<?> input, Actionable mode, BaseActionSource source,
        CallbackInfoReturnable<IAEStack<?>> result) {
        if (mode != Actionable.MODULATE || !(source instanceof MachineSource)) return;
        Object via = ((MachineSource) source).via;
        if (!(via instanceof IGregTechTileEntity)
            || !(((IGregTechTileEntity) via).getMetaTileEntity() instanceof ProxyFactory)) return;
        CraftingCPUCluster cpu = (CraftingCPUCluster) (Object) this;
        ICraftingLink link = cpu.getLastCraftingLink();
        IAEStack<?> remaining = result.getReturnValue();
        GTNHAdjust.LOG.info(
            "[proxy-cpu-receive] cpu={} quantum={} job={} input={} remainder={}",
            Integer.toHexString(System.identityHashCode(cpu)),
            cpu instanceof QuantumCraftingCpuCluster,
            link == null ? "none" : link.getCraftingID(),
            input,
            remaining);
    }
}
