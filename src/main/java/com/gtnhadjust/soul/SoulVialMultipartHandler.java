package com.gtnhadjust.soul;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityInteractEvent;

import com.gtnhadjust.GTNHAdjust;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/** Forward clicks on dragon-style parts to the ordinary soul-vial capture path. */
public final class SoulVialMultipartHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onInteract(EntityInteractEvent event) {
        if (!(event.entityPlayer instanceof EntityPlayerMP player) || !(event.target instanceof EntityDragonPart part)
            || !(part.entityDragonObj instanceof EntityLivingBase parent)) return;
        ItemStack stack = player.getCurrentEquippedItem();
        Item vessel = GameRegistry.findItem("EnderIO", "itemSoulVessel");
        if (stack == null || stack.getItem() != vessel || parent.isDead) return;
        if (!stack.interactWithEntity(player, parent)) return;
        if (stack.stackSize <= 0 && !player.capabilities.isCreativeMode) player.destroyCurrentEquippedItem();
        player.sendContainerToPlayer(player.inventoryContainer);
        event.setCanceled(true);
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
            "Soul vial captured multipart entity {}",
            parent.getClass()
                .getName());
    }
}
