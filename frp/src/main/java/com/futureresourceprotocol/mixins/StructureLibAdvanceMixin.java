package com.futureresourceprotocol.mixins;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.futureresourceprotocol.integration.structurelib.AdvanceItemSource;
import com.gtnewhorizon.structurelib.alignment.constructable.ConstructableUtility;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(value = ConstructableUtility.class, remap = false)
public abstract class StructureLibAdvanceMixin {

    /** Call the original factory first, including any AE source added by other mods. */
    @WrapOperation(
        method = "handle0",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gtnewhorizon/structurelib/structure/IItemSource;fromPlayer(Lnet/minecraft/entity/player/EntityPlayerMP;)Lcom/gtnewhorizon/structurelib/structure/IItemSource;",
            remap = false),
        remap = false)
    private static IItemSource frp$appendAdvance(EntityPlayerMP player, Operation<IItemSource> original) {
        return AdvanceItemSource.wrap(original.call(player), player);
    }
}
