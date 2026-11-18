package com.gtnhadjust.recipe;

import java.util.Collection;

import com.science.gtnl.common.material.GTNLRecipeMaps;

import gregtech.api.enums.TierEU;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;

/** Tune the single planetary engine recipe after GTNR's recipe compilation modifiers have run. */
public final class RecombinationFusionRecipe {

    private RecombinationFusionRecipe() {}

    public static void apply() {
        Collection<GTRecipe> recipes = GTNLRecipeMaps.RecombinationFusionReactorRecipes.getAllRecipes();
        if (recipes.size() != 1) return;
        GTRecipe recipe = recipes.iterator()
            .next();
        recipe.mDuration = 64 * GTRecipeBuilder.SECONDS;
        recipe.mEUt = (int) TierEU.MAX;
    }
}
