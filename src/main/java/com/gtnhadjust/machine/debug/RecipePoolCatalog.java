package com.gtnhadjust.machine.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import codechicken.nei.PositionedStack;
import codechicken.nei.api.API;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTUtility;

/** An independent read-only directory also lists empty maps and maps without a visible NEI recipe handler. */
@SideOnly(Side.CLIENT)
public final class RecipePoolCatalog extends TemplateRecipeHandler {

    public static void register() {
        API.registerUsageHandler(new RecipePoolCatalog());
    }

    @Override
    public String getRecipeName() {
        return StatCollector.translateToLocal("gtnhadjust.debugger.catalog");
    }

    @Override
    public String getGuiTexture() {
        return "minecraft:textures/gui/container/furnace.png";
    }

    @Override
    public int recipiesPerPage() {
        return 2;
    }

    /** Query on U, independent of world placement or multiblock formation. */
    @Override
    public void loadUsageRecipes(ItemStack ingredient) {
        if (!GTUtility.areStacksEqual(ingredient, RecipePoolDebugger.controller(), true)) return;
        RecipeMap.ALL_RECIPE_MAPS.values()
            .stream()
            .sorted(java.util.Comparator.comparing(map -> map.unlocalizedName))
            .forEach(map -> arecipes.add(new PoolEntry(map)));
    }

    @Override
    public void drawBackground(int recipeIndex) {
        codechicken.lib.gui.GuiDraw.drawRect(0, 0, 166, 60, 0xFFE0E0E0);
    }

    /** Show localized pool names and their exact registry identifiers without fabricating executable recipes. */
    @Override
    public void drawExtras(int recipeIndex) {
        RecipeMap<?> map = ((PoolEntry) arecipes.get(recipeIndex)).map;
        Minecraft.getMinecraft().fontRenderer
            .drawSplitString(StatCollector.translateToLocal(map.unlocalizedName), 4, 4, 158, 0x202020);
        Minecraft.getMinecraft().fontRenderer.drawSplitString(map.unlocalizedName, 4, 28, 158, 0x505050);
    }

    private final class PoolEntry extends CachedRecipe {

        private final RecipeMap<?> map;

        private PoolEntry(RecipeMap<?> map) {
            this.map = map;
        }

        @Override
        public PositionedStack getResult() {
            return null;
        }
    }
}
