package com.gtnhadjust.mixins.fix;

import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Slice;

import com.gtnhadjust.GTNHAdjust;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Pseudo
@Mixin(targets = "com.asdflj.ae2thing.loader.RecipeLoader", remap = false)
public abstract class Ae2ThingTerminalRecipeMixin {

    /** AE2Thing leaves this item unset when GTNR supplies the terminal. */
    @WrapOperation(
        method = "run()V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/asdflj/ae2thing/common/item/ItemWirelessDualInterfaceTerminal;stack()Lnet/minecraft/item/ItemStack;"),
        require = 3,
        remap = false)
    private ItemStack gtnhadjust$readTerminalStack(@Coerce Object terminal, Operation<ItemStack> original) {
        return terminal == null ? null : original.call(terminal);
    }

    /** The first shapeless registration after the terminal field read is the terminal's own crafting recipe. */
    @WrapWithCondition(
        method = "run()V",
        slice = @Slice(
            from = @At(
                value = "FIELD",
                target = "Lcom/asdflj/ae2thing/loader/ItemAndBlockHolder;ITEM_WIRELESS_DUAL_INTERFACE_TERMINAL:Lcom/asdflj/ae2thing/common/item/ItemWirelessDualInterfaceTerminal;",
                ordinal = 0)),
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/registry/GameRegistry;addShapelessRecipe(Lnet/minecraft/item/ItemStack;[Ljava/lang/Object;)V",
            ordinal = 0),
        require = 1,
        remap = false)
    private boolean gtnhadjust$registerTerminalCrafting(ItemStack output, Object[] inputs) {
        if (output == null && GTNHAdjust.debug)
            GTNHAdjust.LOG.info("Skipping AE2Thing terminal recipes because its terminal is not registered");
        return output != null;
    }

    /** Both upgrades require a registered instance of the AE2Thing terminal. */
    @WrapWithCondition(
        method = "run()V",
        at = { @At(
            value = "INVOKE",
            target = "Lcom/asdflj/ae2thing/loader/recipe/WirelessTerminalQuantumBridgeRecipe;register(Lnet/minecraft/item/ItemStack;)V"),
            @At(
                value = "INVOKE",
                target = "Lcom/asdflj/ae2thing/loader/recipe/WirelessTerminalEnergyRecipe;register(Lnet/minecraft/item/ItemStack;)V") },
        require = 2,
        remap = false)
    private boolean gtnhadjust$registerTerminalUpgrade(ItemStack terminal) {
        return terminal != null;
    }
}
