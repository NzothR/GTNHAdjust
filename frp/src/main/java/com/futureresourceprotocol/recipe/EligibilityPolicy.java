package com.futureresourceprotocol.recipe;

/** Recipe bypass never overrides the resource identity or the blacklist. */
public final class EligibilityPolicy {

    private EligibilityPolicy() {}

    public static boolean isUxvClass(long voltage, long belowUhvMax) {
        return voltage > belowUhvMax;
    }

    public static boolean exceedsUxvLimit(long voltage, long belowUxvMax) {
        return voltage > belowUxvMax;
    }

    /** Unrestricted mode bypasses recipe and voltage checks while identity and blacklist rules remain in force. */
    public static boolean allows(boolean hasIdentity, boolean forbidden, boolean hasRecipe, boolean allowAll,
        boolean atOrAboveUxv) {
        return hasIdentity && !forbidden && (allowAll || !atOrAboveUxv && hasRecipe);
    }
}
