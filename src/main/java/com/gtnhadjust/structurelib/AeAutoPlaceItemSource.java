package com.gtnhadjust.structurelib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnhadjust.GTNHAdjust;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.SecurityPermissions;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.PlayerInventoryUtil;

/** Adds the player's linked AE network as a fallback material source for StructureLib survival placement. */
public final class AeAutoPlaceItemSource implements IItemSource {

    private static final double ACCESS_POWER = 1.0;

    private final IItemSource inventorySource;
    private final EntityPlayerMP player;

    private AeAutoPlaceItemSource(IItemSource inventorySource, EntityPlayerMP player) {
        this.inventorySource = inventorySource;
        this.player = player;
    }

    public static IItemSource wrap(IItemSource inventorySource, EntityPlayerMP player) {
        if (inventorySource == null || player == null) return inventorySource;
        return new AeAutoPlaceItemSource(inventorySource, player);
    }

    @Override
    public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int maximum) {
        Map<ItemStack, Integer> taken = new LinkedHashMap<>();
        Map<ItemStack, Integer> fromInventory = inventorySource.take(predicate, simulate, maximum);
        if (fromInventory != null) fromInventory.forEach((stack, count) -> add(taken, stack, count));

        int remaining = Math.max(0, maximum - count(taken));
        if (remaining > 0) {
            Map<ItemStack, Integer> fromNetwork = takeFromNetwork(predicate, simulate, remaining);
            fromNetwork.forEach((stack, count) -> add(taken, stack, count));
        }
        return taken;
    }

    private Map<ItemStack, Integer> takeFromNetwork(Predicate<ItemStack> predicate, boolean simulate, int maximum) {
        if (maximum <= 0) return Collections.emptyMap();

        try {
            Access access = findAccess();
            if (access == null) return Collections.emptyMap();

            List<PlannedExtraction> plan = new ArrayList<>();
            int planned = 0;
            for (IAEItemStack stored : access.monitor.getStorageList()) {
                if (stored == null || stored.getStackSize() <= 0) continue;
                ItemStack item = stored.getItemStack();
                if (item == null || item.getItem() == null) continue;
                item = item.copy();
                item.stackSize = 1;
                if (!predicate.test(item.copy())) continue;

                int requested = (int) Math.min((long) maximum - planned, stored.getStackSize());
                if (requested <= 0) break;
                IAEItemStack request = copyWithSize(stored, requested);
                if (request == null) continue;
                IAEItemStack available = access.monitor
                    .extractItems(request.copy(), Actionable.SIMULATE, access.actionSource);
                if (available == null || available.getStackSize() <= 0) continue;

                int amount = (int) Math.min((long) requested, available.getStackSize());
                plan.add(new PlannedExtraction(request, amount));
                planned += amount;
                if (planned >= maximum) break;
            }

            if (plan.isEmpty()) return Collections.emptyMap();
            Actionable mode = simulate ? Actionable.SIMULATE : Actionable.MODULATE;
            if (!simulate && access.host.extractAEPower(ACCESS_POWER, Actionable.MODULATE, PowerMultiplier.CONFIG)
                < ACCESS_POWER * 0.99) return Collections.emptyMap();

            Map<ItemStack, Integer> extractedItems = new LinkedHashMap<>();
            for (PlannedExtraction extraction : plan) {
                IAEItemStack extracted = access.monitor
                    .extractItems(extraction.request.copy(), mode, access.actionSource);
                if (extracted == null || extracted.getStackSize() <= 0) continue;
                ItemStack stack = extracted.getItemStack();
                int amount = (int) Math.min((long) extraction.amount, extracted.getStackSize());
                if (stack != null && amount > 0) add(extractedItems, stack, amount);
            }
            return extractedItems;
        } catch (RuntimeException failure) {
            if (GTNHAdjust.debug) GTNHAdjust.LOG.warn("StructureLib AE material extraction failed", failure);
            return Collections.emptyMap();
        }
    }

    private Access findAccess() {
        List<ItemStack> terminals = new ArrayList<>();
        addTerminal(terminals, PlayerInventoryUtil.getFirstWirelessTerminal(player));
        for (ItemStack stack : player.inventory.mainInventory) addTerminal(terminals, stack);

        for (ItemStack terminal : terminals) {
            IWirelessTermHandler handler = AEApi.instance()
                .registries()
                .wireless()
                .getWirelessTerminalHandler(terminal);
            if (handler == null || !handler.hasPower(player, ACCESS_POWER, terminal)) continue;

            WirelessTerminalGuiObject host = new WirelessTerminalGuiObject(
                handler,
                terminal,
                player,
                player.worldObj,
                -1,
                0,
                0);
            if (!host.rangeCheck() || host.getGrid() == null) continue;

            ISecurityGrid security = host.getGrid()
                .getCache(ISecurityGrid.class);
            if (security == null || !security.hasPermission(player, SecurityPermissions.EXTRACT)) continue;

            IMEMonitor<IAEItemStack> monitor = host.getItemInventory();
            if (monitor == null
                || host.extractAEPower(ACCESS_POWER, Actionable.SIMULATE, PowerMultiplier.CONFIG) < ACCESS_POWER * 0.99)
                continue;
            BaseActionSource actionSource = new PlayerSource(player, host);
            return new Access(host, monitor, actionSource);
        }
        return null;
    }

    private static void addTerminal(List<ItemStack> terminals, ItemStack candidate) {
        if (candidate == null || !AEApi.instance()
            .registries()
            .wireless()
            .isWirelessTerminal(candidate)) return;
        for (ItemStack terminal : terminals) if (terminal == candidate) return;
        terminals.add(candidate);
    }

    private static IAEItemStack copyWithSize(IAEItemStack stack, int size) {
        IAEItemStack copy = stack.copy();
        if (copy != null) copy.setStackSize(size);
        return copy;
    }

    private static int count(Map<ItemStack, Integer> items) {
        long count = 0;
        for (Integer amount : items.values()) if (amount != null && amount > 0) count += amount;
        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    private static void add(Map<ItemStack, Integer> items, ItemStack stack, int count) {
        if (stack == null || stack.getItem() == null || count <= 0) return;
        for (Map.Entry<ItemStack, Integer> entry : items.entrySet()) {
            ItemStack existing = entry.getKey();
            if (existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
                entry.setValue(entry.getValue() + count);
                return;
            }
        }
        items.put(stack.copy(), count);
    }

    private static final class Access {

        private final WirelessTerminalGuiObject host;
        private final IMEMonitor<IAEItemStack> monitor;
        private final BaseActionSource actionSource;

        private Access(WirelessTerminalGuiObject host, IMEMonitor<IAEItemStack> monitor,
            BaseActionSource actionSource) {
            this.host = host;
            this.monitor = monitor;
            this.actionSource = actionSource;
        }
    }

    private static final class PlannedExtraction {

        private final IAEItemStack request;
        private final int amount;

        private PlannedExtraction(IAEItemStack request, int amount) {
            this.request = request;
            this.amount = amount;
        }
    }
}
