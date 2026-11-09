package com.gtnhadjust.quest;

public final class QuestFluidAmounts {

    private QuestFluidAmounts() {}

    public static int containers(long available, int capacity) {
        if (available <= 0 || capacity <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, available / capacity);
    }
}
