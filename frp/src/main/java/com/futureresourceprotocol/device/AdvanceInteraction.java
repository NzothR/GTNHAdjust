package com.futureresourceprotocol.device;

/** Packet actions and amounts for the five supported terminal gestures. */
public final class AdvanceInteraction {

    public static final int CURSOR_FULL = 2;
    public static final int INVENTORY_FULL = 3;
    public static final int REPAY_CURSOR = 4;
    public static final int CURSOR_HALF = 5;
    public static final int INVENTORY_HALF = 6;
    public static final int INVENTORY_ONE = 7;

    private AdvanceInteraction() {}

    public static int action(int button, boolean shift, boolean control) {
        if (button == 0) return control ? INVENTORY_ONE : shift ? INVENTORY_FULL : CURSOR_FULL;
        if (button == 1) return shift ? INVENTORY_HALF : CURSOR_HALF;
        return -1;
    }

    public static boolean isAdvance(int action) {
        return action == CURSOR_FULL || action == INVENTORY_FULL
            || action == CURSOR_HALF
            || action == INVENTORY_HALF
            || action == INVENTORY_ONE;
    }

    public static boolean toCursor(int action) {
        return action == CURSOR_FULL || action == CURSOR_HALF;
    }

    public static int amount(int action, int stackLimit) {
        int max = Math.max(1, stackLimit);
        if (action == INVENTORY_ONE) return 1;
        if (action == CURSOR_HALF || action == INVENTORY_HALF) return (max + 1) / 2;
        return max;
    }
}
