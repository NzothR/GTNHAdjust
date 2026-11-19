package com.gtnhadjust.mixins.structurelib;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElement.PlaceResult;
import com.gtnhadjust.structurelib.ProjectorDismantle;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(targets = "com.gtnewhorizon.structurelib.structure.SurvivalBuildStructureWalker", remap = false)
public abstract class ProjectorDismantleWalkerMixin<T> {

    /** Substitute dismantling for placement at each real structure element. */
    @WrapOperation(
        method = "visit",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/structurelib/structure/IStructureElement;survivalPlaceBlock(Ljava/lang/Object;Lnet/minecraft/world/World;IIILnet/minecraft/item/ItemStack;Lcom/gtnewhorizon/structurelib/structure/AutoPlaceEnvironment;)Lcom/gtnewhorizon/structurelib/structure/IStructureElement$PlaceResult;"))
    private PlaceResult gtnhadjust$visit(IStructureElement<T> element, T context, World world, int x, int y, int z,
        ItemStack trigger, AutoPlaceEnvironment env, Operation<PlaceResult> original) {
        ProjectorDismantle scope = ProjectorDismantle.current();
        if (scope == null) return original.call(element, context, world, x, y, z, trigger, env);
        return scope.visit(element, context, world, x, y, z, trigger, env);
    }

    /** The construction walker normally checks each newly placed block. */
    @WrapOperation(
        method = "visit",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/structurelib/structure/IStructureElement;check(Ljava/lang/Object;Lnet/minecraft/world/World;III)Z"))
    private boolean gtnhadjust$check(IStructureElement<T> element, T context, World world, int x, int y, int z,
        Operation<Boolean> original) {
        return ProjectorDismantle.current() == null && original.call(element, context, world, x, y, z);
    }
}
