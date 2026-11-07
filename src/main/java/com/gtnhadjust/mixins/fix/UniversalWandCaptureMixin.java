package com.gtnhadjust.mixins.fix;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.wand.CapturedMobEgg;

@Mixin(targets = "com.xyp.gtneverreset.common.wand.UniversalWandCombatHandler", remap = false)
public abstract class UniversalWandCaptureMixin {

    @Shadow
    private static int spawnEggId(EntityLivingBase victim) {
        throw new AssertionError();
    }

    @Inject(
        method = "addConfiguredDrops",
        at = @At(
            value = "INVOKE",
            target = "Lcom/xyp/gtneverreset/common/wand/UniversalWandCombatHandler;spawnEggId(Lnet/minecraft/entity/EntityLivingBase;)I"))
    private static void gtnhadjust$addFallbackEgg(LivingDropsEvent event, EntityPlayerMP player, CallbackInfo ci) {
        EntityLivingBase victim = event.entityLiving;
        if (spawnEggId(victim) > 0) return;
        ItemStack egg = CapturedMobEgg.forEntity(victim);
        if (egg == null) {
            if (GTNHAdjust.debug)
                GTNHAdjust.LOG.info("Wand capture: no registered entity name for {}", victim.getClass());
            return;
        }
        event.drops.add(new EntityItem(victim.worldObj, victim.posX, victim.posY, victim.posZ, egg));
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info("Wand capture: fallback egg for {}", victim.getClass());
    }
}
