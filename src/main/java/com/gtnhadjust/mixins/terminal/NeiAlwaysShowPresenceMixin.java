package com.gtnhadjust.mixins.terminal;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.xyp.gtneverreset.client.gui.QuickEncodingTerminalGui;
import com.xyp.gtneverreset.compat.nei.TerminalNeiCrafting;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEStack;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiOverlayButton.ItemOverlayFormat;
import codechicken.nei.recipe.GuiOverlayButton.ItemOverlayState;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.recipe.widget.RecipeWidget;

/** Draw the cached NEI availability and AE2 craftability for the covered GTNR terminal. */
@Mixin(value = RecipeWidget.class, remap = false)
public abstract class NeiAlwaysShowPresenceMixin {

    @Shadow
    public abstract List<GuiRecipeButton> getRecipeButtons();

    @Unique
    private static QuickEncodingTerminalGui gtnhadjust$terminal() {
        if (Minecraft.getMinecraft().currentScreen instanceof GuiRecipe<?>recipe
            && recipe.firstGui instanceof QuickEncodingTerminalGui terminal) return terminal;
        return null;
    }

    @Redirect(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/NEIClientUtils;gl2DRenderContext(Ljava/lang/Runnable;)V",
            ordinal = 0),
        remap = false)
    private void gtnhadjust$skipHoveredOverlay(Runnable draw) {
        if (gtnhadjust$terminal() == null) NEIClientUtils.gl2DRenderContext(draw);
    }

    @Inject(
        method = "draw",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", ordinal = 1),
        remap = false)
    private void gtnhadjust$drawPresence(int mouseX, int mouseY, CallbackInfo ci) {
        QuickEncodingTerminalGui terminal = gtnhadjust$terminal();
        if (terminal == null) return;
        for (GuiRecipeButton button : getRecipeButtons()) {
            if (!(button instanceof GuiOverlayButton overlay) || overlay.firstGui != terminal) continue;
            List<ItemOverlayState> states = ((NeiOverlayButtonAccessor) overlay).gtnhadjust$ingredientsOverlay();
            NEIClientUtils.gl2DRenderContext(() -> {
                for (ItemOverlayState state : states) {
                    state.draw(ItemOverlayFormat.ICON);
                    PositionedStack slot = state.getSlot();
                    if (slot != null && gtnhadjust$isCraftable(terminal, slot.item)) gtnhadjust$drawPattern(slot);
                }
            });
            break;
        }
    }

    @Unique
    private static boolean gtnhadjust$isCraftable(QuickEncodingTerminalGui terminal, ItemStack stack) {
        if (stack == null) return false;
        FluidStack fluid = StackInfo.isFluidDisplayItem(stack) ? StackInfo.getFluid(stack) : null;
        IAEStack<?> query = fluid == null ? AEItemStack.create(stack) : AEFluidStack.create(fluid);
        return TerminalNeiCrafting.isCraftable(terminal, query);
    }

    @Unique
    private static void gtnhadjust$drawPattern(PositionedStack slot) {
        ItemStack pattern = AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .orNull();
        if (pattern == null) return;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(slot.relx, slot.rely, 200.0F);
            GL11.glScalef(0.4F, 0.4F, 1.0F);
            GuiContainerManager.drawItem(0, 0, pattern);
        } finally {
            GL11.glPopMatrix();
        }
    }
}
