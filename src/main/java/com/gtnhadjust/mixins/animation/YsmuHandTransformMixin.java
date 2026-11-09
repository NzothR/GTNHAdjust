package com.gtnhadjust.mixins.animation;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.animation.YsmuAnimatedArm;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(targets = "com.fox.ysmu.client.renderer.FirstPersonHandRenderer", remap = false)
public abstract class YsmuHandTransformMixin {

    @WrapOperation(
        method = "shouldRenderCustomHand",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/ItemRenderer;itemToRender:Lnet/minecraft/item/ItemStack;",
            remap = true))
    private static ItemStack gtnhadjust$allowHeldArm(ItemRenderer renderer, Operation<ItemStack> original) {
        return YsmuAnimatedArm.isResolving() ? null : original.call(renderer);
    }

    @Inject(method = "applyVanillaEmptyHandTransform", at = @At("HEAD"), cancellable = true)
    private static void gtnhadjust$useAnimatedTransform(EntityPlayer player, ItemRenderer renderer, float partial,
        CallbackInfo ci) {
        if (YsmuAnimatedArm.isRendering()) ci.cancel();
    }

    @Inject(method = "alignGeckoArmToVanillaBipedArm", at = @At("RETURN"))
    private static void gtnhadjust$mirroredWinding(CallbackInfo ci) {
        if (YsmuAnimatedArm.isMirrored()) GL11.glFrontFace(GL11.GL_CCW);
    }
}
