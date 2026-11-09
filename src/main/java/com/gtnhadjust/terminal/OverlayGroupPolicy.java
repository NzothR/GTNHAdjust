package com.gtnhadjust.terminal;

final class OverlayGroupPolicy {

    private OverlayGroupPolicy() {}

    static int candidateIndex(int count, long timeMillis) {
        return (int) Math.floorMod(timeMillis / 1000, (long) count);
    }

    static boolean isMissing(boolean hasCraftableCandidate, boolean hasStoredCandidate, long missingCount) {
        return !hasCraftableCandidate && !hasStoredCandidate && missingCount > 0;
    }
}
