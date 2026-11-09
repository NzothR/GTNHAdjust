package com.gtnhadjust.structurelib;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

/** Scope replacement to a player's survival projector invocation. */
public final class HologramReplacementContext {

    private static final ThreadLocal<HologramReplacementContext> CURRENT = new ThreadLocal<>();
    public boolean attempted;
    public boolean changed;

    private HologramReplacementContext() {}

    public static HologramReplacementContext current() {
        return CURRENT.get();
    }

    public static int construct(ISurvivalConstructable target, ItemStack trigger, int budget,
        ISurvivalBuildEnvironment env, java.util.function.IntSupplier original) {
        if (CURRENT.get() != null || !(target instanceof MTEMultiBlockBase machine)
            || !(env.getActor() instanceof EntityPlayerMP player)
            || !ChannelDataAccessor.hasSubChannel(trigger)) {
            return original.getAsInt();
        }
        IGregTechTileEntity tile = machine.getBaseMetaTileEntity();
        if (tile == null || !tile.isServerSide()) return original.getAsInt();
        if (tile.isActive()) {
            player.addChatMessage(new ChatComponentText("[全息投影仪] 请先停止机器再替换等级方块。"));
            return 0;
        }
        boolean formed = machine.mMachine;
        HologramReplacementContext state = new HologramReplacementContext();
        CURRENT.set(state);
        int result;
        try {
            machine.mMachine = false;
            result = original.getAsInt();
        } finally {
            CURRENT.remove();
            if (state.changed) machine.checkStructure(true, tile);
            else machine.mMachine = formed;
        }
        if (!formed && !state.attempted && !state.changed) return original.getAsInt();
        return result;
    }
}
