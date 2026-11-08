package com.futureresourceprotocol.integration.ae2;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;

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
import baubles.api.BaublesApi;
import cpw.mods.fml.common.Loader;

/** Extract real network items, then settle only the exact quantity actually extracted. */
public final class AeDebtRepayment {

    private static final double POWER = 1.0;

    private AeDebtRepayment() {}

    public static void repay(EntityPlayerMP player, WorldProtocol world) {
        Access access;
        try {
            access = findAccess(player);
        } catch (RuntimeException failure) {
            FutureResourceProtocol.LOG.error("FRP could not inspect wireless terminals", failure);
            player.addChatMessage(new ChatComponentTranslation("frp.chat.ae_error", 0));
            return;
        }
        if (access == null) {
            player.addChatMessage(new ChatComponentTranslation("frp.chat.ae_no_access"));
            FutureResourceProtocol
                .debug("AE repayment denied player={} no usable wireless access", player.getCommandSenderName());
            return;
        }
        long total = 0;
        int items = 0;
        boolean charged = false;
        try {
            // Snapshot the keys before mutating the AE monitor.
            List<IAEItemStack> storedItems = new ArrayList<>();
            for (IAEItemStack stored : access.monitor.getStorageList())
                if (stored != null && stored.getStackSize() > 0) storedItems.add(stored.copy());
            for (IAEItemStack stored : storedItems) {
                ItemStack item = stored.getItemStack();
                String key = WorldProtocol.key(item);
                if (key == null) continue;
                long available = stored.getStackSize();
                // Each stage fits in a long; do not overflow when both stages have debt.
                for (int stage = 0; stage < 2 && available > 0; stage++) {
                    long remaining = world.debt(key, stage == 1);
                    if (remaining <= 0) continue;
                    long wanted = Math.min(remaining, available);
                    IAEItemStack request = stored.copy()
                        .setStackSize(wanted);
                    IAEItemStack simulated = access.monitor
                        .extractItems(request.copy(), Actionable.SIMULATE, access.source);
                    if (simulated == null || simulated.getStackSize() <= 0) continue;
                    wanted = Math.min(wanted, simulated.getStackSize());
                    if (!charged) {
                        if (access.host.extractAEPower(POWER, Actionable.MODULATE, PowerMultiplier.CONFIG)
                            < POWER * 0.99) break;
                        charged = true;
                    }
                    IAEItemStack extracted = access.monitor
                        .extractItems(request.setStackSize(wanted), Actionable.MODULATE, access.source);
                    if (extracted == null || extracted.getStackSize() <= 0) continue;
                    long actual = extracted.getStackSize();
                    String actualKey = WorldProtocol.key(extracted.getItemStack());
                    long offered = key.equals(actualKey) ? Math.min(actual, wanted) : 0;
                    long paid = world.settle(key, offered);
                    if (paid < actual) restore(player, access, extracted, actual - paid);
                    total = total > Long.MAX_VALUE - paid ? Long.MAX_VALUE : total + paid;
                    if (paid > 0) items++;
                    available -= Math.min(available, actual);
                    FutureResourceProtocol.debug(
                        "AE repayment player={} key={} requested={} extracted={} settled={} UHV={} UMV={}",
                        player.getCommandSenderName(),
                        key,
                        wanted,
                        actual,
                        paid,
                        world.debt(key, false),
                        world.debt(key, true));
                }
            }
            boolean complete = world.stageDebt(false) == 0 && world.stageDebt(true) == 0;
            int removed = complete ? world.removeDebtFreeMarks() : 0;
            player.addChatMessage(
                new ChatComponentTranslation(
                    "frp.chat.ae_repaid",
                    total,
                    items,
                    removed,
                    world.stageDebt(false),
                    world.stageDebt(true)));
            FutureResourceProtocol.debug(
                "AE repayment complete player={} paid={} entries={} removed={} complete={}",
                player.getCommandSenderName(),
                total,
                items,
                removed,
                complete);
        } catch (RuntimeException failure) {
            FutureResourceProtocol.LOG.error("FRP AE repayment interrupted after settled=" + total, failure);
            player.addChatMessage(new ChatComponentTranslation("frp.chat.ae_error", total));
        }
    }

    private static void restore(EntityPlayerMP player, Access access, IAEItemStack extracted, long amount) {
        IAEItemStack remainder;
        try {
            remainder = access.monitor.injectItems(
                extracted.copy()
                    .setStackSize(amount),
                Actionable.MODULATE,
                access.source);
        } catch (RuntimeException failure) {
            FutureResourceProtocol.LOG
                .error("FRP could not return unexpected AE extraction; giving it to the player", failure);
            remainder = extracted.copy()
                .setStackSize(amount);
        }
        if (remainder == null || remainder.getStackSize() <= 0) return;
        long left = remainder.getStackSize();
        ItemStack prototype = remainder.getItemStack();
        if (prototype == null) throw new IllegalStateException("Cannot recover AE extraction without item identity");
        while (left > 0) {
            ItemStack stack = prototype.copy();
            stack.stackSize = (int) Math.min(left, Math.max(1, stack.getMaxStackSize()));
            left -= stack.stackSize;
            player.inventory.addItemStackToInventory(stack);
            if (stack.stackSize > 0) player.dropPlayerItemWithRandomChoice(stack, false);
        }
        player.inventory.markDirty();
    }

    private static Access findAccess(EntityPlayerMP player) {
        List<ItemStack> candidates = new ArrayList<>();
        if (Loader.isModLoaded("Baubles|Expanded")) {
            IInventory baubles = BaublesApi.getBaubles(player);
            if (baubles != null)
                for (int i = 0; i < baubles.getSizeInventory(); i++) add(candidates, baubles.getStackInSlot(i));
        }
        for (ItemStack item : player.inventory.mainInventory) add(candidates, item);
        for (ItemStack terminal : candidates) {
            try {
                IWirelessTermHandler handler = AEApi.instance()
                    .registries()
                    .wireless()
                    .getWirelessTerminalHandler(terminal);
                if (handler == null || !handler.hasPower(player, POWER, terminal)) continue;
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
                    || host.extractAEPower(POWER, Actionable.SIMULATE, PowerMultiplier.CONFIG) < POWER * 0.99) continue;
                FutureResourceProtocol.debug(
                    "AE repayment access player={} terminal={} dimension={}",
                    player.getCommandSenderName(),
                    WorldProtocol.key(terminal),
                    player.dimension);
                return new Access(host, monitor, new PlayerSource(player, host));
            } catch (RuntimeException failure) {
                FutureResourceProtocol.debug(
                    "AE terminal access failed player={} terminal={} error={}",
                    player.getCommandSenderName(),
                    WorldProtocol.key(terminal),
                    failure.toString());
            }
        }
        return null;
    }

    private static void add(List<ItemStack> list, ItemStack item) {
        if (item != null && AEApi.instance()
            .registries()
            .wireless()
            .isWirelessTerminal(item)) list.add(item);
    }

    private static final class Access {

        final WirelessTerminalGuiObject host;
        final IMEMonitor<IAEItemStack> monitor;
        final BaseActionSource source;

        Access(WirelessTerminalGuiObject host, IMEMonitor<IAEItemStack> monitor, BaseActionSource source) {
            this.host = host;
            this.monitor = monitor;
            this.source = source;
        }
    }
}
