package com.gtnhadjust.mixins.gtnl;

import org.spongepowered.asm.mixin.Mixin;

import com.science.gtnl.common.machine.multiblock.GrandAssemblyLine;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Mixin(value = GrandAssemblyLine.class, remap = false)
public abstract class GrandAssemblyMaintenanceMixin {

    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    public void checkMaintenance() {
        MTEMultiBlockBase machine = (MTEMultiBlockBase) (Object) this;
        machine.mWrench = machine.mScrewdriver = machine.mSoftMallet = machine.mHardHammer = machine.mSolderingTool = machine.mCrowbar = true;
    }
}
