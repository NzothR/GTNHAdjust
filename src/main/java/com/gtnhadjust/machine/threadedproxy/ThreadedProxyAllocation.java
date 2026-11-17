package com.gtnhadjust.machine.threadedproxy;

import gregtech.api.util.OverclockCalculator;

/** Native per-operation OC calculation used by independent slot-ordered threads and estimates. */
final class ThreadedProxyAllocation {

    private ThreadedProxyAllocation() {}

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
