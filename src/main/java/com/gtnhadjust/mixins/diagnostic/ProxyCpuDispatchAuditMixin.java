package com.gtnhadjust.mixins.diagnostic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.InventoryCrafting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.machine.proxy.ProxyDispatchTrace;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.xyp.gtneverreset.api.mixinHelper.LargeInventoryCrafting;
import com.xyp.gtneverreset.common.crafting.quantum.ae2.QuantumCraftingCpuCluster;
import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;

import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingMedium;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEStack;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.util.inv.MEInventoryCrafting;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

/** Read-only observation of the actual CPU-to-hatch handoff, including GTNR batch dispatch. */
@Mixin(value = CraftingCPUCluster.class, remap = false)
public abstract class ProxyCpuDispatchAuditMixin {

    @WrapOperation(
        method = "executeCrafting",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingMedium;pushPattern(Lappeng/api/networking/crafting/ICraftingPatternDetails;Lnet/minecraft/inventory/InventoryCrafting;)Z"),
        require = 1)
    private boolean gtnhadjust$observeDispatch(ICraftingMedium medium, ICraftingPatternDetails pattern,
        InventoryCrafting inventory, Operation<Boolean> original) {
        boolean target = medium instanceof SuperMTEHatchCraftingInputME || medium instanceof MTEHatchCraftingInputME;
        long batch = inventory instanceof LargeInventoryCrafting
            ? ((LargeInventoryCrafting) inventory).gtnr$getAssemblerSize()
            : 1;
        List<String> materials = new ArrayList<>();
        if (target && inventory instanceof MEInventoryCrafting) {
            MEInventoryCrafting aeInventory = (MEInventoryCrafting) inventory;
            for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
                IAEStack<?> stack = aeInventory.getAEStackInSlot(slot);
                if (stack != null) materials.add(stack.toString());
            }
        }
        CraftingCPUCluster current = (CraftingCPUCluster) (Object) this;
        CraftingCPUCluster previous = ProxyDispatchTrace.enter(current);
        boolean accepted;
        try {
            accepted = original.call(medium, pattern, inventory);
        } finally {
            ProxyDispatchTrace.restore(previous);
        }
        if (target) {
            CraftingCPUCluster cpu = (CraftingCPUCluster) (Object) this;
            ICraftingLink link = cpu.getLastCraftingLink();
            IGregTechTileEntity tile = ((IMetaTileEntity) medium).getBaseMetaTileEntity();
            String location = tile == null || tile.getWorld() == null ? "unavailable"
                : tile.getWorld().provider.dimensionId + ":"
                    + tile.getXCoord()
                    + ","
                    + tile.getYCoord()
                    + ","
                    + tile.getZCoord();
            GTNHAdjust.LOG.info(
                "[proxy-dispatch] cpu={} quantum={} job={} medium={} location={} outerBatch={} accepted={} inputs={} outputs={}",
                Integer.toHexString(System.identityHashCode(cpu)),
                cpu instanceof QuantumCraftingCpuCluster,
                link == null ? "none" : link.getCraftingID(),
                medium.getClass()
                    .getName(),
                location,
                batch,
                accepted,
                materials,
                java.util.Arrays.toString(pattern.getCondensedAEOutputs()));
        }
        return accepted;
    }
}
