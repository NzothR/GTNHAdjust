package com.gtnhadjust.mixins.gtnl;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureUtility;
import com.gtnhadjust.structurelib.PlanetaryEngineProjector;
import com.gtnhadjust.structurelib.PlanetaryEngineStructure;
import com.science.gtnl.common.machine.multiblock.GenerationEarthEngine;

@Mixin(value = GenerationEarthEngine.class, remap = false)
public abstract class PlanetaryEngineStructureMixin implements PlanetaryEngineStructure.Provider {

    @Shadow
    @Final
    private static String[][] shape;

    @Shadow
    public abstract IStructureDefinition<GenerationEarthEngine> getStructureDefinition();

    @Unique
    private IStructureDefinition<GenerationEarthEngine> gtnhadjust$fullDefinition;
    @Unique
    private PlanetaryEngineStructure gtnhadjust$projectorStructure;
    @Unique
    private boolean gtnhadjust$paletteOnly;

    @Inject(method = "getStructureDefinition", at = @At("HEAD"), cancellable = true)
    private void gtnhadjust$useCachedDefinition(
        CallbackInfoReturnable<IStructureDefinition<GenerationEarthEngine>> cir) {
        if (gtnhadjust$fullDefinition != null) cir.setReturnValue(gtnhadjust$fullDefinition);
    }

    @Inject(method = "getStructureDefinition", at = @At("RETURN"))
    private void gtnhadjust$cacheDefinition(CallbackInfoReturnable<IStructureDefinition<GenerationEarthEngine>> cir) {
        if (!gtnhadjust$paletteOnly) gtnhadjust$fullDefinition = cir.getReturnValue();
    }

    /** The projector needs the native element bindings but can clip the layout before any shape compilation. */
    @Redirect(
        method = "getStructureDefinition",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/structurelib/structure/StructureUtility;transpose([[Ljava/lang/String;)[[Ljava/lang/String;"))
    private String[][] gtnhadjust$skipFullShapeForProjection(String[][] source) {
        return gtnhadjust$paletteOnly ? new String[0][] : StructureUtility.transpose(source);
    }

    @Redirect(
        method = "getStructureDefinition",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/structurelib/structure/StructureDefinition$Builder;build()Lcom/gtnewhorizon/structurelib/structure/IStructureDefinition;"))
    private IStructureDefinition<GenerationEarthEngine> gtnhadjust$registerProjectorChannels(
        StructureDefinition.Builder<GenerationEarthEngine> builder) {
        return builder.addChannel(PlanetaryEngineStructure.RENDER_CHANNEL, 1, 1)
            .addChannel(PlanetaryEngineStructure.PREVIEW_CHANNEL, 1, 2)
            .addChannel(PlanetaryEngineStructure.SECTION_CHANNEL, 1, Integer.MAX_VALUE)
            .build();
    }

    @Redirect(method = "survivalConstruct", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I"))
    private int gtnhadjust$useProjectorPlacementBudget(int cap, int scaled, ItemStack trigger, int budget,
        ISurvivalBuildEnvironment environment) {
        return PlanetaryEngineProjector.isEnabled(trigger) ? budget : Math.min(cap, scaled);
    }

    /** Let native placement finish without starting a synchronous full structure check. */
    @ModifyArgs(
        method = "survivalConstruct",
        at = @At(
            value = "INVOKE",
            target = "Lcom/science/gtnl/common/machine/multiblock/GenerationEarthEngine;survivalBuildPiece(Ljava/lang/String;Lnet/minecraft/item/ItemStack;IIIILcom/gtnewhorizon/structurelib/structure/ISurvivalBuildEnvironment;ZZ)I"))
    private void gtnhadjust$deferFullStructureCheck(Args args) {
        if (PlanetaryEngineProjector.isEnabled(args.get(1))) args.set(8, false);
    }

    @Inject(method = "survivalConstruct", at = @At("RETURN"))
    private void gtnhadjust$scheduleStructureCheck(ItemStack trigger, int budget, ISurvivalBuildEnvironment environment,
        CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() > 0 && PlanetaryEngineProjector.isEnabled(trigger))
            ((GenerationEarthEngine) (Object) this).setStructureUpdateTime(50);
    }

    @Override
    public PlanetaryEngineStructure gtnhadjust$getProjectorStructure() {
        if (gtnhadjust$projectorStructure == null) {
            gtnhadjust$paletteOnly = gtnhadjust$fullDefinition == null;
            try {
                gtnhadjust$projectorStructure = new PlanetaryEngineStructure(
                    shape,
                    (StructureDefinition<GenerationEarthEngine>) getStructureDefinition());
            } finally {
                gtnhadjust$paletteOnly = false;
            }
        }
        return gtnhadjust$projectorStructure;
    }
}
