package com.gtnhadjust.mixins.structurelib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnhadjust.structurelib.ProjectorBuildBudget;

@Mixin(targets = "com.gtnewhorizon.structurelib.structure.SurvivalBuildStructureWalker", remap = false)
public abstract class ProjectorBuildBudgetWalkerMixin {

    @Shadow
    private int built;

    @Unique
    private int gtnhadjust$beforeVisit;

    /** A later piece cannot start placing after the click's shared budget is exhausted. */
    @Inject(method = "visit", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$checkPlacementBudget(CallbackInfoReturnable<Boolean> cir) {
        ProjectorBuildBudget budget = ProjectorBuildBudget.current();
        if (budget == null) return;
        gtnhadjust$beforeVisit = Math.max(0, built);
        if (budget.remaining() != 0) return;
        built = gtnhadjust$beforeVisit;
        cir.setReturnValue(false);
    }

    /** Native ACCEPT and ACCEPT_STOP both increase built; skips and failures do not use the budget. */
    @Inject(method = "visit", at = @At("RETURN"), cancellable = true)
    private void gtnhadjust$countSuccessfulPlacement(CallbackInfoReturnable<Boolean> cir) {
        ProjectorBuildBudget budget = ProjectorBuildBudget.current();
        if (budget != null && built > gtnhadjust$beforeVisit && !budget.placedBlock()) cir.setReturnValue(false);
    }
}
