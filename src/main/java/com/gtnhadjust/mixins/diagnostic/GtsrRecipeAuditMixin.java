package com.gtnhadjust.mixins.diagnostic;

import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.GTNHAdjust;

import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.util.GTRecipe;

/** Reports the values checked by GTSR without modifying the recipes or the result of its audit. */
@Mixin(targets = "com.miaokatze.gtsr.common.dimension.prosperity.industrial.ProsperityIndustrialRecipes", remap = false)
public abstract class GtsrRecipeAuditMixin {

    @Redirect(
        method = "verifyRegistration",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/util/GTRecipe;getMetadataOrDefault(Lgregtech/api/recipe/RecipeMetadataKey;Ljava/lang/Object;)Ljava/lang/Object;"),
        remap = false)
    private static Object gtnhadjust$inspect(GTRecipe recipe, RecipeMetadataKey<?> key, Object defaultValue) {
        Object stage = gtnhadjust$metadata(recipe, key, defaultValue);
        if (GTNHAdjust.debug && "WS-1".equals(stage)) {
            GTNHAdjust.LOG.info(
                "GTSR audit WS-1 actual: EUt={}, ticks={}, itemsIn={}, itemsOut={}, fluidsIn={}, fluidsOut={}",
                recipe.mEUt,
                recipe.mDuration,
                Arrays.toString(recipe.mInputs),
                Arrays.toString(recipe.mOutputs),
                gtnhadjust$fluidsText(recipe.mFluidInputs),
                gtnhadjust$fluidsText(recipe.mFluidOutputs));
            GTNHAdjust.LOG.info(
                "GTSR audit WS-1 expected: EUt=120, ticks=100, no items; wastesigh 10000 mB -> native_acid_condensate 10000 mB");
        }
        return stage;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Object gtnhadjust$metadata(GTRecipe recipe, RecipeMetadataKey key, Object fallback) {
        return recipe.getMetadataOrDefault(key, fallback);
    }

    @Inject(method = "sameItems", at = @At("RETURN"), remap = false)
    private static void gtnhadjust$items(ItemStack[] actual, ItemStack[] expected,
        CallbackInfoReturnable<Boolean> result) {
        if (GTNHAdjust.debug && !result.getReturnValue()) GTNHAdjust.LOG.info(
            "GTSR audit item mismatch: actual={}, expected={}",
            Arrays.toString(actual),
            Arrays.toString(expected));
    }

    @Inject(method = "sameFluids", at = @At("RETURN"), remap = false)
    private static void gtnhadjust$fluids(FluidStack[] actual, FluidStack[] expected,
        CallbackInfoReturnable<Boolean> result) {
        if (GTNHAdjust.debug && !result.getReturnValue()) GTNHAdjust.LOG.info(
            "GTSR audit fluid mismatch: actual={}, expected={}",
            gtnhadjust$fluidsText(actual),
            gtnhadjust$fluidsText(expected));
    }

    private static String gtnhadjust$fluidsText(FluidStack[] fluids) {
        if (fluids == null) return "null";
        String[] values = new String[fluids.length];
        for (int i = 0; i < fluids.length; i++) {
            values[i] = fluids[i] == null ? "null"
                : fluids[i].writeToNBT(new NBTTagCompound())
                    .toString();
        }
        return Arrays.toString(values);
    }
}
