package com.gtnhadjust.machine.collector;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.gtnhadjust.machine.proxy.ProxyThreadTask;

/** Persistent one-task-per-recipe ledger; all mutation happens on the server thread. */
final class CollectorThreadLedger {

    private final Map<String, ProxyThreadTask> tasks = new LinkedHashMap<>();

    boolean add(String key, ProxyThreadTask task, int limit) {
        if (task == null || !CollectorThreadPolicy.canStart(tasks.keySet(), key, limit)
            || task.getThread() >= limit
            || slotUsed(task.getThread())) return false;
        tasks.put(key, task);
        return true;
    }

    boolean slotUsed(int slot) {
        for (ProxyThreadTask task : tasks.values()) if (task.getThread() == slot) return true;
        return false;
    }

    int size() {
        return tasks.size();
    }

    boolean isEmpty() {
        return tasks.isEmpty();
    }

    Set<String> keys() {
        return tasks.keySet();
    }

    Collection<ProxyThreadTask> values() {
        return tasks.values();
    }

    Set<Map.Entry<String, ProxyThreadTask>> entries() {
        return tasks.entrySet();
    }

    void clear() {
        tasks.clear();
    }

    NBTTagList save() {
        NBTTagList entries = new NBTTagList();
        for (Map.Entry<String, ProxyThreadTask> entry : tasks.entrySet()) {
            NBTTagCompound saved = entry.getValue()
                .save();
            saved.setString("recipeKey", entry.getKey());
            entries.appendTag(saved);
        }
        return entries;
    }

    void load(NBTTagList entries) {
        Map<String, ProxyThreadTask> restored = new LinkedHashMap<>();
        Set<Integer> slots = new java.util.HashSet<>();
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound saved = entries.getCompoundTagAt(i);
            String key = saved.getString("recipeKey");
            ProxyThreadTask task = ProxyThreadTask.load(saved);
            if (key.isEmpty() || restored.put(key, task) != null || !slots.add(task.getThread()))
                throw new IllegalStateException("Invalid or duplicate saved collector thread");
        }
        tasks.clear();
        tasks.putAll(restored);
    }
}
