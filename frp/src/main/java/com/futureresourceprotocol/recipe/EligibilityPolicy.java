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

    public static boolean allows(boolean hasIdentity, boolean forbidden, boolean hasRecipe, boolean allowAll,
        boolean atOrAboveUxv) {
        return hasIdentity && !forbidden && !atOrAboveUxv && (hasRecipe || allowAll);
    }
}
