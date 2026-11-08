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
    private static final Map<String, Boolean> OVERRIDES = new HashMap<>();
    private static String[] allowed = new String[0];
    private static String[] overrides = new String[0];

    private RecipeIndex() {}

    public static void configure(Configuration config) {
        allowed = config.getStringList(
            "additionalEligible",
            "recipes",
            new String[0],
            "Additional outputs as modid:item@meta. Restart required.");
        overrides = config.getStringList(
            "classOverrides",
            "recipes",
            new String[0],
            "Class overrides as modid:item@meta=UHV or =UMV. Restart required; only future advances change class.");
    }

    private static void add(ItemStack stack, Boolean umv) {
        String key = WorldProtocol.key(stack);
        if (key == null) return;
        if (!QUALIFIED.containsKey(key)) QUALIFIED.put(key, umv);
        else if (QUALIFIED.get(key) == null && umv != null) QUALIFIED.put(key, umv);
    }

    public static void rebuild() {
        QUALIFIED.clear();
        OVERRIDES.clear();
        for (Object value : CraftingManager.getInstance()
            .getRecipeList()) {
            if (value instanceof IRecipe) {
                try {
                    add(((IRecipe) value).getRecipeOutput(), null);
                } catch (RuntimeException ex) {
                    FutureResourceProtocol.debug("crafting recipe output unavailable: {}", ex.toString());
                }
            }
        }
        for (Object value : FurnaceRecipes.smelting()
            .getSmeltingList()
            .values()) if (value instanceof ItemStack) add((ItemStack) value, null);
        List<String> maps = new ArrayList<>(RecipeMap.ALL_RECIPE_MAPS.keySet());
        Collections.sort(maps);
        int recipes = 0;
        for (String name : maps) {
            List<GTRecipe> sorted = new ArrayList<>(
                RecipeMap.ALL_RECIPE_MAPS.get(name)
                    .getAllRecipes());
            sorted.sort((a, b) -> fingerprint(a).compareTo(fingerprint(b)));
            for (GTRecipe recipe : sorted) {
                if (!recipe.mEnabled || recipe.mFakeRecipe || recipe.mOutputs == null) continue;
                recipes++;
                Boolean stage = recipe.mEUt > 0 ? recipe.mEUt >= GTValues.V[9] : null;
                for (ItemStack out : recipe.mOutputs) add(out, stage);
            }
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
            if (!stage.equalsIgnoreCase("UHV") && !stage.equalsIgnoreCase("UMV")) {
                FutureResourceProtocol.LOG.warn("Invalid FRP class override {}", value);
                continue;
            }
            OVERRIDES.put(
                value.substring(0, equals)
                    .trim(),
                stage.equalsIgnoreCase("UMV"));
        }
        FutureResourceProtocol.LOG.info(
            "FRP recipe index: {} outputs, {} GT maps, {} GT recipes, {} overrides",
            QUALIFIED.size(),
            maps.size(),
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
        return stack != null
            && (ItemList.Hull_MAX.isStackEqual(stack, false, true) || ItemList.Hull_UMV.isStackEqual(stack, false, true)
                || ItemList.Hull_UXV.isStackEqual(stack, false, true));
    }

    public static boolean eligible(ItemStack stack) {
        String key = WorldProtocol.key(stack);
        return key != null && !forbidden(stack) && QUALIFIED.containsKey(key);
    }

    public static boolean classify(ItemStack stack, WorldProtocol world) {
        String key = WorldProtocol.key(stack);
        if (OVERRIDES.containsKey(key)) return OVERRIDES.get(key);
        Boolean cached = world.cachedClass(key);
        if (cached != null) return cached;
        return Boolean.TRUE.equals(QUALIFIED.get(key));
    }
}
