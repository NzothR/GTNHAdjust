package com.gtnhadjust.mixins.structurelib;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnhadjust.structurelib.HologramReplacementContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(targets = "com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility", remap = false)
public abstract class HologramConstructMixin {

    @WrapOperation(
        method = "handle0",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/structurelib/alignment/constructable/ISurvivalConstructable;survivalConstruct(Lnet/minecraft/item/ItemStack;ILcom/gtnewhorizon/structurelib/structure/ISurvivalBuildEnvironment;)I"))
    private static int gtnhadjust$replace(ISurvivalConstructable target, ItemStack stack, int budget,
        ISurvivalBuildEnvironment env, Operation<Integer> original) {
        return HologramReplacementContext
            .construct(target, stack, budget, env, () -> original.call(target, stack, budget, env));
    }
}
