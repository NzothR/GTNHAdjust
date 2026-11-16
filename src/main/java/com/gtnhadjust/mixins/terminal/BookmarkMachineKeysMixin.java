package com.gtnhadjust.mixins.terminal;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.terminal.BookmarkMachineGroups;

import codechicken.nei.BookmarkPanel;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.Widget;

/** Handle V only when NEI dispatches an unfocused key to the bookmark panel's group gutter. */
@Mixin(value = Widget.class, remap = false)
public abstract class BookmarkMachineKeysMixin {

    /** Advertise the two shortcuts only on an actual group gutter. */
    @Inject(method = "handleHotkeys", at = @At("HEAD"))
    private void gtnhadjust$hotkeys(int x, int y, java.util.Map<String, String> hotkeys,
        CallbackInfoReturnable<java.util.Map<String, String>> cir) {
        if (!((Object) this instanceof BookmarkPanel panel) || panel.inEditingState()) return;
        int row = panel.getGrid()
            .getHoveredRowIndex(true);
        if (row < 0 || panel.getGrid()
            .getRowGroupId(row) == codechicken.nei.bookmark.BookmarkGrid.DEFAULT_GROUP_ID) return;
        hotkeys.put("V", net.minecraft.util.StatCollector.translateToLocal("gtnhadjust.bookmark.sort_key"));
        hotkeys.put("Shift + V", net.minecraft.util.StatCollector.translateToLocal("gtnhadjust.bookmark.classify_key"));
    }

    @Inject(method = "handleKeyPress", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$machineGroups(int key, char character, CallbackInfoReturnable<Boolean> cir) {
        if (key != Keyboard.KEY_V || Keyboard.isRepeatEvent()
            || NEIClientUtils.controlKey()
            || NEIClientUtils.altKey()
            || !((Object) this instanceof BookmarkPanel panel)) return;
        if (BookmarkMachineGroups.handle(panel, NEIClientUtils.shiftKey())) cir.setReturnValue(true);
    }
}
