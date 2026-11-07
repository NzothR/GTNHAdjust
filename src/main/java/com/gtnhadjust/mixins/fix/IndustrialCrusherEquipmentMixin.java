package com.gtnhadjust.mixins.fix;

import java.util.Iterator;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.kuba6000.mobsinfo.api.MobDrop;

/** Apply Soul Prison's drop-record filter independently of the original GUI toggle. */
@Mixin(targets = "kubatech.loaders.MobHandlerLoader$MobEECRecipe", remap = false)
public abstract class IndustrialCrusherEquipmentMixin {

    @ModifyVariable(method = "generateOutputs", at = @At("HEAD"), argsOnly = true, ordinal = 1, remap = false)
    private boolean gtnhadjust$disableOriginalFilter(boolean original) {
        return false;
    }

    @Redirect(
        method = "generateOutputs",
        at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0),
        remap = false)
    private Iterator<MobDrop> gtnhadjust$filterDrops(List<MobDrop> drops) {
        return drops.stream()
            .filter(drop -> drop.damages == null && drop.enchantable == null)
            .iterator();
    }
}
