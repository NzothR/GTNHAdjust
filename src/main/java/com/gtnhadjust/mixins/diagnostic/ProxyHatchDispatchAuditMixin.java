package com.gtnhadjust.mixins.diagnostic;

import net.minecraft.inventory.InventoryCrafting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.machine.proxy.ProxyDispatchTrace;
import com.xyp.gtneverreset.api.mixinHelper.LargeInventoryCrafting;
import com.xyp.gtneverreset.common.crafting.quantum.ae2.QuantumCraftingCpuCluster;
import com.xyp.gtneverreset.common.machine.multiblock.QuantumComputer;

import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/** Observes the multiplier at the receiving hatch, inside GTNR's batch wrapper. */
@Mixin(
    targets = { "com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME",
        "com.xyp.gtneverreset.common.machine.hatch.CircuitMEPatternBuffer",
        "gregtech.common.tileentities.machines.MTEHatchCraftingInputME" },
    remap = false)
public abstract class ProxyHatchDispatchAuditMixin {

    @Inject(method = "pushPattern", at = @At("HEAD"), remap = false)
    private void gtnhadjust$observeActualBatch(ICraftingPatternDetails pattern, InventoryCrafting inventory,
        CallbackInfoReturnable<Boolean> callback) {
        CraftingCPUCluster cpu = ProxyDispatchTrace.current();
        if (cpu == null) return;
        ICraftingLink link = cpu.getLastCraftingLink();
        long batch = inventory instanceof LargeInventoryCrafting
            ? ((LargeInventoryCrafting) inventory).gtnr$getAssemblerSize()
            : 1;
        String owner = "none";
        if (cpu instanceof QuantumCraftingCpuCluster) {
            QuantumComputer computer = ((QuantumCraftingCpuCluster) cpu).getVirtualCPUOwner();
            IGregTechTileEntity tile = computer == null ? null : computer.getBaseMetaTileEntity();
            if (tile != null && tile.getWorld() != null) owner = tile.getWorld().provider.dimensionId + ":"
                + tile.getXCoord()
                + ","
                + tile.getYCoord()
                + ","
                + tile.getZCoord();
        }
        GTNHAdjust.LOG.info(
            "[proxy-dispatch-hatch] cpu={} quantum={} owner={} job={} batch={} medium={}",
            Integer.toHexString(System.identityHashCode(cpu)),
            cpu instanceof QuantumCraftingCpuCluster,
            owner,
            link == null ? "none" : link.getCraftingID(),
            batch,
            this.getClass()
                .getName());
    }
}
