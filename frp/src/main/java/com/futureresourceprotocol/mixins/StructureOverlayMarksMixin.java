package com.futureresourceprotocol.mixins;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.futureresourceprotocol.device.TerminalGui;

import blockrenderer6343.client.renderer.WorldSceneRenderer;
import blockrenderer6343.client.utils.BRUtil;
import codechicken.nei.recipe.GuiRecipe;

/** Replace only FRP's structure-overlay action; leave pattern-terminal handling with BlockRenderer. */
@Mixin(value = BRUtil.class, remap = false)
public abstract class StructureOverlayMarksMixin {

    @Inject(method = "neiOverlay", at = @At("HEAD"), cancellable = true, require = 1)
    private static void frp$markStructure(WorldSceneRenderer renderer, CallbackInfo ci) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiRecipe<?>recipe) || !(recipe.firstGui instanceof TerminalGui terminal))
            return;
        ci.cancel();
        List<ItemStack> materials = new ArrayList<>();
        for (ItemStack item : BRUtil.getIngredients(renderer)) {
            if (item != null && !BRUtil.hatchFilter.test(item)) materials.add(item);
        }
        mc.displayGuiScreen(terminal);
        terminal.importStructureMarks(materials);
    }
}
