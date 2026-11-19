package com.gtnhadjust.mixins.gtnl;

import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnhadjust.compat.MessTechElevatorLink;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.science.gtnl.common.machine.multiblock.SuperSpaceElevator;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gtnhintergalactic.recipe.IGRecipeMaps;
import gtnhintergalactic.tile.multi.elevatormodules.TileEntityModuleBase;
import tectech.thing.metaTileEntity.multi.base.TTMultiblockBase;

@Mixin(value = SuperSpaceElevator.class, remap = false)
public abstract class SuperElevatorMessTechMixin extends TTMultiblockBase implements MessTechElevatorLink.Host {

    protected SuperElevatorMessTechMixin(String name) {
        super(name);
    }

    /** Pass the parent while preserving GTNL's normal module activation and power transfer. */
    @WrapOperation(
        method = "onPostTick",
        at = @At(
            value = "INVOKE",
            target = "Lgtnhintergalactic/tile/multi/elevatormodules/TileEntityModuleBase;connect()V"))
    private void gtnhadjust$connectModule(TileEntityModuleBase module, Operation<Void> original) {
        if (module instanceof MessTechElevatorLink link && mMachine) link.gtnhadjust$linkElevator(this);
        original.call(module);
    }

    @Override
    public boolean gtnhadjust$hosts(TileEntityModuleBase module) {
        SuperSpaceElevator elevator = (SuperSpaceElevator) (Object) this;
        return mMachine && getBaseMetaTileEntity() instanceof TileEntity tile
            && !tile.isInvalid()
            && getBaseMetaTileEntity().isAllowedToWork()
            && elevator.motorTier >= module.getNeededMotorTier()
            && elevator.mProjectModuleHatches.contains(module);
    }

    @Override
    public boolean gtnhadjust$hasDataInputs() {
        return !eInputData.isEmpty();
    }

    @Override
    public long gtnhadjust$availableModuleData() {
        SuperSpaceElevator elevator = (SuperSpaceElevator) (Object) this;
        int activeMiners = 0;
        for (TileEntityModuleBase module : elevator.mProjectModuleHatches) {
            IGregTechTileEntity tile = module.getBaseMetaTileEntity();
            if (tile != null && tile.isActive()
                && module.isDataInputListEmpty()
                && module.getRecipeMap() == IGRecipeMaps.spaceMiningRecipes) activeMiners++;
        }
        return getAvailableData_EM() / Math.max(1, activeMiners);
    }

    @Override
    public List<FluidStack> gtnhadjust$moduleFluids() {
        return getStoredFluids();
    }
}
