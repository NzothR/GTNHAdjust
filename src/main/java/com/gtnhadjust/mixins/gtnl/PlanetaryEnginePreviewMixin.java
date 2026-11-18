package com.gtnhadjust.mixins.gtnl;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizon.structurelib.ClientProxy;
import com.gtnhadjust.structurelib.PlanetaryEngineOutline;
import com.gtnhadjust.structurelib.PlanetaryEngineProjector.Preview;

@Mixin(value = ClientProxy.class, remap = false)
public abstract class PlanetaryEnginePreviewMixin implements Preview {

    @Shadow
    @Final
    private static List<Object> allGroups;
    @Unique
    private WeakReference<Object> gtnhadjust$enginePreview;
    @Unique
    private boolean gtnhadjust$captureEnginePreview;
    @Unique
    private Method gtnhadjust$removeGroup;

    /** Use StructureLib's cleanup so the old preview is removed from both rendering and coordinate lookup. */
    @Override
    public void gtnhadjust$replaceEnginePreview(World world, AxisAlignedBB outline) {
        Object previous = gtnhadjust$enginePreview == null ? null : gtnhadjust$enginePreview.get();
        if (previous != null && allGroups.remove(previous)) {
            try {
                if (gtnhadjust$removeGroup == null) {
                    // The parameter is StructureLib's private HintGroup type.
                    gtnhadjust$removeGroup = ClientProxy.class.getDeclaredMethod("removeGroup", previous.getClass());
                    gtnhadjust$removeGroup.setAccessible(true);
                }
                gtnhadjust$removeGroup.invoke(null, previous);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Cannot remove the previous engine projector section", failure);
            }
        }
        gtnhadjust$enginePreview = null;
        PlanetaryEngineOutline.show(world, outline);
        gtnhadjust$captureEnginePreview = outline == null;
    }

    /** The hint cube has already been registered here; skip creating its decorative client particle. */
    @Inject(
        method = "hintParticleTinted(Lnet/minecraft/world/World;III[Lnet/minecraft/util/IIcon;[S)V",
        at = @At(
            value = "FIELD",
            target = "Lcom/gtnewhorizon/structurelib/ClientProxy;allHintsDirty:Z",
            opcode = Opcodes.PUTSTATIC,
            shift = At.Shift.AFTER),
        cancellable = true)
    private void gtnhadjust$skipEnginePreviewParticles(CallbackInfo ci) {
        if (gtnhadjust$captureEnginePreview) ci.cancel();
    }

    @Redirect(method = "endHinting", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean gtnhadjust$captureEngineGroup(List<Object> groups, Object group) {
        if (gtnhadjust$captureEnginePreview) gtnhadjust$enginePreview = new WeakReference<>(group);
        return groups.add(group);
    }

    @Inject(method = "endHinting", at = @At("RETURN"))
    private void gtnhadjust$finishEngineHinting(World world, CallbackInfo ci) {
        if (world.isRemote) gtnhadjust$captureEnginePreview = false;
    }
}
