package com.gtnhadjust.structurelib;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.gtnhadjust.structurelib.PlanetaryEngineStructure.Section;
import com.science.gtnl.common.machine.multiblock.GenerationEarthEngine;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/** Replaces the engine's client preview while leaving server construction on the native path. */
public final class PlanetaryEngineProjector {

    private PlanetaryEngineProjector() {}

    public interface Preview {

        void gtnhadjust$replaceEnginePreview(World world, AxisAlignedBB outline);
    }

    public static boolean isEnabled(ItemStack trigger) {
        return ChannelDataAccessor.hasSubChannel(trigger, PlanetaryEngineStructure.RENDER_CHANNEL)
            && ChannelDataAccessor.getChannelData(trigger, PlanetaryEngineStructure.RENDER_CHANNEL) == 1;
    }

    public static boolean handle(ItemStack trigger, EntityPlayer player, World world, int x, int y, int z) {
        if (!world.isRemote || !(trigger.getItem() instanceof ItemConstructableTrigger)
            || player instanceof FakePlayer
            || !isEnabled(trigger)
            || !StructureLib.isCurrentPlayer(player)
            || !(world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile)
            || !(tile.getMetaTileEntity() instanceof GenerationEarthEngine engine)) return false;

        PlanetaryEngineStructure plan = ((PlanetaryEngineStructure.Provider) engine).gtnhadjust$getProjectorStructure();
        Preview preview = (Preview) StructureLib.instance()
            .proxy();
        if (ChannelDataAccessor.hasSubChannel(trigger, PlanetaryEngineStructure.PREVIEW_CHANNEL)
            && ChannelDataAccessor.getChannelData(trigger, PlanetaryEngineStructure.PREVIEW_CHANNEL) == 2) {
            AxisAlignedBB outline = plan.bounds(engine);
            preview.gtnhadjust$replaceEnginePreview(world, outline);
            message(
                player,
                "outline",
                (int) (outline.maxX - outline.minX),
                (int) (outline.maxZ - outline.minZ),
                (int) (outline.maxY - outline.minY));
            return true;
        }
        Section section = plan.select(engine, trigger, player);
        if (section == null) return true;
        if (world.getChunkFromChunkCoords(section.chunkX, section.chunkZ) instanceof EmptyChunk) {
            message(player, "unloaded", section.number, section.chunkX, section.chunkZ);
            return true;
        }
        preview.gtnhadjust$replaceEnginePreview(world, null);
        ItemStack hintTrigger = trigger;
        if (ChannelDataAccessor.hasSubChannel(trigger, StructureLibAPI.CHANNEL_SHOW_ERROR)) {
            hintTrigger = trigger.copy();
            ChannelDataAccessor.unsetChannelData(hintTrigger, StructureLibAPI.CHANNEL_SHOW_ERROR);
        }
        section.project(engine, hintTrigger, world);
        message(
            player,
            "preview",
            section.number,
            section.total,
            section.chunkX,
            section.chunkZ,
            section.bottom,
            section.top);
        return true;
    }

    static void message(EntityPlayer player, String key, Object... arguments) {
        player.addChatMessage(new ChatComponentTranslation("gtnhadjust.planetary_engine." + key, arguments));
    }
}
