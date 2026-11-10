package com.gtnhadjust.mixins.gtnl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.science.gtnl.common.machine.multiblock.GrandAssemblyLine;

@Mixin(value = GrandAssemblyLine.class, remap = false)
public abstract class GrandAssemblyMaintenanceMixin {

    @Shadow
    public boolean mWrench;
    @Shadow
    public boolean mScrewdriver;
    @Shadow
    public boolean mSoftMallet;
    @Shadow
    public boolean mHardHammer;
    @Shadow
    public boolean mSolderingTool;
    @Shadow
    public boolean mCrowbar;

    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    public void checkMaintenance() {
        mWrench = mScrewdriver = mSoftMallet = mHardHammer = mSolderingTool = mCrowbar = true;
    }
}
