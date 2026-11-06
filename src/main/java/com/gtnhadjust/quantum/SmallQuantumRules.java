package com.gtnhadjust.quantum;

/** Machine bonuses use logical glass and coil levels, both starting at one. */
public final class SmallQuantumRules {

    private SmallQuantumRules() {}

    public static int parallel(int glassLevel, int voltageTier) {
        long base = 1L + 4L * (Math.max(1, glassLevel) - 1);
        return (int) Math.min(Integer.MAX_VALUE, base << Math.min(Math.max(0, voltageTier - 1), 24));
    }

    public static double durationMultiplier(int coilLevel) {
        return Math.max(0.1, 1.3 - 0.1 * Math.max(1, coilLevel));
    }

    public static double energyMultiplier(int glassLevel, int coilLevel) {
        return Math.max(0.1, 1.6 - 0.1 * Math.max(1, Math.min(glassLevel, coilLevel)));
    }
}
