package com.futureresourceprotocol.integration.structurelib;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;
import com.futureresourceprotocol.recipe.RecipeIndex;
import com.gtnewhorizon.structurelib.structure.IItemSource;

/** Uses FRP only after the existing sources have provided what they can. */
public final class AdvanceItemSource implements IItemSource {

    private final IItemSource delegate;
    private final EntityPlayerMP player;

    private AdvanceItemSource(IItemSource delegate, EntityPlayerMP player) {
        this.delegate = delegate;
        this.player = player;
    }

    public static IItemSource wrap(IItemSource delegate, EntityPlayerMP player) {
        return delegate == null || player == null ? delegate : new AdvanceItemSource(delegate, player);
    }

    @Override
    public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int maximum) {
        Map<ItemStack, Integer> result = new LinkedHashMap<>();
        if (maximum <= 0 || predicate == null) return result;
        Map<ItemStack, Integer> existing = delegate.take(predicate, simulate, maximum);
        int remaining = maximum;
        if (existing != null) for (Map.Entry<ItemStack, Integer> entry : existing.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
            if (entry.getValue() != null && entry.getValue() > 0) remaining -= Math.min(remaining, entry.getValue());
        }
        if (remaining <= 0 || !hasTerminal() || !FutureResourceProtocol.enabled) return result;
        WorldProtocol world = WorldProtocol.get();
        Map<String, Long> simulatedReservations = new LinkedHashMap<>();
        for (ItemStack item : world.markedItems()) {
            if (remaining <= 0) break;
            if (!predicate.test(item.copy()) || !RecipeIndex.eligible(item, world)) continue;
            boolean umv = RecipeIndex.classify(item, world);
            String key = WorldProtocol.key(item);
            String reservationKey = key + (umv ? "#UMV" : "#UHV");
            long reserved = simulate ? simulatedReservations.getOrDefault(reservationKey, 0L) : 0;
            int low = 0, high = remaining;
            while (low < high) {
                int mid = low + (int) (((long) high - low + 1) / 2);
                if (world.canRecord(key, umv, reserved + mid)) low = mid;
                else high = mid - 1;
            }
            if (low <= 0) continue;
            if (!simulate) world.advance(key, umv, low);
            else simulatedReservations.put(reservationKey, reserved + low);
            result.put(item.copy(), low);
            remaining -= low;
            FutureResourceProtocol.debug(
                "StructureLib FRP source player={} key={} amount={} simulate={} class={}",
                player.getCommandSenderName(),
                key,
                low,
                simulate,
                umv ? "UMV" : "UHV");
        }
        return result;
    }

    private boolean hasTerminal() {
        for (ItemStack stack : player.inventory.mainInventory)
            if (stack != null && stack.getItem() == FutureResourceProtocol.terminal) return true;
        return false;
    }
}
