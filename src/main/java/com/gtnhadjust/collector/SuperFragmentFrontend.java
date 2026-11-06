package com.gtnhadjust.collector;

import java.util.List;

import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;

import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.common.gui.modularui.UIHelper;

/** Eye of Harmony-style display: inputs at the top, item grid, then fluid grid. */
public final class SuperFragmentFrontend extends RecipeMapFrontend {

    public SuperFragmentFrontend(BasicUIPropertiesBuilder ui, NEIRecipePropertiesBuilder nei) {
        super(ui.logoPos(new Pos2d(8, 8)), nei.recipeBackgroundSize(new Size(170, 264)));
    }

    @Override
    public List<Pos2d> getItemInputPositions(int count) {
        return UIHelper.getGridPositions(count, 43, 8, 5, 1);
    }

    @Override
    public List<Pos2d> getItemOutputPositions(int count) {
        return UIHelper.getGridPositions(count, 7, 44, 9, 11);
    }

    @Override
    public List<Pos2d> getFluidInputPositions(int count) {
        return UIHelper.getGridPositions(count, 7, 8, 1, 1);
    }

    @Override
    public List<Pos2d> getFluidOutputPositions(int count) {
        return UIHelper.getGridPositions(count, 7, 44 + 11 * 18, 9, 1);
    }
}
