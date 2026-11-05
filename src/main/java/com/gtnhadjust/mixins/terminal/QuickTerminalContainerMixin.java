package com.gtnhadjust.mixins.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.terminal.Reflection;
import com.gtnhadjust.terminal.TransferMetadata;
import com.xyp.gtneverreset.common.gui.container.ContainerQuickEncodingTerminal;
import com.xyp.gtneverreset.common.packet.InterfacePatternTarget;
import com.xyp.gtneverreset.common.packet.RecipeTransferPayload;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEStack;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.container.slot.SlotRestrictedInput;
import appeng.util.Platform;

@Mixin(targets = "com.xyp.gtneverreset.common.gui.container.ContainerQuickEncodingTerminal", remap = false)
public abstract class QuickTerminalContainerMixin {

    @Shadow
    @Final
    private ContainerInterfaceTerminal interfaceDelegate;
    @Shadow
    @Final
    private SlotRestrictedInput encodedPatternSlot;

    @Shadow
    private void placeEncodedPattern(InterfacePatternTarget target) {
        throw new AssertionError();
    }

    @Unique
    private boolean gtnhadjust$outputWasEmpty;

    @Inject(method = "applyRecipeTransfer", at = @At("HEAD"), remap = false)
    private void gtnhadjust$beforeTransfer(RecipeTransferPayload payload, CallbackInfo ci) {
        gtnhadjust$outputWasEmpty = encodedPatternSlot != null && !encodedPatternSlot.getHasStack();
    }

    @Inject(method = "applyRecipeTransfer", at = @At("RETURN"), remap = false)
    private void gtnhadjust$upload(RecipeTransferPayload payload, CallbackInfo ci) {
        if (!Platform.isServer() || !payload.shouldEncode()) return;
        if (!gtnhadjust$outputWasEmpty || encodedPatternSlot == null || !encodedPatternSlot.getHasStack()) {
            gtnhadjust$log("encoding did not produce a new pattern in an empty output slot");
            return;
        }
        ItemStack pattern = encodedPatternSlot.getStack();
        if (!(pattern.getItem() instanceof ICraftingPatternItem)) return;
        ICraftingPatternDetails details = ((ICraftingPatternItem) pattern.getItem()).getPatternForItem(
            pattern,
            ((ContainerQuickEncodingTerminal) (Object) this).getPlayerInv().player.worldObj);
        if (details == null) {
            gtnhadjust$log("cannot read encoded pattern");
            return;
        }
        int outputCount = 0;
        for (IAEStack<?> output : details.getAEOutputs())
            if (output != null && output.getStackSize() > 0) outputCount++;
        if (outputCount != 1) {
            gtnhadjust$log("automatic upload skipped: " + outputCount + " output slots");
            return;
        }
        TransferMetadata meta = (TransferMetadata) (Object) payload;
        try {
            Map<?, ?> tracked = (Map<?, ?>) Reflection.field(interfaceDelegate, "trackedById");
            List<Long> ids = new ArrayList<>();
            for (long id : meta.gtnhadjust$getTargets()) if (tracked.containsKey(id)) ids.add(id);
            String query = meta.gtnhadjust$getSearch()
                .trim()
                .toLowerCase(Locale.ROOT);
            if (!query.isEmpty()) {
                for (Map.Entry<?, ?> entry : tracked.entrySet()) {
                    String name = String.valueOf(Reflection.field(entry.getValue(), "name"))
                        .toLowerCase(Locale.ROOT);
                    long id = ((Number) entry.getKey()).longValue();
                    if (name.contains(query) && !ids.contains(id)) ids.add(id);
                }
            }
            for (long id : ids) {
                Object tracker = tracked.get(id);
                if (tracker == null || !Boolean.TRUE.equals(Reflection.field(tracker, "online"))
                    || !Boolean.TRUE.equals(Reflection.field(tracker, "shouldDisplay"))) continue;
                IInventory inventory = (IInventory) Reflection.field(tracker, "patterns");
                int slots = Math
                    .min(((Number) Reflection.field(tracker, "numSlots")).intValue(), inventory.getSizeInventory());
                for (int slot = 0; slot < slots; slot++) {
                    if (inventory.getStackInSlot(slot) != null || !inventory.isItemValidForSlot(slot, pattern))
                        continue;
                    placeEncodedPattern(new InterfacePatternTarget(id, slot));
                    if (!encodedPatternSlot.getHasStack()) {
                        interfaceDelegate.scheduleUpdate();
                        gtnhadjust$log("uploaded to interface " + id + " slot " + slot);
                        return;
                    }
                }
            }
            gtnhadjust$log("no suitable matching interface slot; pattern remains in terminal");
        } catch (ReflectiveOperationException | RuntimeException failure) {
            if (GTNHAdjust.terminalDebug)
                GTNHAdjust.LOG.info("Quick terminal: upload failed; pattern remains in terminal", failure);
        }
    }

    @Unique
    private static void gtnhadjust$log(String message) {
        if (GTNHAdjust.terminalDebug) GTNHAdjust.LOG.info("Quick terminal: {}", message);
    }
}
