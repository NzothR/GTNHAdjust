package com.gtnhadjust.machine.loot;

import java.util.Random;

final class LootSimulatorRules {

    private LootSimulatorRules() {}

    static int acceptedCount(int alreadyCounted, int offered) {
        return Math.max(0, Math.min(offered, 64 - alreadyCounted));
    }

    static int rolls(Random random, int count, int chance) {
        if (count <= 0 || chance <= 0) return 0;
        if (chance >= 10000) return count;
        int successes = 0;
        for (int i = 0; i < count; i++) if (random.nextInt(10000) < chance) successes++;
        return successes;
    }

    static long experience(int count, int coilTier) {
        return (long) count * 120 * (1 << coilTier);
    }
}
