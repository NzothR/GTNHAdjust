package com.gtnhadjust.mixins.fix;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.common.packet.InterfacePatternTarget;

import appeng.helpers.InventoryAction;
import appeng.util.Platform;

/** Repeat the provider refresh outside the inventory action and its nested network notifications. */
@Mixin(targets = "com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer", remap = false)
public abstract class QuickTerminalCraftingRefreshMixin {

    @Shadow
    private IInventory trackedPatterns(long id) {
        throw new AssertionError();
    }

    @Shadow
    private void refreshCraftingAvailability(InterfacePatternTarget target) {
        throw new AssertionError();
    }

    @Shadow
    private void queuePatternOutputs(ItemStack pattern) {
        throw new AssertionError();
    }

    @Unique
    private final List<Refresh> gtnhadjust$pendingRefreshes = new ArrayList<>();
    @Unique
    private Refresh gtnhadjust$actionSnapshot;

    @Inject(method = "performInterfacePatternAction", at = @At("HEAD"), remap = false)
    private void gtnhadjust$beforeAction(InterfacePatternTarget target, InventoryAction action, CallbackInfo ci) {
        gtnhadjust$remember(target);
    }

    @Inject(method = "performInterfacePatternAction", at = @At("RETURN"), remap = false)
    private void gtnhadjust$afterAction(InterfacePatternTarget target, InventoryAction action, CallbackInfo ci) {
        gtnhadjust$schedule();
    }

    @Inject(method = "placeEncodedPattern", at = @At("HEAD"), remap = false)
    private void gtnhadjust$beforePlace(InterfacePatternTarget target, CallbackInfo ci) {
        gtnhadjust$remember(target);
    }

    @Inject(method = "placeEncodedPattern", at = @At("RETURN"), remap = false)
    private void gtnhadjust$afterPlace(InterfacePatternTarget target, CallbackInfo ci) {
        gtnhadjust$schedule();
    }

    @Unique
    private void gtnhadjust$remember(InterfacePatternTarget target) {
        gtnhadjust$actionSnapshot = null;
        if (!Platform.isServer() || target == null) return;
        IInventory inventory = trackedPatterns(target.getEntryId());
        int slot = target.getSlot();
        if (inventory == null || slot < 0 || slot >= inventory.getSizeInventory()) return;
        ItemStack stack = inventory.getStackInSlot(slot);
        gtnhadjust$actionSnapshot = new Refresh(target, inventory, stack == null ? null : stack.copy());
    }

    @Unique
    private void gtnhadjust$schedule() {
        Refresh change = gtnhadjust$actionSnapshot;
        gtnhadjust$actionSnapshot = null;
        if (change == null) return;
        ItemStack current = change.inventory.getStackInSlot(change.target.getSlot());
        if (ItemStack.areItemStacksEqual(change.previous, current)) return;
        change.current = current == null ? null : current.copy();
        gtnhadjust$pendingRefreshes.add(change);
    }

    @Inject(method = { "detectAndSendChanges", "func_75142_b" }, at = @At("HEAD"), require = 1, remap = false)
    private void gtnhadjust$refreshAfterAction(CallbackInfo ci) {
        if (!Platform.isServer() || gtnhadjust$pendingRefreshes.isEmpty()) return;
        List<Refresh> pending = new ArrayList<>(gtnhadjust$pendingRefreshes);
        gtnhadjust$pendingRefreshes.clear();
        for (Refresh change : pending) {
            if (trackedPatterns(change.target.getEntryId()) != change.inventory) continue;
            try {
                refreshCraftingAvailability(change.target);
                queuePatternOutputs(change.previous);
                queuePatternOutputs(change.current);
                if (GTNHAdjust.terminalDebug) GTNHAdjust.LOG.info(
                    "Deferred crafting refresh for interface {} slot {}",
                    change.target.getEntryId(),
                    change.target.getSlot());
            } catch (RuntimeException failure) {
                GTNHAdjust.LOG.warn(
                    "Deferred crafting refresh failed for interface {} slot {}",
                    change.target.getEntryId(),
                    change.target.getSlot(),
                    failure);
            }
        }
    }

    @Unique
    private static final class Refresh {

        final InterfacePatternTarget target;
        final IInventory inventory;
        final ItemStack previous;
        ItemStack current;

        Refresh(InterfacePatternTarget target, IInventory inventory, ItemStack previous) {
            this.target = target;
            this.inventory = inventory;
            this.previous = previous;
        }
    }
}
