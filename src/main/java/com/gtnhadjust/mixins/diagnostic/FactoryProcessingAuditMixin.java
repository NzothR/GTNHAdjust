package com.gtnhadjust.mixins.diagnostic;

import java.util.Arrays;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.GTNHAdjust;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.util.GTRecipe;
import gregtech.common.tileentities.machines.IDualInputInventoryWithPattern;

@Mixin(targets = "com.xyp.gtneverreset.common.machine.multiblock.UniversalJointFactoryProcessingLogic", remap = false)
public abstract class FactoryProcessingAuditMixin extends ProcessingLogic {

    @Shadow
    private ICraftingPatternDetails pattern;
    @Shadow
    private RecipeMap<?> patternRecipeMap;

    @Inject(method = "tryCachePossibleRecipesFromPattern", at = @At("RETURN"), remap = false)
    private void gtnhadjust$pattern(IDualInputInventoryWithPattern inventory, CallbackInfoReturnable<Boolean> cir) {
        GTNHAdjust.LOG.info(
            "[craft-audit] factory pattern accepted={} map={} patternInputs={} patternOutputs={}",
            cir.getReturnValue(),
            patternRecipeMap == null ? null : patternRecipeMap.unlocalizedName,
            pattern == null ? null : Arrays.toString(pattern.getAEInputs()),
            pattern == null ? null : Arrays.toString(pattern.getAEOutputs()));
    }

    @Inject(method = "findRecipeMatches", at = @At("RETURN"), cancellable = true, remap = false)
    private void gtnhadjust$search(RecipeMap<?> selected, CallbackInfoReturnable<Stream<GTRecipe>> cir) {
        GTNHAdjust.LOG.info(
            "[craft-audit] factory search selected={} patternMap={} items={} fluids={}",
            selected == null ? null : selected.unlocalizedName,
            patternRecipeMap == null ? null : patternRecipeMap.unlocalizedName,
            Arrays.toString(inputItems),
            Arrays.toString(inputFluids));
        cir.setReturnValue(
            cir.getReturnValue()
                .peek(
                    recipe -> GTNHAdjust.LOG.info(
                        "[craft-audit] factory match items={} fluids={} outputs={} duration={} eut={}",
                        Arrays.toString(recipe.mInputs),
                        Arrays.toString(recipe.mFluidInputs),
                        Arrays.toString(recipe.mOutputs),
                        recipe.mDuration,
                        recipe.mEUt)));
    }

    @Inject(method = "processRecipe", at = @At("RETURN"), remap = false)
    private void gtnhadjust$result(CallbackInfoReturnable<CheckRecipeResult> cir) {
        GTNHAdjust.LOG.info("[craft-audit] factory result={}", cir.getReturnValue());
    }
}
