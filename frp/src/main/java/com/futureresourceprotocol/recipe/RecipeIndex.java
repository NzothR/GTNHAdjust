package com.futureresourceprotocol.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.config.Configuration;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;

import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/** Rebuilt after registration. Unknown recipe systems are deliberately denied. */
public final class RecipeIndex {

    private static final Map<String, Boolean> QUALIFIED = new HashMap<>();
    private static final Map<String, Long> VOLTAGES = new HashMap<>();
    private static final Map<String, Boolean> OVERRIDES = new HashMap<>();
    private static String[] allowed = new String[0];
    private static String[] overrides = new String[0];
    private static boolean allowAll;

    private RecipeIndex() {}

    public static void configure(Configuration config) {
        allowAll = config.getBoolean(
            "allowAllItems",
            "recipes",
            false,
            "Default for recipe and stage bypass; world command may override. UXV and higher voltage and blacklist remain forbidden. Restart to change default.");
        allowed = config.getStringList(
            "additionalEligible",
            "recipes",
            new String[0],
            "Additional outputs as modid:item@meta. Restart required.");
        overrides = config.getStringList(
            "classOverrides",
            "recipes",
            new String[0],
            "Class overrides as modid:item@meta=UHV or =UXV. Restart required; only future advances change class.");
    }

    private static void add(ItemStack stack, long voltage) {
        Boolean uxv = voltage > 0 ? Boolean.valueOf(EligibilityPolicy.isUxvClass(voltage, GTValues.V[8])) : null;
        String key = WorldProtocol.key(stack);
        if (key == null) return;
        if (voltage > 0) VOLTAGES.putIfAbsent(key, voltage);
        if (!QUALIFIED.containsKey(key)) QUALIFIED.put(key, uxv);
        else if (QUALIFIED.get(key) == null && uxv != null) QUALIFIED.put(key, uxv);
    }

    public static void rebuild() {
        QUALIFIED.clear();
        VOLTAGES.clear();
        OVERRIDES.clear();
        FutureResourceProtocol.debug("building recipe index: start crafting recipes, allowAllItems={}", allowAll);
        try {
            for (Object value : CraftingManager.getInstance()
                .getRecipeList()) {
                if (value instanceof IRecipe) {
                    try {
                        add(((IRecipe) value).getRecipeOutput(), 0);
                    } catch (RuntimeException | LinkageError ex) {
                        FutureResourceProtocol.debug(
                            "crafting recipe output unavailable, recipe={} error={}",
                            value.getClass()
                                .getName(),
                            ex.toString());
                    }
                }
            }
        } catch (RuntimeException | LinkageError ex) {
            FutureResourceProtocol.LOG.error("FRP crafting recipe scan failed: {}", ex.toString());
        }
        FutureResourceProtocol.debug("building recipe index: crafting outputs={}, start furnace", QUALIFIED.size());
        try {
            for (Object value : FurnaceRecipes.smelting()
                .getSmeltingList()
                .values()) {
                if (value instanceof ItemStack) add((ItemStack) value, 0);
            }
        } catch (RuntimeException | LinkageError ex) {
            FutureResourceProtocol.LOG.error("FRP furnace recipe scan failed: {}", ex.toString());
        }
        FutureResourceProtocol.debug("building recipe index: furnace outputs={}, start GT", QUALIFIED.size());
        int mapCount = 0;
        int recipes = 0;
        try {
            List<String> maps = new ArrayList<>(RecipeMap.ALL_RECIPE_MAPS.keySet());
            Collections.sort(maps);
            mapCount = maps.size();
            for (String name : maps) {
                try {
                    List<GTRecipe> sorted = new ArrayList<>(
                        RecipeMap.ALL_RECIPE_MAPS.get(name)
                            .getAllRecipes());
                    sorted.sort((a, b) -> fingerprint(a).compareTo(fingerprint(b)));
                    for (GTRecipe recipe : sorted) {
                        if (!recipe.mEnabled || recipe.mFakeRecipe || recipe.mOutputs == null) continue;
                        recipes++;
                        for (ItemStack out : recipe.mOutputs) add(out, recipe.mEUt);
                    }
                } catch (RuntimeException | LinkageError ex) {
                    FutureResourceProtocol.LOG.warn("FRP skipped GT recipe map {}: {}", name, ex.toString());
                }
            }
        } catch (RuntimeException | LinkageError ex) {
            FutureResourceProtocol.LOG.error("FRP GT recipe scan failed: {}", ex.toString());
        }
        for (String key : allowed) if (!key.trim()
            .isEmpty() && !QUALIFIED.containsKey(key.trim())) QUALIFIED.put(key.trim(), null);
        for (String value : overrides) {
            int equals = value.lastIndexOf('=');
            if (equals < 0) {
                FutureResourceProtocol.LOG.warn("Invalid FRP class override {}", value);
                continue;
            }
            String stage = value.substring(equals + 1)
                .trim();
            if (!stage.equalsIgnoreCase("UHV") && !stage.equalsIgnoreCase("UXV")) {
                FutureResourceProtocol.LOG.warn("Invalid FRP class override {}", value);
                continue;
            }
            OVERRIDES.put(
                value.substring(0, equals)
                    .trim(),
                stage.equalsIgnoreCase("UXV"));
        }
        FutureResourceProtocol.LOG.info(
            "FRP recipe index: {} outputs, {} GT maps, {} GT recipes, {} overrides",
            QUALIFIED.size(),
            mapCount,
            recipes,
            OVERRIDES.size());
    }

    private static String fingerprint(GTRecipe recipe) {
        StringBuilder out = new StringBuilder();
        if (recipe.mInputs != null) for (ItemStack input : recipe.mInputs) out.append(WorldProtocol.key(input))
            .append(';');
        out.append('|')
            .append(recipe.mEUt)
            .append('|')
            .append(recipe.mDuration);
        return out.toString();
    }

    public static boolean forbidden(ItemStack stack) {
        return stack != null && ((FutureResourceProtocol.hullLockToken != null
            && stack.getItem() == FutureResourceProtocol.hullLockToken)
            || ItemList.Hull_MAX.isStackEqual(stack, false, true)
            || ItemList.Hull_UXV.isStackEqual(stack, false, true));
    }

    public static boolean defaultAllowAll() {
        return allowAll;
    }

    public static boolean eligible(ItemStack stack, WorldProtocol world) {
        String key = WorldProtocol.key(stack);
        Long voltage = VOLTAGES.get(key);
        boolean blocked = forbidden(stack);
        boolean aboveLimit = voltage != null && EligibilityPolicy.exceedsUxvLimit(voltage, GTValues.V[12]);
        boolean allowed = EligibilityPolicy
            .allows(key != null, blocked, QUALIFIED.containsKey(key), world.allowAllItems(), aboveLimit);
        if (!allowed) FutureResourceProtocol.debug(
            "eligibility denied key={} recipe={} voltage={} aboveUxvLimit={} blacklist={} bypass={}",
            key,
            QUALIFIED.containsKey(key),
            voltage,
            aboveLimit,
            blocked,
            world.allowAllItems());
        return allowed;
    }

    public static boolean classify(ItemStack stack, WorldProtocol world) {
        String key = WorldProtocol.key(stack);
        if (OVERRIDES.containsKey(key)) return OVERRIDES.get(key);
        Boolean cached = world.cachedClass(key);
        if (cached != null) return cached;
        return Boolean.TRUE.equals(QUALIFIED.get(key));
    }
}
