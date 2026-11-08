package com.gtnhadjust.machine.proxy;

import appeng.me.cluster.implementations.CraftingCPUCluster;

/** Diagnostic context scoped to a synchronous CPU handoff; never persists across ticks. */
public final class ProxyDispatchTrace {

    private static final ThreadLocal<CraftingCPUCluster> CURRENT = new ThreadLocal<>();

    private ProxyDispatchTrace() {}

    public static CraftingCPUCluster enter(CraftingCPUCluster cpu) {
        CraftingCPUCluster previous = CURRENT.get();
        CURRENT.set(cpu);
        return previous;
    }

    public static void restore(CraftingCPUCluster previous) {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }

    public static CraftingCPUCluster current() {
        return CURRENT.get();
    }
}
