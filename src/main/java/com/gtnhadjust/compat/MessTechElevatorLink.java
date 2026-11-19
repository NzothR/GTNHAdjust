package com.gtnhadjust.compat;

import java.util.List;

import net.minecraftforge.fluids.FluidStack;

import gtnhintergalactic.tile.multi.elevatormodules.TileEntityModuleBase;

public interface MessTechElevatorLink {

    void gtnhadjust$linkElevator(Host elevator);

    interface Host {

        boolean gtnhadjust$hosts(TileEntityModuleBase module);

        boolean gtnhadjust$hasDataInputs();

        long gtnhadjust$availableModuleData();

        List<FluidStack> gtnhadjust$moduleFluids();
    }
}
