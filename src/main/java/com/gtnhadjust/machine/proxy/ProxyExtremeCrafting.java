package com.gtnhadjust.machine.proxy;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.common.me.CircuitRecipeInputs;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/** Optional TST extreme-crafting bridge; only already registered, locator-provided conversion pools are used. */
final class ProxyExtremeCrafting {

    private static final String CURRENT = "tst.recipe.VisualExtremeCraftRecipeMap.name";
    private static final String LEGACY = "tst.common.recipe.VisualExtremeCraftRecipeMap.name";

    private ProxyExtremeCrafting() {}

    static boolean isMap(RecipeMap<?> map) {
        return map != null && isName(map.unlocalizedName);
    }

    private static boolean isName(String name) {
        return CURRENT.equals(name) || LEGACY.equals(name);
    }

    /** Infer only this pool for unannotated processing patterns, and normalize its known historical identifier. */
    static RecipeMap<?> resolve(ProxyFactory controller, String name, ICraftingPatternDetails pattern) {
        if (!GTNHAdjust.proxyExtremeCrafting || pattern == null
            || pattern.isCraftable()
            || name != null && !isName(name)) return null;
        for (String id : new String[] { CURRENT, LEGACY }) {
            RecipeMap<?> map = RecipeMap.ALL_RECIPE_MAPS.get(id);
            if (controller.isRecipeMapUnlocked(map)) return map;
        }
        return null;
    }

    /**
     * Verify whole encoded batches on copies, including ore alternatives, exact quantities, output identity and NBT.
     */
    static boolean matches(GTRecipe recipe, ICraftingPatternDetails pattern) {
        if (pattern == null || pattern.isCraftable() || pattern.isInputOnly()) return false;
        IAEStack<?>[] outputs = pattern.getCondensedAEOutputs();
        if (outputs.length != 1 || !(outputs[0] instanceof IAEItemStack output)
            || recipe.mOutputs == null
            || recipe.mOutputs.length != 1
            || recipe.mOutputs[0] == null) return false;
        ItemStack result = recipe.mOutputs[0];
        ItemStack expected = output.getItemStack();
        if (!result.isItemEqual(expected) || !ItemStack.areItemStackTagsEqual(result, expected)) return false;
        IAEStack<?>[] encodedInputs = pattern.getCondensedAEInputs();
        ItemStack[] inputs = new ItemStack[encodedInputs.length];
        for (int i = 0; i < inputs.length; i++) {
            if (!(encodedInputs[i] instanceof IAEItemStack item)) return false;
            inputs[i] = item.getItemStack()
                .copy();
        }
        FluidStack[] fluids = new FluidStack[0];
        long batches = CircuitRecipeInputs.quantityBatches(recipe, inputs, fluids);
        if (batches <= 0 || batches > Integer.MAX_VALUE || output.getStackSize() != (long) result.stackSize * batches)
            return false;
        if (!recipe.isRecipeInputEqual(true, false, (int) batches, fluids, inputs)) return false;
        for (ItemStack remaining : inputs) if (remaining != null && remaining.stackSize > 0) return false;
        return true;
    }
}
