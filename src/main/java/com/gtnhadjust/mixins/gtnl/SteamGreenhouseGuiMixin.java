package com.gtnhadjust.mixins.gtnl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

@Mixin(targets = "com.science.gtnl.common.gui.modularui.multiblock.SteamGreenhouseModuleGui", remap = false)
public class SteamGreenhouseGuiMixin {

    @Shadow
    private PanelSyncManager mainSyncManager;

    @Redirect(
        method = "createCropInventoryWidget",
        at = @At(
            value = "INVOKE",
            target = "Lcom/science/gtnl/common/machine/multiblock/module/steamElevator/SteamGreenhouseModule;getMaxSeedCount()I"),
        require = 1,
        remap = false)
    private int gtnhadjust$syncedCapacity(@Coerce Object machine) {
        return mainSyncManager.findSyncHandler("steamGreenhouseMaxSeedCount", IntSyncValue.class)
            .getIntValue();
    }
}
