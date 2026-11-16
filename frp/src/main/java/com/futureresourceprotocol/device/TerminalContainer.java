package com.futureresourceprotocol.device;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.util.ChatComponentTranslation;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;

public final class TerminalContainer extends Container {

    public static final String SESSION_TAG = "frpSession";
    public boolean marking;
    public final int heldSlot;
    int importAdded, importSkipped;
    private final String session;
    private final InventoryPlayer inventory;
    private WorldProtocol lastWorld;
    private long lastRevision = -1;
    private long lastSyncTick = -1;
    private long lastMarkRevision = -1;
    private boolean lastValid;
    private Map<String, long[]> lastDebts;

    public TerminalContainer(InventoryPlayer inventory, int slot, boolean marking) {
        this.marking = marking;
        this.heldSlot = slot;
        this.inventory = inventory;
        ItemStack terminal = slot >= 0 && slot < 9 ? inventory.getStackInSlot(slot) : null;
        this.session = terminal != null && terminal.hasTagCompound() ? terminal.getTagCompound()
            .getString(SESSION_TAG) : "";
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 139 + row * 18));
        for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(inventory, col, 8 + col * 18, 197));
    }

    public String session() {
        return session;
    }

    public void setMarking(boolean marking) {
        this.marking = marking;
        lastRevision = -1;
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!(inventory.player instanceof EntityPlayerMP) || inventory.player.worldObj.isRemote) return;
        EntityPlayerMP player = (EntityPlayerMP) inventory.player;
        if (!canInteractWith(player)) return;
        WorldProtocol world = WorldProtocol.get();
        boolean marksChanged = world != lastWorld || world.markRevision() != lastMarkRevision;
        if (!marksChanged && world.debtRevision() == lastRevision) return;
        long tick = player.worldObj.getTotalWorldTime();
        if (lastSyncTick >= 0 && tick >= lastSyncTick && tick - lastSyncTick < 5) return;
        java.util.List<Long> times = new java.util.ArrayList<>();
        java.util.List<ItemStack> marks = world.markedItems(times);
        if (marksChanged) {
            int parts = Math.max(1, (marks.size() + MarkSyncMessage.PER_PART - 1) / MarkSyncMessage.PER_PART);
            for (int part = 0; part < parts; part++) {
                int start = part * MarkSyncMessage.PER_PART;
                int end = Math.min(marks.size(), start + MarkSyncMessage.PER_PART);
                MarkSyncMessage message = new MarkSyncMessage(
                    windowId,
                    session,
                    world.markRevision(),
                    part,
                    parts,
                    Math.min(world.markCount(), TerminalData.MAX),
                    world.isValid(),
                    new java.util.ArrayList<>(marks.subList(start, end)));
                message.times = new java.util.ArrayList<>(times.subList(start, end));
                FutureResourceProtocol.NETWORK.sendTo(message, player);
            }
            FutureResourceProtocol.debug(
                "world marks sync player={} visible={} stored={} revision={} parts={}",
                player.getCommandSenderName(),
                marks.size(),
                world.markCount(),
                world.markRevision(),
                parts);
        }
        Map<String, long[]> snapshot = new LinkedHashMap<>();
        if (world.isValid()) for (ItemStack item : marks) {
            String key = WorldProtocol.key(item);
            if (key != null && !snapshot.containsKey(key))
                snapshot.put(key, new long[] { world.debt(key, false), world.debt(key, true) });
        }
        boolean changed = (lastDebts == null || lastWorld != world
            || lastValid != world.isValid()
            || !sameDebts(lastDebts, snapshot)
            || marksChanged);
        lastWorld = world;
        lastRevision = world.debtRevision();
        lastMarkRevision = world.markRevision();
        if (marksChanged) lastSyncTick = tick;
        if (!changed) return;
        lastValid = world.isValid();
        lastDebts = snapshot;
        lastSyncTick = tick;
        FutureResourceProtocol.NETWORK.sendTo(new DebtSyncMessage(windowId, session, lastValid, snapshot), player);
        FutureResourceProtocol.debug(
            "terminal debt sync player={} entries={} revision={} valid={}",
            player.getCommandSenderName(),
            snapshot.size(),
            lastRevision,
            lastValid);
    }

    private static boolean sameDebts(Map<String, long[]> first, Map<String, long[]> second) {
        if (first.size() != second.size()) return false;
        for (Map.Entry<String, long[]> entry : first.entrySet()) {
            long[] other = second.get(entry.getKey());
            if (other == null || entry.getValue()[0] != other[0] || entry.getValue()[1] != other[1]) return false;
        }
        return true;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        ItemStack current = player.inventory.getStackInSlot(heldSlot);
        boolean valid = current != null && current.getItem() == FutureResourceProtocol.terminal
            && player.inventory.currentItem == heldSlot
            && current.hasTagCompound()
            && sameSession(
                session,
                current.getTagCompound()
                    .getString(SESSION_TAG));
        if (!valid && !player.worldObj.isRemote) FutureResourceProtocol.debug(
            "terminal session closed player={} expectedSlot={} selectedSlot={} hasTerminal={} hasSession={}",
            player.getCommandSenderName(),
            heldSlot,
            player.inventory.currentItem,
            current != null && current.getItem() == FutureResourceProtocol.terminal,
            current != null && current.hasTagCompound()
                && !current.getTagCompound()
                    .getString(SESSION_TAG)
                    .isEmpty());
        return valid;
    }

    static boolean sameSession(String expected, String actual) {
        return expected != null && !expected.isEmpty() && expected.equals(actual);
    }

    @Override
    public ItemStack slotClick(int slot, int button, int mode, EntityPlayer player) {
        if (slot >= 0 && slot < inventorySlots.size() && ((Slot) inventorySlots.get(slot)).getSlotIndex() == heldSlot)
            return null;
        if (mode == 2 && button == heldSlot) return null;
        return super.slotClick(slot, button, mode, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slot) {
        if (!marking && FutureResourceProtocol.portableRepaymentEnabled
            && !player.worldObj.isRemote
            && canInteractWith(player)
            && slot >= 0
            && slot < inventorySlots.size()) {
            Slot target = (Slot) inventorySlots.get(slot);
            if (target.getSlotIndex() == heldSlot || !target.getHasStack()) return null;
            ItemStack item = target.getStack();
            String key = WorldProtocol.key(item);
            long paid = WorldProtocol.get()
                .settle(key, item.stackSize);
            item.stackSize -= (int) paid;
            if (item.stackSize == 0) target.putStack(null);
            else target.onSlotChanged();
            ((EntityPlayerMP) player).playerNetServerHandler
                .sendPacket(new S2FPacketSetSlot(windowId, slot, target.getStack()));
            player.addChatMessage(new ChatComponentTranslation("frp.chat.repaid", paid));
            FutureResourceProtocol.debug(
                "shift repayment player={} key={} paid={} remaining={}",
                player.getCommandSenderName(),
                key,
                paid,
                item.stackSize);
        }
        return null;
    }
}
