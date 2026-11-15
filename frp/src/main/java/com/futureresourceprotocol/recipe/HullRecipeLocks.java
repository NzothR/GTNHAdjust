package com.futureresourceprotocol.recipe;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;

import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.util.GTRecipe;

/** Temporarily remove exact hull outputs according to the current world's permissions. */
public final class HullRecipeLocks {

    public static final int UHV = 1, UMV = 2, UXV = 4;
    private static final RecipeListSnapshot<IRecipe> CRAFT_REMOVED = new RecipeListSnapshot<>();
    private static final Map<RecipeMapBackend, List<GTRecipe>> GT_REMOVED = new LinkedHashMap<>();
    // Machines may retain recipe objects after the backend removes them from its lookup structures.
    private static final Map<GTRecipe, Boolean> GT_ENABLED = new IdentityHashMap<>();
    private static WorldProtocol serverWorld;
    private static int serverMask;

    private HullRecipeLocks() {}

    private static boolean sameItem(ItemStack stack, ItemStack target) {
        return stack != null && target != null
            && HullIdentity.matches(stack.getItem(), stack.getItemDamage(), target.getItem(), target.getItemDamage());
    }

    public static int tier(ItemStack stack) {
        if (stack == null) return 0;
        if (sameItem(stack, ItemList.Hull_MAX.get(1))) return UHV;
        if (sameItem(stack, ItemList.Hull_UMV.get(1))) return UMV;
        if (sameItem(stack, ItemList.Hull_UXV.get(1))) return UXV;
        return 0;
    }

    public static int snapshot(WorldProtocol world) {
        return (world.permitsHull(9) ? 0 : UHV) | (world.permitsHull(12) ? 0 : UMV) | (world.permitsHull(13) ? 0 : UXV);
    }

    public static int serverMask() {
        return serverMask;
    }

    public static void apply(WorldProtocol world) {
        serverWorld = world;
        serverMask = snapshot(world);
        applyMask(serverMask);
        FutureResourceProtocol.LOG.info(
            "FRP hull snapshot UHV debt={} UMV debt={} terminated={} mask={}",
            world.stageDebt(false),
            world.stageDebt(true),
            world.isTerminated(),
            serverMask);
    }

    /** Update production and every connected client before a ledger mutation returns. */
    public static void refresh(WorldProtocol world) {
        if (world != serverWorld) return;
        int mask = snapshot(world);
        if (mask == serverMask) return;
        apply(world);
        FutureResourceProtocol.NETWORK.sendToAll(new HullLockMessage(true, serverMask));
    }

    /** Release the server ledger so another single-player world starts with its own permissions. */
    public static void stop() {
        serverWorld = null;
        serverMask = 0;
        restore();
    }

    private static List<IRecipe> craftingRecipes() {
        return CraftingManager.getInstance()
            .getRecipeList();
    }

    public static void applyMask(int mask) {
        restore();
        int craft = CRAFT_REMOVED.remove(craftingRecipes(), recipe -> {
            if (recipe == null) return false;
            try {
                ItemStack output = recipe.getRecipeOutput();
                int tier = tier(output);
                if ((tier & mask) == 0) return false;
                FutureResourceProtocol.debug(
                    "removing hull crafting tier={} recipe={} output={}",
                    tier,
                    recipe.getClass()
                        .getName(),
                    WorldProtocol.key(output));
                return true;
            } catch (RuntimeException | LinkageError ex) {
                FutureResourceProtocol.LOG.error(
                    "FRP could not inspect crafting recipe {}",
                    recipe.getClass()
                        .getName(),
                    ex);
                return false;
            }
        });
        int machine = 0;
        for (Map.Entry<String, ? extends RecipeMap<?>> pair : RecipeMap.ALL_RECIPE_MAPS.entrySet()) {
            try {
                RecipeMapBackend backend = pair.getValue()
                    .getBackend();
                if (GT_REMOVED.containsKey(backend)) continue;
                List<GTRecipe> matched = new ArrayList<>();
                for (GTRecipe recipe : backend.getAllRecipes()) {
                    if (recipe.mOutputs == null) continue;
                    for (ItemStack output : recipe.mOutputs) {
                        int tier = tier(output);
                        if ((tier & mask) != 0) {
                            matched.add(recipe);
                            FutureResourceProtocol.debug(
                                "removing hull machine recipe map={} tier={} output={}",
                                pair.getKey(),
                                tier,
                                WorldProtocol.key(output));
                            break;
                        }
                    }
                }
                if (!matched.isEmpty()) {
                    GT_REMOVED.put(backend, matched);
                    for (GTRecipe recipe : matched) {
                        GT_ENABLED.putIfAbsent(recipe, recipe.mEnabled);
                        recipe.mEnabled = false;
                    }
                    backend.removeRecipes(matched);
                    machine += matched.size();
                }
            } catch (RuntimeException | LinkageError ex) {
                FutureResourceProtocol.LOG.error("FRP failed to remove hull recipes from {}", pair.getKey(), ex);
            }
        }
        FutureResourceProtocol.LOG
            .info("FRP temporarily removed hull recipes: mask={} crafting={} GT={}", mask, craft, machine);
    }

    public static void restore() {
        CRAFT_REMOVED.restore(craftingRecipes());
        GT_ENABLED.forEach((recipe, enabled) -> recipe.mEnabled = enabled);
        GT_ENABLED.clear();
        int count = 0;
        for (Map.Entry<RecipeMapBackend, List<GTRecipe>> pair : GT_REMOVED.entrySet()) {
            for (GTRecipe recipe : pair.getValue()) {
                try {
                    if (pair.getKey()
                        .getAllRecipes()
                        .contains(recipe)) continue;
                    pair.getKey()
                        .compileRecipe(recipe);
                    count++;
                } catch (RuntimeException | LinkageError ex) {
                    FutureResourceProtocol.LOG.error("FRP failed to restore a hull recipe", ex);
                }
            }
        }
        GT_REMOVED.clear();
        FutureResourceProtocol.debug("restored temporarily removed GT hull recipes={}", count);
    }
}
