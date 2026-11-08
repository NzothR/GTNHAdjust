package com.gtnhadjust.mixins.proxy;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.machine.proxy.ProxyFactoryRegistration;

@Mixin(RenderItem.class)
public abstract class ProxyFactoryItemOverlayMixin {

    @Inject(
        method = "renderItemOverlayIntoGUI(Lnet/minecraft/client/gui/FontRenderer;Lnet/minecraft/client/renderer/texture/TextureManager;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V",
        at = @At("RETURN"))
    private void gtnhadjust$drawMark(FontRenderer font, TextureManager textures, ItemStack stack, int x, int y,
        String text, CallbackInfo ci) {
        if (!ProxyFactoryRegistration.isController(stack)) return;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 300.0F);
            GL11.glScalef(0.75F, 0.75F, 1.0F);
            font.drawString(
                "依",
                (int) (16 / 0.75F) - font.getStringWidth("依"),
                (int) (16 / 0.75F) - font.FONT_HEIGHT + 1,
                0xFFFFFF,
                true);
        } finally {
            GL11.glPopMatrix();
        }
    }
}
