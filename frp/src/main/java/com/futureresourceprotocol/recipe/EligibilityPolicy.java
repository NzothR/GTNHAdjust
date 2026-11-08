package com.futureresourceprotocol.recipe;

/** Recipe bypass never overrides the resource identity or the blacklist. */
public final class EligibilityPolicy {

    private EligibilityPolicy() {}

    public static boolean allows(boolean hasIdentity, boolean forbidden, boolean hasRecipe, boolean allowAll) {
        return hasIdentity && !forbidden && (hasRecipe || allowAll);
    }
}
