package com.gtnhadjust.mixins.gtnl;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnhadjust.structurelib.PlanetaryEngineProjector;

@Mixin(value = ConstructableUtility.class, remap = false)
public abstract class PlanetaryEngineProjectorMixin {

    /** Keep StructureLib's outer start/end hinting calls around the sectional preview. */
    @Inject(method = "handle0", at = @At("HEAD"), cancellable = true)
    private static void gtnhadjust$handleEngineSection(ItemStack stack, EntityPlayer player, World world, int x, int y,
        int z, int side, CallbackInfoReturnable<Boolean> cir) {
        if (PlanetaryEngineProjector.handle(stack, player, world, x, y, z)) cir.setReturnValue(false);
    }
}
