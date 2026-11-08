package com.gtnhadjust.mixins.terminal;

import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.terminal.BookmarkIngredients;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import gregtech.api.recipe.RecipeMaps;

@Mixin(targets = "com.xyp.gtneverreset.compat.nei.QuickTerminalRecipeTransferHandler", remap = false)
public abstract class QuickTerminalRecipeMixin {

    @Unique
    private static final ThreadLocal<Boolean> gtnhadjust$smeltingImport = new ThreadLocal<>();

    @Inject(method = "overlayRecipe", at = @At("HEAD"), remap = false)
    private void gtnhadjust$begin(GuiContainer gui, IRecipeHandler recipe, int index, boolean shift, CallbackInfo ci) {
        BookmarkIngredients.begin(true);
        gtnhadjust$smeltingImport.set("smelting".equals(recipe.getOverlayIdentifier()));
    }

    @Inject(method = "overlayRecipe", at = @At("RETURN"), remap = false)
    private void gtnhadjust$end(GuiContainer gui, IRecipeHandler recipe, int index, boolean shift, CallbackInfo ci) {
        BookmarkIngredients.end();
        gtnhadjust$smeltingImport.remove();
    }

    @ModifyArg(
        method = "overlayRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lcom/xyp/gtneverreset/common/packet/RecipeTransferPayload;<init>(ZZIZ[Lappeng/api/storage/data/IAEStack;[Lappeng/api/storage/data/IAEStack;Lnet/minecraft/nbt/NBTTagList;Ljava/lang/String;)V"),
        index = 7,
        remap = false)
    private String gtnhadjust$smeltingRecipeMap(String current) {
        return GTNHAdjust.proxyFactoryEnabled && current == null && Boolean.TRUE.equals(gtnhadjust$smeltingImport.get())
            ? RecipeMaps.furnaceRecipes.unlocalizedName
            : current;
    }

    @Redirect(
        method = "overlayRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lcom/xyp/gtneverreset/compat/nei/QuickTerminalRecipeTransferHandler;hasInterchangeableInputs(Lcodechicken/nei/recipe/IRecipeHandler;IZ)Z"),
        remap = false)
    private boolean gtnhadjust$allowAlternatives(IRecipeHandler recipe, int index, boolean crafting) {
        return false;
    }

    @Redirect(
        method = "overlayRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/recipe/IRecipeHandler;getIngredientStacks(I)Ljava/util/List;"),
        remap = false)
    private List<PositionedStack> gtnhadjust$overlayIngredients(IRecipeHandler recipe, int index) {
        return BookmarkIngredients.get(recipe, index);
    }

    @Redirect(
        method = "collectCraftingInputs",
        at = @At(
            value = "INVOKE",
            target = "Lcodechicken/nei/recipe/IRecipeHandler;getIngredientStacks(I)Ljava/util/List;"),
        remap = false)
    private static List<PositionedStack> gtnhadjust$ingredients(IRecipeHandler recipe, int index) {
        return BookmarkIngredients.get(recipe, index);
    }
}
