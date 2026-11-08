package com.futureresourceprotocol.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.device.TerminalData;
import com.futureresourceprotocol.recipe.RecipeIndex;

import cpw.mods.fml.common.registry.GameRegistry;

/** Server-owned world ledger. Call only from the logical server thread. */
public final class WorldProtocol extends WorldSavedData {

    private static final String NAME = "futureresourceprotocol_ledger";
    private static final int VERSION = 1;
    private final Map<String, Entry> entries = new TreeMap<>();
    private boolean uhvMilestone;
    private boolean terminated;
    private boolean invalid;
    private Boolean allowAllOverride;
    private long debtRevision;
    private long markRevision;
    private NBTTagList worldMarks = new NBTTagList();
    public static final int MAX_MARKS = 900;

    /** Runtime-only change counter; the ledger remains the authoritative persisted state. */
    public long debtRevision() {
        return debtRevision;
    }

    public long markRevision() {
        return markRevision;
    }

    public int markCount() {
        return worldMarks.tagCount();
    }

    public boolean isValid() {
        return !invalid;
    }

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

    /** Invalid or missing mod items remain in world NBT and still occupy a mark slot. */
    public List<ItemStack> markedItems() {
        List<ItemStack> items = new ArrayList<>();
        if (invalid) return items;
        for (int i = 0; i < worldMarks.tagCount(); i++) {
            NBTTagCompound entry = worldMarks.getCompoundTagAt(i);
            String id = entry.getString("item");
            int colon = id.indexOf(':');
            if (colon <= 0 || colon == id.length() - 1) continue;
            Item item = GameRegistry.findItem(id.substring(0, colon), id.substring(colon + 1));
            if (item == null) continue;
            ItemStack stack = new ItemStack(item, 1, entry.getInteger("meta"));
            if (entry.hasKey("tag", 10)) stack.setTagCompound(
                (NBTTagCompound) entry.getCompoundTag("tag")
                    .copy());
            items.add(stack);
        }
        return items;
    }

    private NBTTagCompound markEntry(ItemStack stack) {
        String identity = key(stack);
        if (identity == null) return null;
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("item", id.toString());
        entry.setInteger("meta", stack.getItemDamage());
        if (stack.hasTagCompound()) entry.setTag(
            "tag",
            stack.getTagCompound()
                .copy());
        return entry;
    }

    public boolean hasMark(ItemStack stack) {
        if (invalid || stack == null) return false;
        NBTTagCompound target = markEntry(stack);
        if (target == null) return false;
        for (int i = 0; i < worldMarks.tagCount(); i++) if (target.equals(worldMarks.getCompoundTagAt(i))) return true;
        return false;
    }

    public boolean addMark(ItemStack stack) {
        if (invalid || isTerminated()
            || worldMarks.tagCount() >= MAX_MARKS
            || !TerminalData.safeTag(stack)
            || hasMark(stack)) return false;
        NBTTagCompound entry = markEntry(stack);
        if (entry == null) return false;
        worldMarks.appendTag(entry);
        markRevision++;
        markDirty();
        FutureResourceProtocol
            .debug("world mark added key={} count={} revision={}", key(stack), markCount(), markRevision);
        return true;
    }

    public boolean removeMark(ItemStack stack) {
        if (invalid || stack == null) return false;
        String key = key(stack);
        if (key == null || debt(key, false) != 0 || debt(key, true) != 0) return false;
        NBTTagCompound target = markEntry(stack);
        if (target == null) return false;
        for (int i = 0; i < worldMarks.tagCount(); i++) {
            if (target.equals(worldMarks.getCompoundTagAt(i))) {
                worldMarks.removeTag(i);
                markRevision++;
                markDirty();
                FutureResourceProtocol
                    .debug("world mark removed key={} count={} revision={}", key, markCount(), markRevision);
                return true;
            }
        }
        return false;
    }

    /** Remove only entries whose shared registry/meta debt is completely cleared. */
    public int removeDebtFreeMarks() {
        if (invalid) return 0;
        int removed = 0;
        for (int i = worldMarks.tagCount() - 1; i >= 0; i--) {
            NBTTagCompound entry = worldMarks.getCompoundTagAt(i);
            String id = entry.getString("item");
            if (id.isEmpty()) continue;
            String key = id + "@" + entry.getInteger("meta");
            if (debt(key, false) != 0 || debt(key, true) != 0) continue;
            worldMarks.removeTag(i);
            removed++;
        }
        if (removed > 0) {
            markRevision++;
            markDirty();
        }
        FutureResourceProtocol.debug("removed debt-free marks count={} remaining={}", removed, markCount());
        return removed;
    }

    public boolean isTerminated() {
        return terminated || invalid;
    }

    public boolean allowAllItems() {
        return allowAllOverride != null ? allowAllOverride : RecipeIndex.defaultAllowAll();
    }

    public boolean setAllowAllItems(boolean enabled) {
        if (invalid) return false;
        allowAllOverride = enabled;
        markDirty();
        FutureResourceProtocol.debug("world recipe and stage bypass changed enabled={}", enabled);
        return true;
    }

    public boolean canAdvance(boolean umv) {
        return !isTerminated() && (!umv || uhvMilestone || allowAllItems());
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
        debtRevision++;
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
        if (first + second > 0) {
            debtRevision++;
            markDirty();
        }
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
        allowAllOverride = nbt.hasKey("allowAllItems", 1) ? nbt.getBoolean("allowAllItems") : null;
        uhvMilestone = nbt.getBoolean("uhvMilestone");
        terminated = nbt.getBoolean("terminated");
        worldMarks = (NBTTagList) nbt.getTagList("worldMarks", 10)
            .copy();
        if (worldMarks.tagCount() > MAX_MARKS) invalid = true;
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
        if (allowAllOverride != null) nbt.setBoolean("allowAllItems", allowAllOverride);
        nbt.setBoolean("uhvMilestone", uhvMilestone);
        nbt.setBoolean("terminated", terminated);
        nbt.setTag("worldMarks", worldMarks.copy());
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
