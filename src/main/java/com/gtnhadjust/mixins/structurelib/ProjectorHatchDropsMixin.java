package com.gtnhadjust.mixins.structurelib;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnhadjust.structurelib.ProjectorDismantle;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import gregtech.common.blocks.BlockMachines;

@Mixin(value = BlockMachines.class, remap = false)
public abstract class ProjectorHatchDropsMixin {

    /** Route inventory items ejected by GT's hatch break path with the hatch drop. */
    @WrapOperation(
        method = "breakBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;spawnEntityInWorld(Lnet/minecraft/entity/Entity;)Z",
            remap = true),
        remap = true)
    private boolean gtnhadjust$captureDrops(World world, Entity entity, Operation<Boolean> original) {
        ProjectorDismantle scope = ProjectorDismantle.current();
        return (scope != null && entity instanceof EntityItem item && scope.captureHatchDrop(item.getEntityItem()))
            || original.call(world, entity);
    }
}
