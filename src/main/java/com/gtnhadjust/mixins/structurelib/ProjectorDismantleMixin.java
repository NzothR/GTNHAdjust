package com.gtnhadjust.mixins.structurelib;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.structurelib.ConfigurationHandler;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnhadjust.structurelib.ProjectorDismantle;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(value = ConstructableUtility.class, remap = false)
public abstract class ProjectorDismantleMixin {

    /** Reuse the target's survival structure traversal and the native click budget. */
    @WrapOperation(
        method = "handle0",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/structurelib/alignment/constructable/ISurvivalConstructable;survivalConstruct(Lnet/minecraft/item/ItemStack;ILcom/gtnewhorizon/structurelib/structure/ISurvivalBuildEnvironment;)I"))
    private static int gtnhadjust$dismantle(ISurvivalConstructable target, ItemStack trigger, int budget,
        ISurvivalBuildEnvironment env, Operation<Integer> original, @Local(argsOnly = true, ordinal = 0) int x,
        @Local(argsOnly = true, ordinal = 1) int y, @Local(argsOnly = true, ordinal = 2) int z) {
        if (!ProjectorDismantle.isEnabled(trigger)) return original.call(target, trigger, budget, env);
        ProjectorDismantle
            .run(target, trigger, budget, env, x, y, z, () -> original.call(target, trigger, budget, env));
        return 0;
    }

    /** Creative clicks in dismantle mode also use the budgeted traversal. */
    @WrapOperation(
        method = "handle0",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/structurelib/alignment/constructable/IConstructable;construct(Lnet/minecraft/item/ItemStack;Z)V"))
    private static void gtnhadjust$dismantleCreative(IConstructable target, ItemStack trigger, boolean hints,
        Operation<Void> original, @Local(argsOnly = true) EntityPlayer actor,
        @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y,
        @Local(argsOnly = true, ordinal = 2) int z) {
        if (hints || !ProjectorDismantle.isEnabled(trigger) || !(actor instanceof EntityPlayerMP player)) {
            original.call(target, trigger, hints);
            return;
        }
        if (target instanceof ISurvivalConstructable survival) {
            int budget = ConfigurationHandler.INSTANCE.getAutoPlaceBudget();
            ISurvivalBuildEnvironment env = ISurvivalBuildEnvironment.create(IItemSource.fromPlayer(player), player);
            ProjectorDismantle
                .run(survival, trigger, budget, env, x, y, z, () -> survival.survivalConstruct(trigger, budget, env));
        }
    }
}
