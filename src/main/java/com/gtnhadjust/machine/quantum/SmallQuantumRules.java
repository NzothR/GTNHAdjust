package com.gtnhadjust.machine.quantum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Machine bonuses use logical glass and coil levels, both starting at one. */
public final class SmallQuantumRules {

    private SmallQuantumRules() {}

    public static int parallel(int glassLevel, int voltageTier) {
        long base = 32L * Math.max(1, glassLevel);
        return (int) Math.min(Integer.MAX_VALUE, base << Math.min(Math.max(0, voltageTier - 1), 24));
    }

    public static double durationMultiplier(int coilLevel) {
        return Math.max(1, 11 - Math.max(1, coilLevel)) / 10.0;
    }

    public static int recipeDuration(int duration, double multiplier) {
        BigDecimal result = BigDecimal.valueOf(duration)
            .multiply(BigDecimal.valueOf(multiplier))
            .setScale(0, RoundingMode.CEILING);
        if (result.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) >= 0) return Integer.MAX_VALUE;
        return Math.max(1, result.intValue());
    }

    public static double energyMultiplier(int glassLevel, int coilLevel) {
        return Math.max(0.1, 1.6 - 0.1 * Math.max(1, Math.min(glassLevel, coilLevel)));
    }
}
