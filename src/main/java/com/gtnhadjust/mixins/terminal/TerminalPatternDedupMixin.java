package com.gtnhadjust.mixins.terminal;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.terminal.PatternDuplicates;
import com.gtnhadjust.terminal.TerminalNotifications;
import com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer;
import com.xyp.gtneverreset.common.packet.InterfacePatternTarget;

import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.InventoryAction;

@Mixin(targets = "com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer", remap = false)
public abstract class TerminalPatternDedupMixin {

    @Shadow
    @Final
    private SlotRestrictedInput encodedPatternSlot;

    @Shadow
    private IInventory trackedPatterns(long id) {
        throw new AssertionError();
    }

    @Inject(method = "performInterfacePatternAction", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtnhadjust$checkManual(InterfacePatternTarget target, InventoryAction action, CallbackInfo ci) {
        if (action != InventoryAction.PICKUP_OR_SET_DOWN || target == null) return;
        QuickEncodingTerminalContainer terminal = (QuickEncodingTerminalContainer) (Object) this;
        EntityPlayer player = terminal.getPlayerInv().player;
        ItemStack held = terminal.getPlayerInv()
            .getItemStack();
        if (held == null) return;
        if (PatternDuplicates.contains(trackedPatterns(target.getEntryId()), target.getSlot(), held, player.worldObj)) {
            TerminalNotifications.report(player, "已拒绝放入：此接口已有产物种类和 NBT 完全相同的样板。");
            ci.cancel();
        }
    }

    @Inject(method = "placeEncodedPattern", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtnhadjust$checkAutomatic(InterfacePatternTarget target, CallbackInfo ci) {
        if (target == null || encodedPatternSlot == null || !encodedPatternSlot.getHasStack()) return;
        EntityPlayer player = ((QuickEncodingTerminalContainer) (Object) this).getPlayerInv().player;
        if (PatternDuplicates.contains(
            trackedPatterns(target.getEntryId()),
            target.getSlot(),
            encodedPatternSlot.getStack(),
            player.worldObj)) {
            TerminalNotifications.report(player, "未自动上传：此接口已有产物种类和 NBT 完全相同的样板；样板已保留。");
            ci.cancel();
        }
    }
}
