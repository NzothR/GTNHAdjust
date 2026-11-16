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

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import fox.spiteful.avaritia.crafting.ExtremeCraftingManager;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.util.GTRecipe;

/** Temporarily remove stargate-base recipes while either world debt class remains unpaid. */
public final class HullRecipeLocks {

    public static final int STARGATE = 1;
    private static final RecipeListSnapshot<IRecipe> CRAFT_REMOVED = new RecipeListSnapshot<>();
    private static final RecipeListSnapshot<IRecipe> EXTREME_REMOVED = new RecipeListSnapshot<>();
    private static final Map<RecipeMapBackend, List<GTRecipe>> GT_REMOVED = new LinkedHashMap<>();
    // Machines may retain recipe objects after the backend removes them from its lookup structures.
    private static final Map<GTRecipe, Boolean> GT_ENABLED = new IdentityHashMap<>();
    private static WorldProtocol serverWorld;
    private static int serverMask;

    private HullRecipeLocks() {}

    /** Match the GTNH 2.9.0 base block exactly, leaving other stargate components unaffected. */
    public static boolean isStargateBase(ItemStack stack) {
        return stack != null && stack.getItemDamage() == 0
            && stack.getItem() == GameRegistry.findItem("SGCraft", "stargateBase");
    }

    public static int targetMask(ItemStack stack) {
        return isStargateBase(stack) ? STARGATE : 0;
    }

    /** Derive the lock from existing ledger fields without migrating or modifying saved world data. */
    public static int snapshot(WorldProtocol world) {
        return world.isValid() && world.stageDebt(false) == 0 && world.stageDebt(true) == 0 ? 0 : STARGATE;
    }

    public static int serverMask() {
        return serverMask;
    }

    public static void apply(WorldProtocol world) {
        serverWorld = world;
        serverMask = snapshot(world);
        applyMask(serverMask);
        FutureResourceProtocol.LOG.info(
            "FRP stargate-base snapshot UHV debt={} UXV debt={} terminated={} mask={}",
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
                int tier = targetMask(output);
                if ((tier & mask) == 0) return false;
                FutureResourceProtocol.debug(
                    "removing stargate-base crafting tier={} recipe={} output={}",
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
        int extreme = 0;
        if (Loader.isModLoaded("Avaritia")) extreme = EXTREME_REMOVED.remove(
            ExtremeCraftingManager.getInstance()
                .getRecipeList(),
            recipe -> recipe != null && (targetMask(recipe.getRecipeOutput()) & mask) != 0);
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
                        int tier = targetMask(output);
                        if ((tier & mask) != 0) {
                            matched.add(recipe);
                            FutureResourceProtocol.debug(
                                "removing stargate-base machine recipe map={} tier={} output={}",
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
                FutureResourceProtocol.LOG
                    .error("FRP failed to remove stargate-base recipes from {}", pair.getKey(), ex);
            }
        }
        FutureResourceProtocol.LOG.info(
            "FRP temporarily removed stargate-base recipes: mask={} crafting={} extreme={} GT={}",
            mask,
            craft,
            extreme,
            machine);
    }

    public static void restore() {
        CRAFT_REMOVED.restore(craftingRecipes());
        if (Loader.isModLoaded("Avaritia")) EXTREME_REMOVED.restore(
            ExtremeCraftingManager.getInstance()
                .getRecipeList());
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
                    FutureResourceProtocol.LOG.error("FRP failed to restore a stargate-base recipe", ex);
                }
            }
        }
        GT_REMOVED.clear();
        FutureResourceProtocol.debug("restored temporarily removed GT stargate-base recipes={}", count);
    }
}
