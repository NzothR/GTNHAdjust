package com.gtnhadjust.machine.collector;

import com.gtnhadjust.GTNHAdjust;
import com.xyp.gtneverreset.common.recipe.gtneverreset.FragmentWorldCollectionBackend;

import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.recipe.RecipeCategory;
import gregtech.api.recipe.RecipeCategorySetting;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBuilder;

public final class SuperFragmentRecipes {

    public static final int ITEM_OUTPUTS = 90;
    public static final int FLUID_OUTPUTS = 90;
    public static final RecipeMap<FragmentWorldCollectionBackend> MAP = RecipeMapBuilder
        .of("gtnhadjust.recipe.super_fragment_collection", FragmentWorldCollectionBackend::new)
        .maxIO(2, ITEM_OUTPUTS, 0, FLUID_OUTPUTS)
        .minInputs(1, 0)
        .progressBar(GTUITextures.PROGRESSBAR_HAMMER)
        .progressBarPos(81, 27)
        .neiHandlerInfo(
            builder -> builder.setDisplayStack(SuperFragmentRegistration.controller())
                .setHeight(305)
                .setMultipleWidgetsAllowed(false))
        .frontend(SuperFragmentFrontend::new)
        .build();

    public static final RecipeCategory ORE = category("ore");
    public static final RecipeCategory GEM = category("gem");
    public static final RecipeCategory DUST = category("dust");
    public static final RecipeCategory INGOT = category("ingot");
    public static final RecipeCategory FLUID = category("fluid");

    private static RecipeCategory category(String type) {
        return new RecipeCategory(
            "gtnhadjust.recipe.super_fragment_" + type,
            MAP,
            () -> RecipeCategorySetting.ENABLE,
            builder -> builder.setDisplayStack(SuperFragmentRegistration.controller())
                .setHeight(305)
                .setMultipleWidgetsAllowed(false));
    }

    static RecipeCategory categoryFor(int circuit) {
        switch (circuit) {
            case 20:
                return ORE;
            case 21:
                return GEM;
            case 22:
                return DUST;
            case 23:
                return INGOT;
            case 24:
                return FLUID;
            default:
                throw new IllegalArgumentException("Unknown resource circuit " + circuit);
        }
    }

    private SuperFragmentRecipes() {}

    public static void initialize() {
        if (MAP == null) throw new IllegalStateException("Missing super fragment recipe map");
    }

    public static void register() {
        if (GTNHAdjust.singularityResourceRecipes) SingularityResources.register();
    }
}
