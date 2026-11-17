package com.gtnhadjust.mixins.godforge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tectech.thing.metaTileEntity.multi.godforge.upgrade.ForgeOfGodsUpgrade;
import tectech.thing.metaTileEntity.multi.godforge.upgrade.UpgradeStorage;
import tectech.thing.metaTileEntity.multi.godforge.util.ForgeOfGodsData;

/** EyeOfHarmonyBuffer's free upgrade behavior, adapted to GTNH 2.9.0's data model. */
@Mixin(value = ForgeOfGodsData.class, remap = false)
public abstract class FreeUpgradesMixin {

    @Shadow
    public abstract UpgradeStorage getUpgrades();

    @Shadow
    public abstract void setGravitonShardEjection(boolean enabled);

    /** Unlock exactly the clicked node without prerequisite, ring, shard or material checks. */
    @Inject(method = "unlockUpgrade", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$unlock(ForgeOfGodsUpgrade upgrade, CallbackInfo ci) {
        getUpgrades().unlockUpgrade(upgrade);
        ci.cancel();
    }

    /** Free purchases have no shard refund; retain the native dependent-node and final-upgrade cleanup rules. */
    @Inject(method = "respecUpgrade", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$respec(ForgeOfGodsUpgrade upgrade, CallbackInfo ci) {
        UpgradeStorage upgrades = getUpgrades();
        if (upgrades.isUpgradeActive(upgrade) && upgrades.checkDependents(upgrade)) {
            upgrades.respecUpgrade(upgrade);
            if (upgrade == ForgeOfGodsUpgrade.END) setGravitonShardEjection(false);
        }
        ci.cancel();
    }
}
