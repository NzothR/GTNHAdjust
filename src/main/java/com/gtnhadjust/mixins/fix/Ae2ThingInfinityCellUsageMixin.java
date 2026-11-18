package com.gtnhadjust.mixins.fix;

import net.minecraft.item.ItemStack;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.asdflj.ae2thing.nei.InfinityStorageCellUsageHandler", remap = false)
public abstract class Ae2ThingInfinityCellUsageMixin {

    /** The first read validates the query's stack, before the handler stores it in its cell field. */
    @Redirect(
        method = "getUsageHandler(Ljava/lang/String;[Ljava/lang/Object;)Lcodechicken/nei/recipe/IUsageHandler;",
        at = @At(
            value = "FIELD",
            target = "Lcom/asdflj/ae2thing/nei/InfinityStorageCellUsageHandler;cell:Lnet/minecraft/item/ItemStack;",
            opcode = Opcodes.GETFIELD,
            ordinal = 0))
    private ItemStack gtnhadjust$readQueriedCell(@Coerce Object handler, String inputId, Object[] ingredients) {
        return (ItemStack) ingredients[0];
    }
}
