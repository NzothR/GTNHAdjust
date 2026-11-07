package com.gtnhadjust.mixins.fix;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IAEStackType;
import appeng.api.storage.data.IItemList;

/** NeoECOAE 7.3.7 treats zero-count craftability changes as successful quantity updates. */
@Pseudo
@Mixin(targets = "cn.dancingsnow.neoecoae.platform.ae2.storage.NativeIncrementalStorageList", remap = false)
public abstract class NeoEcoCraftingRefreshMixin {

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true, remap = false)
    private static void gtnhadjust$rebuildForCraftability(IItemList cached, IAEStackType<?> type, Iterable<?> changes,
        CallbackInfoReturnable<Boolean> callback) {
        // Do not consume a potentially single-use iterable before the original method sees it.
        if (!(changes instanceof Collection<?>)) {
            callback.setReturnValue(false);
            return;
        }
        for (Object change : (Collection<?>) changes) {
            if (change instanceof IAEStack<?> && ((IAEStack<?>) change).getStackSize() == 0) {
                callback.setReturnValue(false);
                return;
            }
        }
    }
}
