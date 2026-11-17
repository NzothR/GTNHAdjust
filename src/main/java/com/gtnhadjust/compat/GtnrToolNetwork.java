package com.gtnhadjust.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.util.GTUtility;

/** Per-network/tick presence queries. Never extracts ingredients, submits jobs or waits for a crafting result. */
public final class GtnrToolNetwork {

    private static final Map<AENetworkProxy, Presence> CACHE = new WeakHashMap<>();

    private GtnrToolNetwork() {}

    public static boolean available(ItemStack tool, ItemStack[] physical, AENetworkProxy network, long tick) {
        for (ItemStack item : physical) if (GTUtility.areStacksEqual(tool, item, false)) return true;
        if (network == null || !network.isActive()) return false;
        Presence presence = CACHE.computeIfAbsent(network, ignored -> new Presence());
        if (presence.tick != tick) {
            presence.tick = tick;
            presence.tools.clear();
        }
        return presence.tools.computeIfAbsent(GTUtility.ItemId.create(tool), ignored -> query(tool, network));
    }

    private static boolean query(ItemStack tool, AENetworkProxy network) {
        try {
            IAEItemStack physical = network.getStorage()
                .getItemInventory()
                .getStorageList()
                .findPrecise(AEItemStack.create(GTUtility.copyAmount(1, tool)));
            if (physical != null && physical.getStackSize() > 0) return true;
            Item programmer = GameRegistry.findItem("programmablehatches", "prog_circuit");
            if (programmer == null) return false;
            ItemStack encoded = new ItemStack(programmer, 1);
            NBTTagCompound target = GTUtility.copyAmount(1, tool)
                .writeToNBT(new NBTTagCompound());
            target.setString("string_id", Item.itemRegistry.getNameForObject(tool.getItem()));
            target.removeTag("id");
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("targetCircuit", target);
            encoded.setTagCompound(tag);
            IAEItemStack requested = AEItemStack.create(encoded);
            IAEItemStack stored = network.getStorage()
                .getItemInventory()
                .getStorageList()
                .findPrecise(requested);
            return stored != null && stored.getStackSize() > 0 || !network.getCrafting()
                .getCraftingFor(requested, null, 0, null)
                .isEmpty();
        } catch (GridAccessException disconnected) {
            return false;
        }
    }

    private static final class Presence {

        long tick = Long.MIN_VALUE;
        final Map<GTUtility.ItemId, Boolean> tools = new HashMap<>();
    }
}
