package com.gtnhadjust.machine.collector;

/** Pure limits for the collector's independently scheduled recipes. */
final class CollectorThreadPolicy {

    static final int LV_TIER = 1;
    static final int HV_GLASS = 3;

    private CollectorThreadPolicy() {}

    static int parallel(int voltageTier, int glassTier, int base, int voltageMultiplier) {
        if (voltageTier < LV_TIER || glassTier < HV_GLASS) return 0;
        long value = base;
        for (int i = LV_TIER; i < voltageTier && value < Integer.MAX_VALUE; i++)
            value = Math.min(Integer.MAX_VALUE, value * voltageMultiplier);
        return (int) value;
    }

    static int threads(int voltageTier, int glassTier, int base, int voltageIncrease) {
        if (voltageTier < LV_TIER || glassTier < HV_GLASS) return 0;
        long value = (long) base + (long) (voltageTier - LV_TIER) * voltageIncrease + (long) (glassTier - HV_GLASS) * 4;
        return (int) Math.min(256, value);
    }

    static int glassEnergyPercent(int glassTier) {
        return (int) Math.max(10, 100L - Math.max(0L, (long) glassTier - HV_GLASS) * 10);
    }

    static long minimumEnergy(long recipeEUt, int percent) {
        return (recipeEUt * percent + 99) / 100;
    }

    static long cappedVoltage(long summedVoltage, long highestHatchVoltage) {
        if (summedVoltage <= 0 || highestHatchVoltage <= 0) return 0;
        long cap = highestHatchVoltage > Long.MAX_VALUE / 4 ? Long.MAX_VALUE : highestHatchVoltage * 4;
        return Math.min(summedVoltage, cap);
    }

    /** Admit the greatest number of runnable recipes, then divide power as evenly as their minimums allow. */
    static long[] allocatePower(long capacity, long[] minimums, int slots) {
        long[] budget = new long[minimums.length];
        if (capacity <= 0 || slots <= 0) return budget;
        java.util.List<Integer> eligible = new java.util.ArrayList<>();
        for (int i = 0; i < minimums.length; i++) if (minimums[i] > 0) eligible.add(i);
        eligible.sort(java.util.Comparator.comparingLong(i -> minimums[i]));
        java.util.List<Integer> admitted = new java.util.ArrayList<>();
        long reserved = 0;
        for (int index : eligible) {
            if (admitted.size() >= slots || minimums[index] > capacity - reserved) break;
            reserved += minimums[index];
            admitted.add(index);
        }
        long left = capacity;
        while (!admitted.isEmpty()) {
            long share = left / admitted.size();
            int largest = admitted.get(admitted.size() - 1);
            if (minimums[largest] > share) {
                budget[largest] = minimums[largest];
                left -= minimums[largest];
                admitted.remove(admitted.size() - 1);
            } else {
                long remainder = left % admitted.size();
                for (int index : admitted) budget[index] = share + (remainder-- > 0 ? 1 : 0);
                break;
            }
        }
        return budget;
    }

    static boolean canStart(java.util.Set<String> active, String key, int limit) {
        return key != null && !key.isEmpty() && active.size() < limit && !active.contains(key);
    }

    static long remainingPower(long capacity, long used) {
        if (used < 0) throw new IllegalArgumentException("Negative used power");
        return Math.max(0, capacity) <= used ? 0 : capacity - used;
    }

    static long addPower(long used, long next) {
        if (used < 0 || next < 0) throw new IllegalArgumentException("Negative power");
        return used > Long.MAX_VALUE - next ? Long.MAX_VALUE : used + next;
    }
}
