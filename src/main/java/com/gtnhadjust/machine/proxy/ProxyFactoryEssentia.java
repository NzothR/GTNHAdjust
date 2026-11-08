package com.gtnhadjust.machine.proxy;

import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.recipe.RecipeMap;

public final class ProxyFactoryEssentia {

    private ProxyFactoryEssentia() {}

    public static boolean requiresEssentiaCell(RecipeMap<?> map) {
        if (map == null) return false;
        switch (map.unlocalizedName) {
            case "gtnl.recipe.industrial_crucible":
            case "gtnl.recipe.industrial_infusion_crafting":
            case "gtnl.recipe.industrial_shaped_arcane_crafting":
            case "tst.common.recipe.IndustrialAlchemyTowerRecipeMap.name":
            case "tst.common.recipe.IndustrialMagicMatrixRecipeMap.name":
                return true;
            default:
                return false;
        }
    }

    public static boolean isAspectDisplay(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
        return id != null && (("aspectrecipeindex".equals(id.modId) && "aspect".equals(id.name))
            || ("Thaumcraft".equals(id.modId) && "ItemEssence".equals(id.name) && stack.getItemDamage() == 1));
    }

}
