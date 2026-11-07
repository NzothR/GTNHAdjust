package com.gtnhadjust.mixins.fix;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Leave Ender IO's capture and release implementation intact. */
@Mixin(targets = "crazypants.enderio.item.ItemSoulVessel", remap = false)
@Pseudo
public abstract class SoulVialCaptureMixin {

    @Inject(
        method = { "itemInteractionForEntity", "func_111207_a" },
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void gtnhadjust$requireRestorableEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity,
        CallbackInfoReturnable<Boolean> cir) {
        String id = EntityList.getEntityString(entity);
        if (id == null || EntityList.stringToClassMapping.get(id) != entity.getClass()) cir.setReturnValue(false);
    }

    @Redirect(
        method = { "itemInteractionForEntity", "func_111207_a" },
        at = @At(
            value = "FIELD",
            target = "Lcrazypants/enderio/config/Config;soulVesselCapturesBosses:Z",
            opcode = Opcodes.GETSTATIC),
        remap = false)
    private boolean gtnhadjust$allowBosses() {
        return true;
    }

    @Redirect(
        method = { "itemInteractionForEntity", "func_111207_a" },
        at = @At(
            value = "INVOKE",
            target = "Lcrazypants/enderio/item/ItemSoulVessel;isBlackListed(Ljava/lang/String;)Z"),
        remap = false)
    private boolean gtnhadjust$allowBlacklisted(@Coerce Object vessel, String id) {
        return false;
    }
}
