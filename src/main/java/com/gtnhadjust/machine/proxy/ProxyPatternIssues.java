package com.gtnhadjust.machine.proxy;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtneverreset.common.machine.hatch.SuperMTEHatchCraftingInputME;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

/** Retains only failed, still-pending pattern buffers. No source structures are scanned to refresh this display. */
final class ProxyPatternIssues {

    private static final Field SUPER_PARENT = field(SuperMTEHatchCraftingInputME.PatternSlot.class, "parentMTE");
    private static final Field SUPER_SLOTS = field(SuperMTEHatchCraftingInputME.class, "internalInventory");
    private static final Field GT_PARENT = field(MTEHatchCraftingInputME.PatternSlot.class, "parentMTE");
    private static final Field GT_SLOTS = field(MTEHatchCraftingInputME.class, "internalInventory");
    private final Map<IMetaTileEntity, Map<Object, Integer>> slotIndexes = new IdentityHashMap<>();
    private final Map<IDualInputInventoryWithPattern, ProxyFactoryStatus.Issue> issues = new IdentityHashMap<>();

    private static Field field(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException failure) {
            throw new IllegalStateException("Unsupported pattern hatch layout: " + owner.getName(), failure);
        }
    }

    /** Internal pattern-array identity preserves real slot numbers, including holes and identical encoded patterns. */
    void record(IDualInputInventoryWithPattern inventory, ICraftingPatternDetails pattern, ItemStack source,
        CheckRecipeResult result) {
        if (result.wasSuccessful() || result.equals(CheckRecipeResultRegistry.NONE) || inventory.isEmpty()) {
            issues.remove(inventory);
            return;
        }
        if (pattern == null) return;
        if (result.equals(CheckRecipeResultRegistry.NO_RECIPE))
            result = gregtech.api.recipe.check.SimpleCheckRecipeResult.ofFailure("gtnhadjust_proxy_waiting_recipe");
        boolean extended = inventory instanceof SuperMTEHatchCraftingInputME.PatternSlot;
        try {
            IMetaTileEntity hatch = (IMetaTileEntity) (extended ? SUPER_PARENT : GT_PARENT).get(inventory);
            Object[] slots = (Object[]) (extended ? SUPER_SLOTS : GT_SLOTS).get(hatch);
            int i = slotIndex(hatch, slots, inventory);
            if (i < 0) return;
            ItemStack product = null;
            FluidStack fluid = null;
            for (IAEStack<?> output : pattern.getAEOutputs()) {
                if (output instanceof IAEItemStack item) product = item.getItemStack();
                else if (output instanceof IAEFluidStack liquid) fluid = liquid.getFluidStack();
                else continue;
                break;
            }
            IGregTechTileEntity tile = hatch.getBaseMetaTileEntity();
            issues.put(
                inventory,
                new ProxyFactoryStatus.Issue(
                    tile.getWorld().provider.dimensionId,
                    tile.getXCoord(),
                    tile.getYCoord(),
                    tile.getZCoord(),
                    i,
                    product,
                    fluid,
                    source,
                    result));
            return;
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot read physical pattern slot", failure);
        }
    }

    /** Build an identity index once per hatch layout instead of rescanning every slot for every failed pattern. */
    private int slotIndex(IMetaTileEntity hatch, Object[] slots, Object inventory) {
        Map<Object, Integer> indexes = slotIndexes.computeIfAbsent(hatch, unused -> new IdentityHashMap<>());
        Integer index = indexes.get(inventory);
        if (index == null || slots[index] != inventory) {
            indexes.clear();
            for (int i = 0; i < slots.length; i++) if (slots[i] != null) indexes.put(slots[i], i);
            index = indexes.get(inventory);
        }
        return index == null ? -1 : index;
    }

    List<ProxyFactoryStatus.Issue> snapshot() {
        issues.entrySet()
            .removeIf(entry -> !pending(entry.getKey(), entry.getValue().slot));
        List<ProxyFactoryStatus.Issue> result = new ArrayList<>(issues.values());
        result.sort(
            Comparator.comparingInt((ProxyFactoryStatus.Issue issue) -> issue.dimension)
                .thenComparingInt(issue -> issue.x)
                .thenComparingInt(issue -> issue.y)
                .thenComparingInt(issue -> issue.z)
                .thenComparingInt(issue -> issue.slot));
        return result;
    }

    /** A removed/replaced pattern must not leave an old launch warning behind. */
    private static boolean pending(IDualInputInventoryWithPattern inventory, int slot) {
        if (inventory.isEmpty()) return false;
        boolean extended = inventory instanceof SuperMTEHatchCraftingInputME.PatternSlot;
        try {
            IMetaTileEntity hatch = (IMetaTileEntity) (extended ? SUPER_PARENT : GT_PARENT).get(inventory);
            IGregTechTileEntity tile = hatch.getBaseMetaTileEntity();
            Object[] slots = (Object[]) (extended ? SUPER_SLOTS : GT_SLOTS).get(hatch);
            return tile != null && tile.getMetaTileEntity() == hatch && slots[slot] == inventory;
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot read physical pattern slot", failure);
        }
    }

    void clear() {
        issues.clear();
        slotIndexes.clear();
    }
}
