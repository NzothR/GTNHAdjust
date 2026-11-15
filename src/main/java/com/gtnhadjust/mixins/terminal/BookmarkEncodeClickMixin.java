package com.gtnhadjust.mixins.terminal;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.terminal.BookmarkQuickEncode;
import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;

import codechicken.nei.BookmarkPanel;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.bookmark.BookmarkItem.BookmarkItemType;
import codechicken.nei.bookmark.BookmarksGridSlot;

@Mixin(value = BookmarkPanel.class, remap = false)
public abstract class BookmarkEncodeClickMixin {

    @Inject(method = "handleClick", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$encodeBookmark(int x, int y, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0 || !NEIClientUtils.altKey()
            || !(Minecraft.getMinecraft().currentScreen instanceof QuickEncodingTerminalGui terminal)) return;
        BookmarkPanel panel = (BookmarkPanel) (Object) this;
        BookmarksGridSlot slot = panel.getSlotMouseOver(x, y);
        if (slot == null || slot.getType() != BookmarkItemType.RESULT || slot.getRecipeId() == null) return;
        BookmarkQuickEncode.encode(terminal, panel, slot.getBookmarkItem());
        cir.setReturnValue(true);
    }
}
