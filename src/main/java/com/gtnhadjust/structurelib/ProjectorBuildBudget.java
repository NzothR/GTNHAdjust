package com.gtnhadjust.structurelib;

import java.util.function.BooleanSupplier;

import net.minecraft.item.ItemStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;

/** Shares one successful-placement budget across all StructureLib pieces of a projector action. */
public final class ProjectorBuildBudget {

    public static final String CHANNEL = "placement_budget";
    private static final int MAX_BLOCKS = 2_100_000_000;
    private static final ThreadLocal<ProjectorBuildBudget> CURRENT = new ThreadLocal<>();

    private int remaining;

    private ProjectorBuildBudget(int remaining) {
        this.remaining = remaining;
    }

    public static boolean build(ItemStack trigger, BooleanSupplier original) {
        if (CURRENT.get() != null || !ChannelDataAccessor.hasSubChannel(trigger, CHANNEL))
            return original.getAsBoolean();
        int requested = ChannelDataAccessor.getChannelData(trigger, CHANNEL);
        if (requested <= 0) return original.getAsBoolean();
        CURRENT.set(new ProjectorBuildBudget(Math.min(requested, MAX_BLOCKS)));
        try {
            return original.getAsBoolean();
        } finally {
            CURRENT.remove();
        }
    }

    public static ProjectorBuildBudget current() {
        return CURRENT.get();
    }

    public int remaining() {
        return remaining;
    }

    public boolean placedBlock() {
        return --remaining > 0;
    }
}
