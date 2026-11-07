package com.gtnhadjust.terminal;

/** Prefer GTNR's current NEI result; recover a map only from the pattern being edited. */
public final class PatternRecipeMapPreservation {

    private PatternRecipeMapPreservation() {}

    public static String select(String current, String draft, String existing) {
        if (current != null) return current;
        return existing != null && existing.equals(draft) ? existing : null;
    }
}
