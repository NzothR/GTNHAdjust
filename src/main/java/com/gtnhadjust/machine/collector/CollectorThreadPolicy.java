package com.gtnhadjust.machine.collector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import gregtech.api.util.OverclockCalculator;

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
        return (int) Math.max(1, 100L - Math.max(0L, (long) glassTier - HV_GLASS) * 15);
    }

    static long minimumEnergy(long recipeEUt, int percent) {
        return (recipeEUt * percent + 99) / 100;
    }

    static long requiredEnergy(long recipeEUt, int parallel, int percent, int overclocks) {
        double energy = recipeEUt * (percent / 100.0);
        for (int i = 0; i < overclocks && energy < Long.MAX_VALUE; i++) energy *= 2.0;
        energy = Math.ceil(energy) * parallel;
        if (energy >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return Math.max(1, (long) Math.ceil(energy));
    }

    static int maximumOverclocks(long voltage, long recipeEUt, int percent, int duration, int parallel) {
        if (voltage <= 0 || recipeEUt <= 0) return 0;
        return new OverclockCalculator().setRecipeEUt(recipeEUt)
            .setEUt(voltage)
            .setAmperage(parallel)
            .setParallel(parallel)
            .setCurrentParallel(parallel)
            .setMaxTierSkips(1)
            .setDuration(duration)
            .setDurationModifier(percent / 100.0)
            .setEUtDiscount(percent / 100.0)
            .setAmperageOC(false)
            .setEUtIncreasePerOC(2.0)
            .setDurationDecreasePerOC(2.0)
            .setMaxOverclocks(usefulOverclocks(duration, percent))
            .calculate()
            .getPerformedOverclocks();
    }

    /** Limit doubling to the first step that reaches one tick after the glass bonus. */
    static int usefulOverclocks(int duration, int percent) {
        double ticks = duration * (percent / 100.0);
        int count = 0;
        while (ticks >= 2.0 && count < 31) {
            ticks /= 2.0;
            count++;
        }
        return count;
    }

    static BalancedAllocation allocateBalanced(long capacity, long voltage, long[] recipeEUts, int[] durations,
        int slots, int maxParallel, int percent, boolean wireless, int wirelessOverclockLimit) {
        long[] budgets = new long[recipeEUts.length];
        int[] threadOverclocks = new int[recipeEUts.length];
        if (capacity <= 0 || slots <= 0 || maxParallel <= 0)
            return new BalancedAllocation(budgets, 0, threadOverclocks);

        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < recipeEUts.length; i++) if (recipeEUts[i] > 0) candidates.add(i);
        candidates.sort(Comparator.comparingLong(i -> requiredEnergy(recipeEUts[i], 1, percent, 0)));

        List<Integer> admitted = new ArrayList<>();
        long reserved = 0;
        for (int index : candidates) {
            long minimum = requiredEnergy(recipeEUts[index], 1, percent, 0);
            if (admitted.size() >= slots || minimum > capacity - reserved) break;
            admitted.add(index);
            reserved = addPower(reserved, minimum);
        }
        if (admitted.isEmpty()) return new BalancedAllocation(budgets, 0, threadOverclocks);

        int low = 1;
        int high = maxParallel;
        while (low < high) {
            int candidate = low + (int) (((long) high - low + 1) / 2);
            if (requiredPower(admitted, recipeEUts, candidate, 0, percent) <= capacity) low = candidate;
            else high = candidate - 1;
        }
        int parallel = low;

        if (wireless) {
            long used = 0;
            for (int index : admitted) {
                budgets[index] = requiredEnergy(recipeEUts[index], parallel, percent, 0);
                used += budgets[index];
            }
            boolean advanced;
            do {
                advanced = false;
                for (int index : admitted) {
                    int next = threadOverclocks[index] + 1;
                    if (next > wirelessOverclockLimit || next > usefulOverclocks(durations[index], percent)) continue;
                    long nextBudget = requiredEnergy(recipeEUts[index], parallel, percent, next);
                    long extra = nextBudget - budgets[index];
                    if (nextBudget == Long.MAX_VALUE || extra > capacity - used) continue;
                    used += extra;
                    budgets[index] = nextBudget;
                    threadOverclocks[index] = next;
                    advanced = true;
                }
            } while (advanced);
            return new BalancedAllocation(budgets, parallel, threadOverclocks);
        }

        int commonOverclocks = Integer.MAX_VALUE;
        for (int index : admitted) commonOverclocks = Math
            .min(commonOverclocks, maximumOverclocks(voltage, recipeEUts[index], percent, durations[index], parallel));
        int overclocks = 0;
        for (int candidate = 1; candidate <= commonOverclocks; candidate++) {
            if (requiredPower(admitted, recipeEUts, parallel, candidate, percent) > capacity) break;
            overclocks = candidate;
        }

        for (int index : admitted) {
            budgets[index] = requiredEnergy(recipeEUts[index], parallel, percent, overclocks);
            threadOverclocks[index] = overclocks;
        }
        return new BalancedAllocation(budgets, parallel, threadOverclocks);
    }

    private static long requiredPower(List<Integer> indexes, long[] recipeEUts, int parallel, int overclocks,
        int percent) {
        long total = 0;
        for (int index : indexes)
            total = addPower(total, requiredEnergy(recipeEUts[index], parallel, percent, overclocks));
        return total;
    }

    static final class BalancedAllocation {

        final long[] budgets;
        final int parallel;
        final int[] overclocks;

        BalancedAllocation(long[] budgets, int parallel, int[] overclocks) {
            this.budgets = budgets;
            this.parallel = parallel;
            this.overclocks = overclocks;
        }
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
