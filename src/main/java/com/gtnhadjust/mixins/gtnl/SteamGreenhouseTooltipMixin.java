package com.gtnhadjust.mixins.gtnl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "com.science.gtnl.common.machine.tooltip.MultiblockTooltipReader", remap = false)
public class SteamGreenhouseTooltipMixin {

    @ModifyVariable(method = "ownLines", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1, remap = false)
    private static String gtnhadjust$tooltip(String key) {
        return key.endsWith(".steam_greenhouse_module") ? "gtnl.machine.gtnhadjust_steam_greenhouse_module" : key;
    }
}
