package com.gtnhadjust.recipe;

/** Only names owned by GTSR's recipe-map namespace are excluded. */
public final class GtsrRecipeMapPolicy {

    private GtsrRecipeMapPolicy() {}

    public static boolean isExclusive(String mapName) {
        return mapName != null && mapName.startsWith("gtsr.recipe.");
    }
}
