package com.futureresourceprotocol.recipe;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Unobtainable recipe placeholder used while a world starts with a locked hull. */
public final class HullLockToken extends Item {

    public HullLockToken() {
        setUnlocalizedName("frp.hull_lock");
        setTextureName("minecraft:paper");
        setHasSubtypes(true);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int meta = stack.getItemDamage();
        return "item.frp.hull_lock_" + (meta == 0 ? "uhv" : meta == 1 ? "uxv" : "final");
    }
}
