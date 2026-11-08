package com.futureresourceprotocol.protocol;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;

import com.futureresourceprotocol.FutureResourceProtocol;

import cpw.mods.fml.common.registry.GameRegistry;

/** Server-owned world ledger. Call only from the logical server thread. */
public final class WorldProtocol extends WorldSavedData {

    private static final String NAME = "futureresourceprotocol_ledger";
    private static final int VERSION = 1;
    private final Map<String, Entry> entries = new TreeMap<>();
    private boolean uhvMilestone;
    private boolean terminated;
    private boolean invalid;

    public WorldProtocol() {
        super(NAME);
    }

    public WorldProtocol(String name) {
        super(name);
    }

    public static WorldProtocol get() {
        WorldServer world = MinecraftServer.getServer()
            .worldServerForDimension(0);
        if (world == null) throw new IllegalStateException("FRP overworld is not loaded");
        WorldProtocol data = (WorldProtocol) world.mapStorage.loadData(WorldProtocol.class, NAME);
        if (data == null) {
            data = new WorldProtocol(NAME);
            world.mapStorage.setData(NAME, data);
            data.markDirty();
            FutureResourceProtocol.debug("created world ledger");
        }
        return data;
    }

    public static String key(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        try {
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            return id == null ? null : id.toString() + "@" + stack.getItemDamage();
        } catch (RuntimeException ex) {
            FutureResourceProtocol.debug(
                "cannot identify unregistered item class={} meta={} error={}",
                stack.getItem()
                    .getClass()
                    .getName(),
                stack.getItemDamage(),
                ex.toString());
            return null;
        }
    }

    public boolean isTerminated() {
        return terminated || invalid;
    }

    public boolean canAdvance(boolean umv) {
        return !isTerminated() && (!umv || uhvMilestone);
    }

    public boolean isUhvMilestone() {
        return uhvMilestone;
    }

    public Boolean cachedClass(String key) {
        Entry e = entries.get(key);
        return e == null || !e.classified ? null : e.umvClass;
    }

    public long debt(String key, boolean umv) {
        Entry e = entries.get(key);
        return e == null ? 0 : (umv ? e.umvAdvanced - e.umvPaid : e.uhvAdvanced - e.uhvPaid);
    }

    public long stageDebt(boolean umv) {
        long sum = 0;
        for (String key : entries.keySet()) {
            long value = debt(key, umv);
            if (Long.MAX_VALUE - sum < value) return Long.MAX_VALUE;
            sum += value;
        }
        return sum;
    }

    public boolean canRecord(String key, boolean umv, long amount) {
        if (key == null || amount <= 0 || !canAdvance(umv)) return false;
        Entry e = entries.get(key);
        long total = e == null ? 0 : (umv ? e.umvAdvanced : e.uhvAdvanced);
        return total <= Long.MAX_VALUE - amount && (e == null || e.events < Long.MAX_VALUE);
    }

    /** World permission only; callers must enforce it at each production entry point. */
    public boolean permitsHull(int tier) {
        if (invalid) return false;
        if (tier == 9) return stageDebt(false) == 0;
        if (tier == 12) return stageDebt(false) == 0 && stageDebt(true) == 0;
        if (tier == 13) return terminated;
        throw new IllegalArgumentException("Unknown FRP hull tier: " + tier);
    }

    public void advance(String key, boolean umv, long amount) {
        if (!canRecord(key, umv, amount)) throw new IllegalStateException("Invalid FRP advance");
        Entry e = entries.computeIfAbsent(key, k -> new Entry());
        if (!e.classified) {
            e.classified = true;
            e.umvClass = umv;
        }
        if (umv) e.umvAdvanced += amount;
        else e.uhvAdvanced += amount;
        e.events++;
        markDirty();
        FutureResourceProtocol.debug(
            "advance key={} class={} count={} outstanding={}/{} events={}",
            key,
            umv ? "UMV" : "UHV",
            amount,
            debt(key, false),
            debt(key, true),
            e.events);
    }

    /** Returns consumed count; callers must consume exactly that many physical items. */
    public long settle(String key, long count) {
        if (invalid || key == null || count <= 0) return 0;
        Entry e = entries.get(key);
        if (e == null) return 0;
        long first = Math.min(count, e.uhvAdvanced - e.uhvPaid);
        long second = Math.min(count - first, e.umvAdvanced - e.umvPaid);
        e.uhvPaid += first;
        e.umvPaid += second;
        if (first + second > 0) markDirty();
        FutureResourceProtocol.debug(
            "settle key={} offered={} consumed={} remaining={}/{}",
            key,
            count,
            first + second,
            debt(key, false),
            debt(key, true));
        return first + second;
    }

    public boolean completeUhv() {
        if (invalid || terminated || uhvMilestone || stageDebt(false) != 0) return false;
        uhvMilestone = true;
        markDirty();
        FutureResourceProtocol.debug("UHV milestone completed");
        return true;
    }

    public boolean completeUmv() {
        if (invalid || terminated || !uhvMilestone || stageDebt(false) != 0 || stageDebt(true) != 0) return false;
        terminated = true;
        markDirty();
        FutureResourceProtocol.debug("UMV milestone completed, protocol terminated");
        return true;
    }

    public long eventSequence(String key) {
        Entry e = entries.get(key);
        return e == null ? 0 : e.events;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        entries.clear();
        if (nbt.getInteger("version") != VERSION) {
            invalid = true;
            FutureResourceProtocol.LOG
                .error("Unsupported FRP world data version {}; protocol locked", nbt.getInteger("version"));
            return;
        }
        uhvMilestone = nbt.getBoolean("uhvMilestone");
        terminated = nbt.getBoolean("terminated");
        NBTTagList list = nbt.getTagList("entries", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            Entry e = new Entry();
            e.classified = tag.getBoolean("classified");
            e.umvClass = tag.getBoolean("umvClass");
            e.uhvAdvanced = tag.getLong("uhvAdvanced");
            e.uhvPaid = tag.getLong("uhvPaid");
            e.umvAdvanced = tag.getLong("umvAdvanced");
            e.umvPaid = tag.getLong("umvPaid");
            e.events = tag.getLong("events");
            String key = tag.getString("key");
            if (key.isEmpty() || e.uhvAdvanced < 0
                || e.uhvPaid < 0
                || e.uhvPaid > e.uhvAdvanced
                || e.umvAdvanced < 0
                || e.umvPaid < 0
                || e.umvPaid > e.umvAdvanced
                || entries.put(key, e) != null) {
                invalid = true;
            }
        }
        if (invalid) FutureResourceProtocol.LOG.error("Invalid FRP ledger; protocol locked; restore a backup");
        FutureResourceProtocol.debug(
            "loaded ledger entries={} uhvMilestone={} terminated={} invalid={}",
            entries.size(),
            uhvMilestone,
            terminated,
            invalid);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        if (invalid) throw new IllegalStateException("Refusing to overwrite invalid FRP ledger");
        nbt.setInteger("version", VERSION);
        nbt.setBoolean("uhvMilestone", uhvMilestone);
        nbt.setBoolean("terminated", terminated);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Entry> pair : entries.entrySet()) {
            Entry e = pair.getValue();
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("key", pair.getKey());
            tag.setBoolean("classified", e.classified);
            tag.setBoolean("umvClass", e.umvClass);
            tag.setLong("uhvAdvanced", e.uhvAdvanced);
            tag.setLong("uhvPaid", e.uhvPaid);
            tag.setLong("umvAdvanced", e.umvAdvanced);
            tag.setLong("umvPaid", e.umvPaid);
            tag.setLong("events", e.events);
            list.appendTag(tag);
        }
        nbt.setTag("entries", list);
    }

    private static final class Entry {

        boolean classified, umvClass;
        long uhvAdvanced, uhvPaid, umvAdvanced, umvPaid, events;
    }
}
