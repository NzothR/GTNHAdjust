package com.gtnhadjust.mixins.fix;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.recipe.GtsrRecipeMapPolicy;
import com.gtnhadjust.recipe.GtsrRegistrationScope;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.util.GTRecipe;

@Mixin(value = RecipeMapBackend.class, remap = false)
public abstract class GtsrRecipeMapSpeedExclusionMixin {

    @Shadow
    private RecipeMap<?> recipeMap;

    @WrapMethod(method = "compileRecipe", remap = false)
    private GTRecipe gtnhadjust$preserveGtsrPoolDuration(GTRecipe recipe, Operation<GTRecipe> original) {
        if (recipe == null || recipeMap == null
            || !(GtsrRegistrationScope.active() || GtsrRecipeMapPolicy.isExclusive(recipeMap.unlocalizedName)))
            return original.call(recipe);
        int duration = recipe.mDuration;
        GTRecipe result = original.call(recipe);
        if (GTNHAdjust.debug && recipe.mDuration != duration) GTNHAdjust.LOG.info(
            "GTSR recipe in pool {}: restored duration from {} to {} ticks",
            recipeMap.unlocalizedName,
            recipe.mDuration,
            duration);
        recipe.mDuration = duration;
        if (result != null) result.mDuration = duration;
        return result;
    }
}
