package com.gtnhadjust.machine.threadedproxy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraftforge.fluids.FluidRegistry;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;

/** Finds the first shared input between different patterns without scanning pairs of patterns. */
final class PatternInputConflicts {

    private PatternInputConflicts() {}

    static Conflict find(List<ICraftingPatternDetails> patterns, boolean hasWaterReservoir) {
        Map<IAEStack<?>, Integer> owners = new HashMap<>();
        for (int index = 0; index < patterns.size(); index++) {
            ICraftingPatternDetails pattern = patterns.get(index);
            for (IAEStack<?> input : pattern.getAEInputs()) {
                if (input == null || input.getStackSize() <= 0) continue;
                if (hasWaterReservoir && input instanceof IAEFluidStack fluid
                    && fluid.getFluidStack().getFluid() == FluidRegistry.WATER) continue;
                Integer previous = owners.putIfAbsent(input, index);
                if (previous != null && previous != index) return new Conflict(previous, index, input.copy());
            }
        }
        return null;
    }

    /** Pattern positions and the input shared by those two patterns. */
    static final class Conflict {
        final int firstPattern;
        final int secondPattern;
        final IAEStack<?> input;

        Conflict(int firstPattern, int secondPattern, IAEStack<?> input) {
            this.firstPattern = firstPattern;
            this.secondPattern = secondPattern;
            this.input = input;
        }
    }
}
