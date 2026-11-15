package com.gtnhadjust.mixins.terminal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.terminal.ClientTargets;
import com.gtnhadjust.terminal.Reflection;
import com.gtnhadjust.terminal.TransferMetadata;
import com.xyp.gtneverreset.common.packet.RecipeTransferPayload;

@Mixin(targets = "com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui", remap = false)
public abstract class QuickTerminalGuiMixin {

    @Shadow
    private boolean pendingAutoPlace;

    @Inject(method = "transferRecipe", at = @At("HEAD"), remap = false)
    private void gtnhadjust$targets(RecipeTransferPayload payload, String search, CallbackInfo ci) {
        try {
            Object terminal = Reflection.field(this, "interfaceTerminal");
            ((TransferMetadata) (Object) payload).gtnhadjust$setTargets(ClientTargets.find(terminal, search), search);
        } catch (ReflectiveOperationException ignored) {
            ((TransferMetadata) (Object) payload).gtnhadjust$setTargets(new long[0], search);
        }
    }

    @Inject(method = "transferRecipe", at = @At("RETURN"), remap = false)
    private void gtnhadjust$disableOldUpload(RecipeTransferPayload payload, String search, CallbackInfo ci) {
        pendingAutoPlace = false;
    }
}
