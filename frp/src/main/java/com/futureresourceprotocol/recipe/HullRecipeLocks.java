package com.futureresourceprotocol.recipe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;

import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/** Applies a world-start snapshot and restores the original objects when the server stops. */
public final class HullRecipeLocks {

    private static final Map<GTRecipe, Boolean> GT_ORIGINALS = new IdentityHashMap<>();
    private static final Map<ShapedOreRecipe, CraftSnapshot> CRAFT_ORIGINALS = new IdentityHashMap<>();
    private static final Field INPUT = field("input");
    private static final Field WIDTH = field("width");
    private static final Field HEIGHT = field("height");

    private HullRecipeLocks() {}

    private static Field field(String name) {
        try {
            Field field = ShapedOreRecipe.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access shaped recipe field " + name, ex);
        }
    }

    private static int tier(ItemStack stack) {
        if (stack == null) return -1;
        if (ItemList.Hull_MAX.isStackEqual(stack, false, true)) return 9;
        if (ItemList.Hull_UMV.isStackEqual(stack, false, true)) return 12;
        if (ItemList.Hull_UXV.isStackEqual(stack, false, true)) return 13;
        return -1;
    }

    private static List<ItemStack> requirements(WorldProtocol world, int tier) {
        List<ItemStack> result = new ArrayList<>();
        if ((tier == 9 || tier == 12) && !world.permitsHull(9))
            result.add(new ItemStack(FutureResourceProtocol.hullLockToken, 1, 0));
        if (tier == 12 && world.stageDebt(true) > 0)
            result.add(new ItemStack(FutureResourceProtocol.hullLockToken, 1, 1));
        if (tier == 13 && !world.permitsHull(13)) result.add(new ItemStack(FutureResourceProtocol.hullLockToken, 1, 2));
        return result;
    }

    public static void apply(WorldProtocol world) {
        restore();
        int craft = 0, gt = 0, unsupported = 0;
        for (Object entry : CraftingManager.getInstance()
            .getRecipeList()) {
            if (!(entry instanceof IRecipe)) continue;
            IRecipe recipe = (IRecipe) entry;
            try {
                int tier = tier(recipe.getRecipeOutput());
                if (tier < 0) continue;
                List<ItemStack> locks = requirements(world, tier);
                if (locks.isEmpty()) continue;
                if (!(recipe instanceof ShapedOreRecipe)) {
                    unsupported++;
                    FutureResourceProtocol.LOG.warn(
                        "FRP cannot modify hull recipe type={} tier={}",
                        recipe.getClass()
                            .getName(),
                        tier);
                    continue;
                }
                ShapedOreRecipe shaped = (ShapedOreRecipe) recipe;
                Object[] old = (Object[]) INPUT.get(shaped);
                int width = WIDTH.getInt(shaped), height = HEIGHT.getInt(shaped);
                if (width <= 0 || width > 3 || height <= 0 || height > 3 || old == null) {
                    unsupported++;
                    continue;
                }
                Object[] changed = Arrays.copyOf(old, width * 3);
                int placed = 0;
                for (int i = 0; i < changed.length && placed < locks.size(); i++) {
                    if (changed[i] == null) changed[i] = locks.get(placed++);
                }
                if (placed != locks.size()) {
                    unsupported++;
                    FutureResourceProtocol.LOG.warn(
                        "FRP hull recipe has no space for {} lock ingredients: type={} tier={}",
                        locks.size(),
                        recipe.getClass()
                            .getName(),
                        tier);
                    continue;
                }
                CRAFT_ORIGINALS.put(shaped, new CraftSnapshot(old, height));
                INPUT.set(shaped, changed);
                HEIGHT.setInt(shaped, 3);
                craft++;
            } catch (ReflectiveOperationException | RuntimeException ex) {
                unsupported++;
                FutureResourceProtocol.LOG.error("FRP failed to lock a crafting recipe: {}", ex.toString());
            }
        }
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            try {
                for (GTRecipe recipe : map.getAllRecipes()) {
                    if (!recipe.mEnabled || recipe.mFakeRecipe || recipe.mOutputs == null) continue;
                    boolean locked = false;
                    for (ItemStack output : recipe.mOutputs) {
                        int tier = tier(output);
                        if (tier >= 0 && !world.permitsHull(tier)) {
                            locked = true;
                            break;
                        }
                    }
                    if (locked && !GT_ORIGINALS.containsKey(recipe)) {
                        GT_ORIGINALS.put(recipe, recipe.mEnabled);
                        recipe.mEnabled = false;
                        gt++;
                    }
                }
            } catch (RuntimeException | LinkageError ex) {
                unsupported++;
                FutureResourceProtocol.LOG.error("FRP failed to inspect a GT recipe map: {}", ex.toString());
            }
        }
        FutureResourceProtocol.LOG.info(
            "FRP hull snapshot applied: UHV debt={}, UMV debt={}, terminated={}, crafting locked={}, GT locked={}, unsupported={}; restart to refresh",
            world.stageDebt(false),
            world.stageDebt(true),
            world.isTerminated(),
            craft,
            gt,
            unsupported);
        if (unsupported > 0) FutureResourceProtocol.LOG
            .error("FRP found {} unsupported recipe entries or maps; production may bypass the snapshot", unsupported);
    }

    public static void restore() {
        for (Map.Entry<GTRecipe, Boolean> entry : GT_ORIGINALS.entrySet()) entry.getKey().mEnabled = entry.getValue();
        GT_ORIGINALS.clear();
        for (Map.Entry<ShapedOreRecipe, CraftSnapshot> entry : CRAFT_ORIGINALS.entrySet()) {
            try {
                INPUT.set(entry.getKey(), entry.getValue().input);
                HEIGHT.setInt(entry.getKey(), entry.getValue().height);
            } catch (ReflectiveOperationException ex) {
                FutureResourceProtocol.LOG.error("FRP failed to restore a hull recipe: {}", ex.toString());
            }
        }
        CRAFT_ORIGINALS.clear();
    }

    private static final class CraftSnapshot {

        final Object[] input;
        final int height;

        CraftSnapshot(Object[] input, int height) {
            this.input = input;
            this.height = height;
        }
    }
}
