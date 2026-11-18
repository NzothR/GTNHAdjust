package com.gtnhadjust.structurelib;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraftforge.common.util.FakePlayer;

import com.gtnewhorizon.structurelib.ConfigurationHandler;
import com.gtnewhorizon.structurelib.StructureLib;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.gtnewhorizon.structurelib.item.ItemDebugConstructableTrigger;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnhadjust.GTNHAdjust;
import com.gtnhadjust.structurelib.PlanetaryEngineStructure.Section;
import com.science.gtnl.common.machine.multiblock.GenerationEarthEngine;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

/** Schedules one section of an explicitly selected engine per projector action. */
public final class PlanetaryEngineProjector {

    private static final int BLOCKS_PER_TICK = 32;
    private static final PlanetaryEngineProjector INSTANCE = new PlanetaryEngineProjector();
    private final Map<UUID, Job> jobs = new LinkedHashMap<>();

    private PlanetaryEngineProjector() {}

    public interface Preview {

        void gtnhadjust$replaceEnginePreview();
    }

    /** Leave all other controllers, trigger items and channel values on the native path. */
    public static boolean handle(ItemStack trigger, EntityPlayer player, World world, int x, int y, int z) {
        if (!(trigger.getItem() instanceof ItemConstructableTrigger) || player instanceof FakePlayer
            || !ChannelDataAccessor.hasSubChannel(trigger, PlanetaryEngineStructure.RENDER_CHANNEL)
            || ChannelDataAccessor.getChannelData(trigger, PlanetaryEngineStructure.RENDER_CHANNEL) != 1
            || !(world.getTileEntity(x, y, z) instanceof IGregTechTileEntity tile)
            || !(tile.getMetaTileEntity() instanceof GenerationEarthEngine engine)) return false;

        if (world.isRemote ? !StructureLib.isCurrentPlayer(player) : !player.isSneaking()) return true;
        PlanetaryEngineStructure plan = ((PlanetaryEngineStructure.Provider) engine).gtnhadjust$getProjectorStructure();
        Section section = plan.select(engine, trigger, player);
        if (section == null) return true;
        if (world.isRemote ? world.getChunkFromChunkCoords(section.chunkX, section.chunkZ) instanceof EmptyChunk
            : !world.getChunkProvider()
                .chunkExists(section.chunkX, section.chunkZ)) {
            message(player, "unloaded", section.number, section.chunkX, section.chunkZ);
            return true;
        }
        if (world.isRemote) {
            ((Preview) StructureLib.instance()
                .proxy()).gtnhadjust$replaceEnginePreview();
            section.project(engine, trigger, world);
            message(
                player,
                "preview",
                section.number,
                section.total,
                section.chunkX,
                section.chunkZ,
                section.bottom,
                section.top);
        } else if (player instanceof EntityPlayerMP serverPlayer) {
            INSTANCE.enqueue(serverPlayer, engine, trigger, section);
        }
        return true;
    }

    private void enqueue(EntityPlayerMP player, GenerationEarthEngine engine, ItemStack trigger, Section section) {
        if (engine.mMachine) {
            player.addChatMessage(new ChatComponentTranslation("structurelib.autoplace.complete"));
            return;
        }
        UUID id = player.getUniqueID();
        Job pending = jobs.get(id);
        if (pending != null && pending.engine == engine
            && pending.section == section
            && ItemStack.areItemStacksEqual(pending.trigger, trigger)) return;
        jobs.put(id, new Job(player, engine, trigger.copy(), section));
        message(
            player,
            "queued",
            section.number,
            section.total,
            section.chunkX,
            section.chunkZ,
            section.bottom,
            section.top);
    }

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
        jobs.remove(event.player.getUniqueID());
    }

    /** One active builder gets a bounded native placement pass per server tick, in round-robin order. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || jobs.isEmpty()) return;
        Iterator<Job> iterator = jobs.values()
            .iterator();
        Job job = iterator.next();
        iterator.remove();
        if (job.tick()) jobs.put(job.player.getUniqueID(), job);
    }

    static void message(EntityPlayer player, String key, Object... arguments) {
        player.addChatMessage(new ChatComponentTranslation("gtnhadjust.planetary_engine." + key, arguments));
    }

    private static final class Job {

        final EntityPlayerMP player;
        final GenerationEarthEngine engine;
        final ItemStack trigger;
        final Section section;
        final World world;
        final boolean creative;
        final ISurvivalBuildEnvironment environment;
        int remaining, cursor, built;

        Job(EntityPlayerMP player, GenerationEarthEngine engine, ItemStack trigger, Section section) {
            this.player = player;
            this.engine = engine;
            this.trigger = trigger;
            this.section = section;
            world = player.worldObj;
            creative = player.capabilities.isCreativeMode || trigger.getItem() instanceof ItemDebugConstructableTrigger;
            int nativeBudget = ConfigurationHandler.INSTANCE.getAutoPlaceBudget();
            remaining = Math.min(500, nativeBudget * 5);
            environment = creative ? null : ISurvivalBuildEnvironment.create(IItemSource.fromPlayer(player), player);
        }

        boolean tick() {
            if (player.worldObj != world || player.isDead
                || !player.playerNetServerHandler.netManager.isChannelOpen()
                || !ItemStack.areItemStacksEqual(trigger, player.getHeldItem())) return false;
            if (!world.getChunkProvider()
                .chunkExists(section.x >> 4, section.z >> 4)
                || !world.getChunkProvider()
                    .chunkExists(section.chunkX, section.chunkZ)) {
                message(player, "unloaded", section.number, section.chunkX, section.chunkZ);
                return false;
            }
            if (!(world.getTileEntity(section.x, section.y, section.z) instanceof IGregTechTileEntity tile)
                || tile.getMetaTileEntity() != engine
                || engine.getExtendedFacing() != section.facing
                || engine.mMachine) return false;
            if (creative) {
                if (!player.capabilities.isCreativeMode
                    && !(trigger.getItem() instanceof ItemDebugConstructableTrigger)) return false;
                int next = Math.min(cursor + BLOCKS_PER_TICK, section.volume());
                built += section.buildCreative(engine, trigger, world, player, cursor, next);
                cursor = next;
                if (cursor < section.volume()) return true;
            } else {
                int count = section.build(engine, trigger, world, Math.min(remaining, BLOCKS_PER_TICK), environment);
                if (count > 0) {
                    remaining -= count;
                    built += count;
                    if (remaining > 0) return true;
                }
            }
            if (built > 0) engine.setStructureUpdateTime(50);
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                "[planetary-projector] finished section {} at chunk {},{} with {} placements",
                section.number,
                section.chunkX,
                section.chunkZ,
                built);
            message(player, "finished", section.number, built);
            return false;
        }
    }
}
