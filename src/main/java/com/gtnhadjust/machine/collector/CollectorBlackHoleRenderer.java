package com.gtnhadjust.machine.collector;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.tileentities.render.RenderingTileEntityBlackhole;

/** Reuses the registered GT renderer with a client-only, non-world tile entity. */
@SideOnly(Side.CLIENT)
final class CollectorBlackHoleRenderer {

    private final RenderingTileEntityBlackhole tile = new RenderingTileEntityBlackhole();

    CollectorBlackHoleRenderer(World world) {
        NBTTagCompound data = new NBTTagCompound();
        data.setFloat("BLACKHOLESTABILITY", 1.0f);
        data.setBoolean("BLACKHOLESCALING", true);
        data.setLong("BLACKHOLESTART_TIME", world.getTotalWorldTime());
        tile.readFromNBT(data);
        tile.setWorldObj(world);
    }

    void render(double x, double y, double z, float partialTick) {
        TileEntitySpecialRenderer renderer = TileEntityRendererDispatcher.instance.getSpecialRenderer(tile);
        if (renderer != null) renderer.renderTileEntityAt(tile, x, y, z, partialTick);
    }
}
