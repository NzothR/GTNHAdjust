package com.futureresourceprotocol.device;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.IGuiHandler;

public final class TerminalGuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id < 0 || id > 1 || x < 0 || x > 8) return null;
        return new TerminalContainer(player.inventory, x, id == 1);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id < 0 || id > 1 || x < 0 || x > 8) return null;
        return new TerminalGui(new TerminalContainer(player.inventory, x, id == 1), player.inventory);
    }
}
