package com.gtnhadjust.diagnostic;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.gtnhadjust.GTNHAdjust;

import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.helpers.PatternHelper;

/** Diagnostics validate a copy only; they never clear the flag on the saved pattern. */
public final class MatrixPatternAudit {

    private static final ThreadLocal<Boolean> LOADING = new ThreadLocal<>();
    private static final Map<World, Set<Integer>> REPORTED = new WeakHashMap<>();

    private MatrixPatternAudit() {}

    public static void beginLoad() {
        LOADING.set(true);
    }

    public static void endLoad() {
        LOADING.remove();
    }

    private static boolean invalid(ItemStack stack) {
        return stack != null && stack.hasTagCompound()
            && stack.getTagCompound()
                .getBoolean("InvalidPattern");
    }

    public static void observe(ItemStack stack, World world, ICraftingPatternDetails result) {
        if (stack == null || world == null || world.isRemote || result != null) return;
        int hash = fingerprint(stack);
        Set<Integer> seen = REPORTED.computeIfAbsent(world, key -> new HashSet<>());
        if (seen.size() < 2048 && seen.add(hash))
            report("decode", world, -1, stack, invalid(stack), invalid(stack), result);
    }

    public static void inspect(World world, IInventory inventory, int x, int y, int z) {
        if (world == null || world.isRemote || inventory == null) return;
        int invalid = 0;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!invalid(stack)) continue;
            invalid++;
            report("tick20:" + x + "," + y + "," + z, world, slot, stack, true, true, null);
        }
        if (invalid > 0)
            GTNHAdjust.LOG.info("[matrix-audit] ready position={},{},{} invalidPatterns={}", x, y, z, invalid);
    }

    private static int fingerprint(ItemStack stack) {
        ItemStack copy = stack.copy();
        if (copy.hasTagCompound()) copy.getTagCompound()
            .removeTag("InvalidPattern");
        return copy.getTagCompound() == null ? 0
            : copy.getTagCompound()
                .hashCode();
    }

    private static void report(String phase, World world, int slot, ItemStack stack, boolean before, boolean after,
        ICraftingPatternDetails original) {
        ItemStack copy = stack.copy();
        NBTTagCompound tag = copy.getTagCompound();
        if (tag != null) tag.removeTag("InvalidPattern");
        String retry;
        try {
            if (copy.getItem() instanceof appeng.items.misc.ItemEncodedPattern) {
                ICraftingPatternDetails details = new PatternHelper(copy, world);
                retry = "valid:" + details.isCraftable();
            } else if (copy.getItem() instanceof ICraftingPatternItem) {
                ICraftingPatternDetails details = ((ICraftingPatternItem) copy.getItem())
                    .getPatternForItem(copy, world);
                retry = details == null ? "null" : "valid:" + details.isCraftable();
            } else retry = "not-pattern";
        } catch (Throwable error) {
            retry = error.getClass()
                .getSimpleName() + ":"
                + String.valueOf(error.getMessage());
        }
        NBTTagCompound originalTag = stack.getTagCompound();
        GTNHAdjust.LOG.info(
            "[matrix-audit] phase={} loading={} dim={} worldTime={} slot={} fingerprint={} invalidBefore={} invalidAfter={} decoded={} retryCopy={} crafting={} inputSlots={} outputSlots={}",
            phase,
            Boolean.TRUE.equals(LOADING.get()),
            world.provider.dimensionId,
            world.getTotalWorldTime(),
            slot,
            fingerprint(stack),
            before,
            after,
            original != null,
            retry,
            originalTag != null && originalTag.getBoolean("crafting"),
            originalTag == null ? -1
                : originalTag.getTagList("in", 10)
                    .tagCount(),
            originalTag == null ? -1
                : originalTag.getTagList("out", 10)
                    .tagCount());
    }
}
