package com.gtnhadjust.terminal;

/** Select an existing candidate, or -1 to preserve the currently displayed alternative. */
final class IngredientPriority {

    private IngredientPriority() {}

    static int select(int[] favorite, boolean[] anyCircuit, boolean[] craftable, boolean[] stored) {
        int selected = -1, first = Integer.MAX_VALUE;
        for (int i = 0; i < favorite.length; i++) if (favorite[i] >= 0 && favorite[i] < first) {
            selected = i;
            first = favorite[i];
        }
        if (selected >= 0) return selected;
        for (int i = 0; i < anyCircuit.length; i++) if (anyCircuit[i]) return i;
        for (int i = craftable.length - 1; i >= 0; i--) if (craftable[i]) return i;
        for (int i = stored.length - 1; i >= 0; i--) if (stored[i]) return i;
        return -1;
    }
}
