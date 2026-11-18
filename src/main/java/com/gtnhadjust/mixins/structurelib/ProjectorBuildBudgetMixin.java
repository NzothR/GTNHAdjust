package com.gtnhadjust.mixins.structurelib;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.gtnewhorizon.structurelib.item.ItemDebugConstructableTrigger;
import com.gtnhadjust.structurelib.ProjectorBuildBudget;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(value = ConstructableUtility.class, remap = false)
public abstract class ProjectorBuildBudgetMixin {

    /** Scope the override outside all native and addon survival-build calls for this click. */
    @WrapOperation(
        method = "handle",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/structurelib/alignment/constructable/ConstructableUtility;handle0(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;IIII)Z"))
    private static boolean gtnhadjust$limitSurvivalPlacement(ItemStack trigger, EntityPlayer player, World world, int x,
        int y, int z, int side, Operation<Boolean> original) {
        if (world.isRemote || !(player instanceof EntityPlayerMP)
            || !player.isSneaking()
            || player.capabilities.isCreativeMode
            || !(trigger.getItem() instanceof ItemConstructableTrigger)
            || trigger.getItem() instanceof ItemDebugConstructableTrigger)
            return original.call(trigger, player, world, x, y, z, side);
        return ProjectorBuildBudget.build(trigger, () -> original.call(trigger, player, world, x, y, z, side));
    }

    @ModifyExpressionValue(
        method = "handle0",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/structurelib/ConfigurationHandler;getAutoPlaceBudget()I"))
    private static int gtnhadjust$overridePlacementBudget(int configured) {
        ProjectorBuildBudget budget = ProjectorBuildBudget.current();
        return budget == null ? configured : budget.remaining();
    }
}
