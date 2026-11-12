package com.gtnhadjust.machine.threadedproxy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import gregtech.api.util.OverclockCalculator;

/** Admits the most threads, reserves their overclocks, then maximizes their common concurrent parallel. */
final class ThreadedProxyAllocation {

    final Plan[] plans;
    final int parallel;

    private ThreadedProxyAllocation(Plan[] plans, int parallel) {
        this.plans = plans;
        this.parallel = parallel;
    }

    /** O(n log n + n * log(max duration)); no loop depends on the configured parallel or OC upper bound. */
    static ThreadedProxyAllocation allocate(long capacity, long voltage, long[] eut, int[] durations,
        int[] materialLimits, int ocLimit) {
        Plan[] plans = new Plan[eut.length];
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < eut.length; i++) if (materialLimits[i] > 0) order.add(i);
        order.sort(Comparator.comparingLong(i -> eut[i]));
        long reserved = 0;
        int commonLimit = Integer.MAX_VALUE;
        for (int index : order) {
            if (eut[index] > capacity - reserved) break;
            plans[index] = plan(eut[index], durations[index], voltage, 0);
            reserved += eut[index];
            commonLimit = Math.min(commonLimit, materialLimits[index]);
        }
        if (reserved == 0 && java.util.Arrays.stream(plans)
            .allMatch(java.util.Objects::isNull)) return new ThreadedProxyAllocation(plans, 0);
        // One OC per admitted thread per pass. Never take the base power reserved for another thread.
        boolean advanced;
        do {
            advanced = false;
            for (int i = 0; i < plans.length; i++) {
                Plan current = plans[i];
                if (current == null || current.overclocks >= ocLimit || current.duration <= 1) continue;
                Plan next = plan(eut[i], durations[i], voltage, current.overclocks + 1);
                long extra = next.eut - current.eut;
                if (next.overclocks <= current.overclocks || next.eut == Long.MAX_VALUE || extra > capacity - reserved)
                    continue;
                plans[i] = next;
                reserved += extra;
                advanced = true;
            }
        } while (advanced);
        int parallel = reserved == 0 ? commonLimit : (int) Math.min(commonLimit, capacity / reserved);
        return new ThreadedProxyAllocation(plans, parallel);
    }

    /** Native GT voltage/ULV rules, with an OC cap that stops at the first integer 1-tick duration. */
    static Plan plan(long eut, int duration, long voltage, int maxOverclocks) {
        OverclockCalculator calculator = new OverclockCalculator().setRecipeEUt(eut)
            .setDuration(duration)
            .setEUt(voltage)
            .setAmperage(1)
            .setAmperageOC(false)
            .setMaxOverclocks(Math.min(maxOverclocks, 31 - Integer.numberOfLeadingZeros(duration)))
            .calculate();
        return new Plan(calculator.getConsumption(), calculator.getDuration(), calculator.getPerformedOverclocks());
    }

    static final class Plan {

        final long eut;
        final int duration, overclocks;

        Plan(long eut, int duration, int overclocks) {
            this.eut = eut;
            this.duration = duration;
            this.overclocks = overclocks;
        }
    }
}
