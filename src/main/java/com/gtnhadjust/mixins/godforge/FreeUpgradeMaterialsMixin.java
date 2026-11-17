package com.gtnhadjust.mixins.godforge;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tectech.thing.metaTileEntity.multi.godforge.upgrade.ForgeOfGodsUpgrade;
import tectech.thing.metaTileEntity.multi.godforge.upgrade.UpgradeStorage;

/** The client upgrade tree opens the selected node directly instead of its material payment panel. */
@Mixin(value = UpgradeStorage.class, remap = false)
public abstract class FreeUpgradeMaterialsMixin {

    @Inject(method = "isCostPaid", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$paid(ForgeOfGodsUpgrade upgrade, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    /** The manual payment panel must also leave ingredients untouched while upgrades are free. */
    @Inject(method = "payCost", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$skipPayment(ForgeOfGodsUpgrade upgrade, ItemStack[] inputs, CallbackInfo ci) {
        ci.cancel();
    }
}
