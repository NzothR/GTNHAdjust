package com.futureresourceprotocol.recipe;

/** Recipe bypass never overrides the resource identity or the blacklist. */
public final class EligibilityPolicy {

    private EligibilityPolicy() {}

    public static boolean isUmvClass(long voltage, long belowUhvMax) {
        return voltage > belowUhvMax;
    }

    public static boolean exceedsUmvLimit(long voltage, long belowUmvMax) {
        return voltage > belowUmvMax;
    }

    public static boolean allows(boolean hasIdentity, boolean forbidden, boolean hasRecipe, boolean allowAll,
        boolean atOrAboveUmv) {
        return hasIdentity && !forbidden && !atOrAboveUmv && (hasRecipe || allowAll);
    }
}
