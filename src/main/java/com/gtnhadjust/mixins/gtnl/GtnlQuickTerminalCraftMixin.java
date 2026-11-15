package com.gtnhadjust.mixins.gtnl;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.common.gui.container.QuickEncodingTerminalContainer;
import com.xyp.gtneverreset.common.terminal.DualTerminalGuiObject;

import appeng.core.sync.GuiBridge;
import appeng.util.Platform;

/** Preserve the combined terminal's actual inventory or Baubles slot when GTNL opens AE's craft GUI. */
@Mixin(targets = "com.science.gtnl.common.packet.KeyBindingHandler", remap = false)
public abstract class GtnlQuickTerminalCraftMixin {

    @Redirect(
        method = "startCraft",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/util/Platform;openGUI(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraftforge/common/util/ForgeDirection;Lappeng/core/sync/GuiBridge;)V"))
    private void gtnhadjust$openWithTerminalSlot(EntityPlayer player, TileEntity tile, ForgeDirection side,
        GuiBridge type) {
        if (player.openContainer instanceof QuickEncodingTerminalContainer) {
            QuickEncodingTerminalContainer container = (QuickEncodingTerminalContainer) player.openContainer;
            if (container.getTarget() instanceof DualTerminalGuiObject) {
                int slot = ((DualTerminalGuiObject) container.getTarget()).getInventorySlot();
                if (GTNHAdjust.debug)
                    GTNHAdjust.LOG.info("GTNL AE craft request: opening combined terminal slot {}", slot);
                Platform.openGUI(player, tile, side, type, slot);
                return;
            }
        }
        Platform.openGUI(player, tile, side, type);
    }
}
