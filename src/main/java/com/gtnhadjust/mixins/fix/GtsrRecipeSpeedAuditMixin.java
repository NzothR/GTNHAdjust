package com.gtnhadjust.mixins.fix;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnhadjust.GTNHAdjust;
import com.miaokatze.gtsr.common.dimension.prosperity.industrial.IndustrialRecipeLedger.Stage;
import com.xyp.gtneverreset.utils.recipes.RecipeSpeed;

/** GTSR validates the post-registration duration; GTNR changes it as recipes are registered. */
@Mixin(targets = "com.miaokatze.gtsr.common.dimension.prosperity.industrial.ProsperityIndustrialRecipes", remap = false)
public abstract class GtsrRecipeSpeedAuditMixin {

    @Redirect(
        method = "verifyRegistration",
        at = @At(
            value = "FIELD",
            target = "Lcom/miaokatze/gtsr/common/dimension/prosperity/industrial/IndustrialRecipeLedger$Stage;ticks:I",
            opcode = Opcodes.GETFIELD,
            ordinal = 0),
        require = 1,
        remap = false)
    private static int gtnhadjust$expectedDuration(Stage stage) {
        int adjusted = RecipeSpeed.getModifiedRecipeDuration(stage.ticks);
        if (GTNHAdjust.debug && adjusted != stage.ticks) GTNHAdjust.LOG
            .info("GTSR recipe {} audit duration: original={}, after GTNR speed={}", stage.id, stage.ticks, adjusted);
        return adjusted;
    }
}
