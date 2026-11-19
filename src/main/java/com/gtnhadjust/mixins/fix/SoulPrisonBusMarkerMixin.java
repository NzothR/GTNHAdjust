package com.gtnhadjust.mixins.fix;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Pseudo
@Mixin(targets = "com.newmaa.othtech.machine.OTEMegaEEC", remap = false)
public abstract class SoulPrisonBusMarkerMixin {

    @Shadow
    @Final
    private static Item poweredSpawnerItem;

    /** Fall back to the machine's stored inputs when the controller has no bound spawner. */
    @ModifyVariable(method = "checkProcessing", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private ItemStack gtnhadjust$findSpawnerInBus(ItemStack controllerStack) {
        if (gtnhadjust$isBoundSpawner(controllerStack)) return controllerStack;
        MTEMultiBlockBase machine = (MTEMultiBlockBase) (Object) this;
        for (ItemStack stack : machine.getStoredInputs()) {
            if (stack.stackSize > 0 && gtnhadjust$isBoundSpawner(stack)) return stack;
        }
        return controllerStack;
    }

    /** Match the spawner item and mob tag used by the original processing method. */
    @Unique
    private static boolean gtnhadjust$isBoundSpawner(ItemStack stack) {
        return stack != null && stack.getItem() == poweredSpawnerItem
            && stack.hasTagCompound()
            && !stack.getTagCompound()
                .getString("mobType")
                .isEmpty();
    }
}
