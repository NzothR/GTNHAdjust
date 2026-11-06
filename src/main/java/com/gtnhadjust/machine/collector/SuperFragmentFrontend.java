package com.gtnhadjust.machine.collector;

import java.util.List;

import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.common.widget.ProgressBar;

import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.recipe.BasicUIPropertiesBuilder;
import gregtech.api.recipe.NEIRecipePropertiesBuilder;
import gregtech.api.recipe.RecipeMapFrontend;
import gregtech.common.gui.modularui.UIHelper;
import gregtech.nei.GTNEIDefaultHandler;

/** Resource pages share the one-to-many 9 by 10 grid used by GT-Not-Hard. */
public final class SuperFragmentFrontend extends RecipeMapFrontend {

    public SuperFragmentFrontend(BasicUIPropertiesBuilder ui, NEIRecipePropertiesBuilder nei) {
        super(ui.logoPos(new Pos2d(8, 8)), nei.recipeBackgroundSize(new Size(170, 225)));
    }

    @Override
    public List<Pos2d> getItemInputPositions(int count) {
        return UIHelper.getGridPositions(count, 70, 8, 2, 1);
    }

    @Override
    public List<Pos2d> getItemOutputPositions(int count) {
        return UIHelper.getGridPositions(count, 7, 44, 9, 10);
    }

    @Override
    public List<Pos2d> getFluidInputPositions(int count) {
        return UIHelper.getGridPositions(count, 0, 0, 0, 0);
    }

    @Override
    public List<Pos2d> getFluidOutputPositions(int count) {
        return UIHelper.getGridPositions(count, 7, 44, 9, 10);
    }

    @Override
    public void addProgressBar(ModularWindow.Builder builder, GTNEIDefaultHandler.NEITemplateContext ctx) {
        builder.widget(
            new ProgressBar().setTexture(GTUITextures.PROGRESSBAR_HAMMER, 16)
                .setDirection(ProgressBar.Direction.DOWN)
                .setProgress(ctx.progressSupplier)
                .setSynced(false, false)
                .setPos(new Pos2d(81, 27).add(ctx.windowOffset))
                .setSize(14, 16));
    }
}
