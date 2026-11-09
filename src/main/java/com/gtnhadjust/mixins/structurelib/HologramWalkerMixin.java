package com.gtnhadjust.mixins.structurelib;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElement.PlaceResult;
import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.structurelib.HologramReplacement;
import com.gtnhadjust.structurelib.HologramReplacementContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(targets = "com.gtnewhorizon.structurelib.structure.SurvivalBuildStructureWalker", remap = false)
public abstract class HologramWalkerMixin<T> {

    @WrapOperation(
        method = "visit",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/structurelib/structure/IStructureElement;survivalPlaceBlock(Ljava/lang/Object;Lnet/minecraft/world/World;IIILnet/minecraft/item/ItemStack;Lcom/gtnewhorizon/structurelib/structure/AutoPlaceEnvironment;)Lcom/gtnewhorizon/structurelib/structure/IStructureElement$PlaceResult;"))
    private PlaceResult gtnhadjust$replace(IStructureElement<T> element, T context, World world, int x, int y, int z,
        ItemStack stack, AutoPlaceEnvironment env, Operation<PlaceResult> original) {
        PlaceResult result;
        try {
            result = HologramReplacement.tryReplace(element, context, world, x, y, z, stack, env);
        } catch (RuntimeException failure) {
            GTNHAdjust.LOG.warn("[hologram] replacement failed at {},{},{}", x, y, z, failure);
            return PlaceResult.REJECT;
        }
        if (result != null) return result;
        PlaceResult placed = original.call(element, context, world, x, y, z, stack, env);
        HologramReplacementContext scope = HologramReplacementContext.current();
        if (scope != null && (placed == PlaceResult.ACCEPT || placed == PlaceResult.ACCEPT_STOP)) {
            scope.attempted = true;
            scope.changed = true;
        }
        return placed;
    }
}
