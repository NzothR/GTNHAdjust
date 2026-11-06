package com.gtnhadjust.mixins.fix;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;

import com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler;

import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton.ItemOverlayState;
import codechicken.nei.recipe.IRecipeHandler;

/** Keep GTNR's transfer behavior, but use the same ME availability overlay as the AE pattern terminal. */
@Mixin(targets = "com.xyp.gtneverreset.compat.nei.QuickTerminalRecipeTransferHandler", remap = false)
public abstract class QuickTerminalPresenceMixin implements IOverlayHandler {

    @Override
    public List<ItemOverlayState> presenceOverlay(GuiContainer gui, IRecipeHandler recipe, int index) {
        return NEEPatternTerminalHandler.instance.presenceOverlay(gui, recipe, index);
    }
}
