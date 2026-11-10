package com.gtnhadjust.mixins.quest;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.quest.QuestCraftQueue;

import appeng.container.implementations.ContainerCraftConfirm;

@Mixin(value = ContainerCraftConfirm.class, remap = false)
public abstract class QuestCraftConfirmMixin {

    @Inject(
        method = "startJob(Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/container/implementations/ContainerCraftConfirm;switchToOriginalGUI()V"))
    private void gtnhadjust$submitted(boolean follow, CallbackInfo ci) {
        EntityPlayer player = ((ContainerCraftConfirm) (Object) this).getPlayerInv().player;
        if (!player.worldObj.isRemote) QuestCraftQueue.submitted(player, (Container) (Object) this);
    }

    @Inject(method = "func_75134_a(Lnet/minecraft/entity/player/EntityPlayer;)V", at = @At("TAIL"), remap = false)
    private void gtnhadjust$closed(EntityPlayer player, CallbackInfo ci) {
        if (!player.worldObj.isRemote) QuestCraftQueue.closed(player, (Container) (Object) this);
    }
}
