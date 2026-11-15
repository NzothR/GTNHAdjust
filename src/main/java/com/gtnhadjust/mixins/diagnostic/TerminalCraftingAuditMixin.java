package com.gtnhadjust.mixins.diagnostic;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.common.packet.InterfacePatternTarget;

import appeng.helpers.InventoryAction;
import appeng.util.Platform;

@Mixin(targets = "com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer", remap = false)
public abstract class TerminalCraftingAuditMixin {

    @Shadow
    private IInventory trackedPatterns(long id) {
        throw new AssertionError();
    }

    @Inject(method = "performInterfacePatternAction", at = @At("HEAD"), remap = false)
    private void gtnhadjust$manualBefore(InterfacePatternTarget target, InventoryAction action, CallbackInfo ci) {
        gtnhadjust$log("manual-before " + action, target);
    }

    @Inject(method = "performInterfacePatternAction", at = @At("RETURN"), remap = false)
    private void gtnhadjust$manualAfter(InterfacePatternTarget target, InventoryAction action, CallbackInfo ci) {
        gtnhadjust$log("manual-after " + action, target);
    }

    @Inject(method = "placeEncodedPattern", at = @At("HEAD"), remap = false)
    private void gtnhadjust$autoBefore(InterfacePatternTarget target, CallbackInfo ci) {
        gtnhadjust$log("auto-before", target);
    }

    @Inject(method = "placeEncodedPattern", at = @At("RETURN"), remap = false)
    private void gtnhadjust$autoAfter(InterfacePatternTarget target, CallbackInfo ci) {
        gtnhadjust$log("auto-after", target);
    }

    @Inject(method = "refreshCraftingAvailability", at = @At("HEAD"), remap = false)
    private void gtnhadjust$refresh(InterfacePatternTarget target, CallbackInfo ci) {
        gtnhadjust$log("refresh", target);
    }

    private void gtnhadjust$log(String stage, InterfacePatternTarget target) {
        if (!Platform.isServer() || target == null) return;
        IInventory inv = trackedPatterns(target.getEntryId());
        int slot = target.getSlot();
        ItemStack stack = inv == null || slot < 0 || slot >= inv.getSizeInventory() ? null : inv.getStackInSlot(slot);
        GTNHAdjust.LOG.info(
            "[craft-audit] terminal {} id={} slot={} inventory={} pattern={} nbt={}",
            stage,
            target.getEntryId(),
            slot,
            inv == null ? null
                : inv.getClass()
                    .getName(),
            stack,
            stack == null ? null : stack.getTagCompound());
    }
}
