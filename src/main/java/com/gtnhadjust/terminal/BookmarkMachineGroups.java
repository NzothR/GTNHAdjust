package com.gtnhadjust.terminal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;

import com.gtnhadjust.mixins.terminal.BookmarkGridAccessor;

import codechicken.nei.BookmarkPanel;
import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.bookmark.BookmarkGroup;
import codechicken.nei.bookmark.BookmarkItem;
import codechicken.nei.bookmark.BookmarkItem.BookmarkItemType;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.common.blocks.ItemMachines;

/** Batch edits one hovered bookmark group, keeping complete recipe entries, quantities and NBT intact. */
public final class BookmarkMachineGroups {

    private BookmarkMachineGroups() {}

    /** Only the left group gutter handles these keys; dragging and unrelated panel shortcuts remain unchanged. */
    public static boolean handle(BookmarkPanel panel, boolean classify) {
        if (panel.inEditingState()) return false;
        BookmarkGrid grid = panel.getGrid();
        int row = grid.getHoveredRowIndex(true);
        if (row < 0) return false;
        int groupId = grid.getRowGroupId(row);
        if (groupId == BookmarkGrid.DEFAULT_GROUP_ID) return false;
        List<BookmarkItem> items = ((BookmarkGridAccessor) grid).gtnhadjust$items();
        Map<Object, Unit> entries = new LinkedHashMap<>();
        for (BookmarkItem item : items) if (item.groupId == groupId) {
            Object key = item.recipeId == null ? item : item.recipeId;
            entries.computeIfAbsent(key, ignored -> new Unit())
                .add(item);
        }
        List<Unit> machines = new ArrayList<>();
        for (Unit unit : entries.values()) if (unit.machine != null) machines.add(unit);
        if (machines.isEmpty()) return true;
        int groups = classify ? classify(grid, items, machines) : sort(grid, items, machines, groupId);
        grid.onItemsChanged();
        panel.save();
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            new ChatComponentText(
                StatCollector.translateToLocalFormatted(
                    classify ? "gtnhadjust.bookmark.classified" : "gtnhadjust.bookmark.sorted",
                    machines.size(),
                    groups)));
        return true;
    }

    /** Sort measured machines lexicographically; extract all unknown structures into one separate group. */
    private static int sort(BookmarkGrid grid, List<BookmarkItem> items, List<Unit> machines, int groupId) {
        Map<IMetaTileEntity, BookmarkMachineStructure.Size> sizes = new HashMap<>();
        List<Unit> known = new ArrayList<>(), unknown = new ArrayList<>();
        for (Unit unit : machines) {
            if (!sizes.containsKey(unit.machine))
                sizes.put(unit.machine, BookmarkMachineStructure.measure(unit.machine, unit.stack));
            unit.size = sizes.get(unit.machine);
            (unit.size == null ? unknown : known).add(unit);
        }
        known.sort(
            Comparator.comparingLong((Unit unit) -> unit.size.shortSide())
                .thenComparingLong(unit -> unit.size.longSide())
                .thenComparingLong(unit -> unit.size.height())
                .thenComparing(unit -> unit.stack.getDisplayName(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(unit -> unit.stack.getDisplayName()));
        Set<BookmarkItem> selected = new HashSet<>();
        for (Unit unit : machines) selected.addAll(unit.items);
        int insertion = items.size();
        for (int i = 0; i < items.size(); i++) if (selected.contains(items.get(i))) {
            insertion = i;
            break;
        }
        items.removeIf(selected::contains);
        List<BookmarkItem> sorted = new ArrayList<>();
        for (Unit unit : known) sorted.addAll(unit.items);
        items.addAll(Math.min(insertion, items.size()), sorted);
        if (!unknown.isEmpty()) {
            int target = newGroup(grid, grid.getGroup(groupId).viewMode);
            append(items, unknown, target);
        }
        return unknown.size();
    }

    /** One new group per shared registered pool; machines in several shared pools are copied to each group. */
    private static int classify(BookmarkGrid grid, List<BookmarkItem> items, List<Unit> machines) {
        Map<String, List<Unit>> pools = new TreeMap<>();
        for (Unit unit : machines) if (unit.machine instanceof RecipeMapWorkable workable) {
            Set<String> seen = new HashSet<>();
            for (RecipeMap<?> map : workable.getAvailableRecipeMaps()) {
                if (map != null && RecipeMap.ALL_RECIPE_MAPS.get(map.unlocalizedName) == map
                    && seen.add(map.unlocalizedName))
                    pools.computeIfAbsent(map.unlocalizedName, ignored -> new ArrayList<>())
                        .add(unit);
            }
        }
        Set<BookmarkItem> moved = new HashSet<>();
        int count = 0;
        Map<Integer, BookmarkGroup> groups = ((BookmarkGridAccessor) grid).gtnhadjust$groups();
        int nextId = Math.max(
            grid.getNextGroupId(),
            groups.keySet()
                .stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1);
        for (List<Unit> pool : pools.values()) if (pool.size() >= 2) {
            int target = nextId++;
            groups.put(target, new BookmarkGroup(BookmarkPanel.BookmarkViewMode.DEFAULT));
            append(items, pool, target);
            for (Unit unit : pool) moved.addAll(unit.items);
            count++;
        }
        items.removeIf(moved::contains);
        return count;
    }

    /** Avoid reusing an empty source group's ID while moving its last entries into a new group. */
    private static int newGroup(BookmarkGrid grid, BookmarkPanel.BookmarkViewMode mode) {
        Map<Integer, BookmarkGroup> groups = ((BookmarkGridAccessor) grid).gtnhadjust$groups();
        int id = Math.max(
            grid.getNextGroupId(),
            groups.keySet()
                .stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1);
        groups.put(id, new BookmarkGroup(mode));
        return id;
    }

    /** Copy complete bookmark records, preserving recipe identity, amounts, permutations and stack NBT. */
    private static void append(List<BookmarkItem> items, List<Unit> units, int target) {
        for (Unit unit : units) for (BookmarkItem item : unit.items) {
            BookmarkItem copy = item.copy();
            copy.groupId = target;
            items.add(copy);
        }
    }

    /** A recipe's ingredients and results travel together; ordinary controller bookmarks form single-entry units. */
    private static final class Unit {

        private final List<BookmarkItem> items = new ArrayList<>();
        private IMetaTileEntity machine;
        private ItemStack stack;
        private BookmarkMachineStructure.Size size;

        void add(BookmarkItem item) {
            items.add(item);
            if (machine != null || item.type == BookmarkItemType.INGREDIENT) return;
            IMetaTileEntity candidate = ItemMachines.getMetaTileEntity(item.itemStack);
            if (candidate instanceof MTEMultiBlockBase) {
                machine = candidate;
                stack = item.itemStack;
            }
        }
    }
}
