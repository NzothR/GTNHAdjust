package com.gtnhadjust.mixins.fix;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnhadjust.recipe.GtsrRegistrationScope;

/** Scope only GTSR's own registration callback, including exception paths. */
@Mixin(targets = "com.miaokatze.gtsr.loader.GTSRRecipeLoader", remap = false)
public abstract class GtsrRegistrationScopeMixin {

    @Redirect(
        method = "registerGroup",
        at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V"),
        require = 1,
        remap = false)
    private static void gtnhadjust$registrationScope(Runnable action) {
        GtsrRegistrationScope.run(action);
    }
}
