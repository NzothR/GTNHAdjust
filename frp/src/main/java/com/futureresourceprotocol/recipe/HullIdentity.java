package com.futureresourceprotocol.recipe;

/** Exact item and metadata matching avoids wildcard or ore-dictionary substitutions. */
public final class HullIdentity {

    private HullIdentity() {}

    public static boolean matches(Object item, int metadata, Object target, int targetMetadata) {
        return item != null && item == target && metadata == targetMetadata;
    }
}
