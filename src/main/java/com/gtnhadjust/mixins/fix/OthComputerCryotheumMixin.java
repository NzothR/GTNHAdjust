package com.gtnhadjust.mixins.fix;

import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Pseudo
@Mixin(targets = "com.newmaa.othtech.machine.OTEComputer", remap = false)
public abstract class OthComputerCryotheumMixin {

    /** Simulate a full dose before draining so the two coolants are never mixed. */
    @WrapOperation(
        method = "tryConsumeFluid(Z)Z",
        at = @At(
            value = "INVOKE",
            target = "Lcom/newmaa/othtech/machine/OTEComputer;depleteInput(Lnet/minecraftforge/fluids/FluidStack;Z)Z"))
    private boolean gtnhadjust$chooseCoolant(@Coerce Object machine, FluidStack superCoolant, boolean simulate,
        Operation<Boolean> original) {
        if (original.call(machine, superCoolant, true)) {
            return simulate || original.call(machine, superCoolant, false);
        }
        FluidStack cryotheum = FluidRegistry.getFluidStack("cryotheum", superCoolant.amount);
        return cryotheum != null && original.call(machine, cryotheum, true)
            && (simulate || original.call(machine, cryotheum, false));
    }
}
