package com.gtnhadjust.mixins.goodgenerator;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import goodgenerator.blocks.tileEntity.AntimatterGenerator;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Mixin(value = AntimatterGenerator.class, remap = false)
public abstract class AntimatterWirelessMixin {

    @Shadow
    private boolean wirelessEnabled;

    /** Start newly placed controllers in wireless mode. */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtnhadjust$defaultWireless(CallbackInfo ci) {
        wirelessEnabled = true;
    }

    /** Migrate existing controllers once, then respect the saved mode. */
    @Inject(method = "loadNBTData", at = @At("RETURN"))
    private void gtnhadjust$loadWirelessMode(NBTTagCompound nbt, CallbackInfo ci) {
        if (!nbt.getBoolean("gtnhadjustAntimatterWireless")) wirelessEnabled = true;
    }

    /** Record that the controller has saved its mode under the adjusted rules. */
    @Inject(method = "saveNBTData", at = @At("RETURN"))
    private void gtnhadjust$saveWirelessMode(NBTTagCompound nbt, CallbackInfo ci) {
        nbt.setBoolean("gtnhadjustAntimatterWireless", true);
    }

    /** Lower only the wireless routing threshold, leaving catalyst exponents intact. */
    @ModifyConstant(method = "createEU", constant = @Constant(floatValue = 1.03F, ordinal = 1))
    private float gtnhadjust$allowAllCatalysts(float threshold) {
        return 1.0F;
    }

    /** Send the calculated production to the grid without a hatch capacity cap. */
    @Redirect(method = "createEU", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(JJ)J"))
    private long gtnhadjust$wirelessOutput(long production, long hatchCapacity) {
        return production;
    }

    /** Require a laser source hatch only when the machine routes power to hatches. */
    @Redirect(method = "checkMachine", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private boolean gtnhadjust$missingLaserHatch(List<?> hatches) {
        return !wirelessEnabled && hatches.isEmpty();
    }

    /** Remove the obsolete UMV requirement from the screwdriver feedback. */
    @Redirect(
        method = "onScrewdriverRightClick",
        at = @At(
            value = "INVOKE",
            target = "Lgregtech/api/util/GTUtility;sendChatTrans(Lnet/minecraft/entity/player/EntityPlayer;Ljava/lang/String;[Ljava/lang/Object;)V",
            ordinal = 1))
    private void gtnhadjust$omitCatalystHint(EntityPlayer player, String key, Object[] args) {}

    /** Revalidate the hatch requirement after either mode control changes it. */
    @Inject(method = { "onScrewdriverRightClick", "setWirelessEnabled" }, at = @At("RETURN"))
    private void gtnhadjust$recheckStructure(CallbackInfo ci) {
        MTEMultiBlockBase machine = (MTEMultiBlockBase) (Object) this;
        machine.mStructureChanged = true;
        machine.mUpdate = 1;
    }
}
