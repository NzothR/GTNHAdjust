package com.gtnhadjust.mixins.structurelib;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnhadjust.structurelib.AeAutoPlaceItemSource;

@Mixin(value = IItemSource.class, remap = false)
public interface StructureLibAeAutoPlaceMixin {

    @Inject(method = "fromPlayer", at = @At("RETURN"), cancellable = true, remap = false)
    static void gtnhadjust$appendAeNetwork(EntityPlayerMP player, CallbackInfoReturnable<IItemSource> callback) {
        callback.setReturnValue(AeAutoPlaceItemSource.wrap(callback.getReturnValue(), player));
    }
}
