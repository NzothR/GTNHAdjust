package com.gtnhadjust.machine.quantum;

import com.gtnhadjust.GTNHAdjust;

import gregtech.api.gui.modularui.GTUITextures;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMapBackend;
import gregtech.api.recipe.RecipeMapBuilder;
import gregtech.api.recipe.maps.QuantumForceTransformerFrontend;

public final class SmallQuantumRecipes {

    public static final RecipeMap<RecipeMapBackend> MAP = RecipeMapBuilder.of("gtnhadjust.recipe.small_quantum")
        .maxIO(7, 6, 6, 6)
        .minInputs(0, 0)
        .progressBar(GTUITextures.PROGRESSBAR_ARROW_MULTIPLE)
        .neiHandlerInfo(
            b -> b.setDisplayStack(SmallQuantumRegistration.controller())
                .setHeight(210))
        .frontend(QuantumForceTransformerFrontend::new)
        .build();

    private SmallQuantumRecipes() {}

    public static void initialize() {
        if (MAP == null) throw new IllegalStateException("Missing recipe map");
    }

    public static void register() {
        SmallQuantumSnapshot.register();
        GTNHAdjust.LOG.info(
            "Registered {} fixed small quantum recipes",
            MAP.getAllRecipes()
                .size());
    }
}
