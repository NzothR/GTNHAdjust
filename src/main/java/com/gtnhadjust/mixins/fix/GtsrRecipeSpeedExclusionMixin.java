package com.gtnhadjust.mixins.fix;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnhadjust.GTNHAdjust;

import gregtech.api.interfaces.IRecipeMap;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;

/** Preserve GTSR's own industrial recipe durations after GTNR's global registration hook runs. */
@Mixin(targets = "com.miaokatze.gtsr.common.dimension.prosperity.industrial.ProsperityIndustrialRecipes", remap = false)
public abstract class GtsrRecipeSpeedExclusionMixin {

    @Redirect(
        method = "registerAll",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/util/GTRecipeBuilder;addTo(Lgregtech/api/interfaces/IRecipeMap;)Ljava/util/Collection;"),
        require = 1,
        remap = false)
    private static Collection<GTRecipe> gtnhadjust$preserveDuration(GTRecipeBuilder builder, IRecipeMap map) {
        int originalDuration = builder.getDuration();
        Collection<GTRecipe> registered = builder.addTo(map);
        for (GTRecipe recipe : registered) {
            if (recipe != null && recipe.mDuration != originalDuration) {
                if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                    "GTSR industrial recipe: restored duration from {} to {} ticks",
                    recipe.mDuration,
                    originalDuration);
                recipe.mDuration = originalDuration;
            }
        }
        return registered;
    }
}
