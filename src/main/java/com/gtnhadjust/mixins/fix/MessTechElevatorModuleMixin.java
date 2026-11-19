package com.gtnhadjust.mixins.fix;

import java.util.List;

import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.compat.MessTechElevatorLink;

import gtnhintergalactic.tile.multi.elevatormodules.TileEntityModuleBase;

@Pseudo
@Mixin(targets = "com.MessTech.common.machine.module.SpaceModuleInfinityBase", remap = false)
public abstract class MessTechElevatorModuleMixin implements MessTechElevatorLink {

    @Unique
    private Host gtnhadjust$elevator;

    /** Refresh the module structure on its next tick when a new parent accepts it. */
    @Override
    public void gtnhadjust$linkElevator(Host elevator) {
        if (gtnhadjust$elevator == elevator) return;
        gtnhadjust$elevator = elevator;
        ((TileEntityModuleBase) (Object) this).mUpdate = 1;
    }

    /** Forget the GTNL parent when disconnecting or switching to a native parent. */
    @Inject(
        method = { "disconnect()V", "connect(Lgtnhintergalactic/tile/multi/elevator/TileEntitySpaceElevator;)V",
            "connectToElevator(Lcom/MessTech/common/machine/MTSpaceElevator;)V" },
        at = @At("HEAD"))
    private void gtnhadjust$clearParent(CallbackInfo ci) {
        gtnhadjust$elevator = null;
    }

    @Inject(method = "hasLinkedElevator", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$linked(CallbackInfoReturnable<Boolean> cir) {
        if (gtnhadjust$elevator != null) cir.setReturnValue(gtnhadjust$hasLiveParent());
    }

    @Inject(method = "isMountedOnFormedElevator", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$mounted(CallbackInfoReturnable<Boolean> cir) {
        if (gtnhadjust$elevator != null) cir.setReturnValue(gtnhadjust$hasLiveParent());
    }

    @Inject(method = "linkedElevatorHasDataHatches", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$dataHatches(CallbackInfoReturnable<Boolean> cir) {
        if (gtnhadjust$elevator != null)
            cir.setReturnValue(gtnhadjust$hasLiveParent() && gtnhadjust$elevator.gtnhadjust$hasDataInputs());
    }

    @Inject(method = "linkedElevatorStoredFluids", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$fluids(CallbackInfoReturnable<List<FluidStack>> cir) {
        if (gtnhadjust$hasLiveParent()) cir.setReturnValue(gtnhadjust$elevator.gtnhadjust$moduleFluids());
    }

    @Inject(method = "getAvailableData_EM", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$computation(CallbackInfoReturnable<Long> cir) {
        if (gtnhadjust$elevator != null && ((TileEntityModuleBase) (Object) this).isDataInputListEmpty())
            cir.setReturnValue(gtnhadjust$hasLiveParent() ? gtnhadjust$elevator.gtnhadjust$availableModuleData() : 0L);
    }

    @Unique
    private boolean gtnhadjust$hasLiveParent() {
        return gtnhadjust$elevator != null
            && gtnhadjust$elevator.gtnhadjust$hosts((TileEntityModuleBase) (Object) this);
    }
}
