package com.gtnhadjust.machine.proxy;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnhadjust.GTNHAdjust;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import io.netty.buffer.ByteBuf;

/** Server-side target validation and collision-tested arrival; never builds a portal or an artificial platform. */
public final class MachineLocatorNavigation {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("gtnha_locator");
    private static final Map<EntityPlayerMP, Integer> LAST_USE = new WeakHashMap<>();
    private static final int RADIUS = 12, VERTICAL = 16;

    private MachineLocatorNavigation() {}

    public static void init() {
        CHANNEL.registerMessage(Handler.class, Highlight.class, 0, Side.CLIENT);
        if (FMLCommonHandler.instance()
            .getSide()
            .isClient()) MachineLocatorHighlight.init();
    }

    /** Validate the recorded controller, then highlight or move to the closest safe location in the bounded search. */
    public static void use(EntityPlayerMP player, MachineLocatorRecord record, boolean teleport) {
        Integer previous = LAST_USE.get(player);
        if (previous != null && player.ticksExisted - previous < 10) return;
        LAST_USE.put(player, player.ticksExisted);
        if (!teleport && player.dimension != record.dimension) {
            player.addChatMessage(new ChatComponentTranslation("gtnhadjust.locator.other_dimension", record.dimension));
            return;
        }
        if (!DimensionManager.isDimensionRegistered(record.dimension)) {
            report(player, "unavailable");
            return;
        }
        WorldServer world = DimensionManager.getWorld(record.dimension);
        if (world == null && teleport) {
            DimensionManager.initDimension(record.dimension);
            world = DimensionManager.getWorld(record.dimension);
        }
        if (world == null) {
            report(player, "unavailable");
            return;
        }
        world.getChunkFromBlockCoords(record.x, record.z);
        TileEntity tile = world.getTileEntity(record.x, record.y, record.z);
        if (!(tile instanceof IGregTechTileEntity gt) || gt.getMetaTileEntity() == null
            || !record.matches(gt.getMetaTileEntity())) {
            report(player, "target_missing");
            return;
        }
        if (teleport) {
            double[] arrival = findArrival(world, record, player);
            if (arrival == null) {
                report(player, "no_safe_ground");
                return;
            }
            player.mountEntity(null);
            player.closeScreen();
            if (player.dimension != record.dimension) {
                MinecraftServer.getServer()
                    .getConfigurationManager()
                    .transferPlayerToDimension(player, record.dimension, new DirectTeleporter(world, arrival));
            }
            if (player.dimension != record.dimension) {
                report(player, "unavailable");
                return;
            }
            player.motionX = player.motionY = player.motionZ = 0;
            player.fallDistance = 0;
            double dx = record.x + 0.5 - arrival[0], dz = record.z + 0.5 - arrival[2];
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            player.playerNetServerHandler.setPlayerLocation(arrival[0], arrival[1], arrival[2], yaw, 0);
            if (GTNHAdjust.debug) GTNHAdjust.LOG.info(
                "[locator-navigation] {} arrived dim={} pos=({}, {}, {}) target=({}, {}, {})",
                player.getCommandSenderName(),
                record.dimension,
                arrival[0],
                arrival[1],
                arrival[2],
                record.x,
                record.y,
                record.z);
        }
        CHANNEL.sendTo(new Highlight(record), player);
        player.addChatMessage(
            new ChatComponentTranslation(
                "gtnhadjust.locator." + (teleport ? "teleported" : "highlighted"),
                record.dimension,
                record.x,
                record.y,
                record.z));
    }

    /** Search actual support surfaces and player-sized free space, minimizing 3-D distance to the controller. */
    private static double[] findArrival(WorldServer world, MachineLocatorRecord record, EntityPlayerMP player) {
        for (int cx = (record.x - RADIUS) >> 4; cx <= (record.x + RADIUS) >> 4; cx++)
            for (int cz = (record.z - RADIUS) >> 4; cz <= (record.z + RADIUS) >> 4; cz++) world.getChunkProvider()
                .loadChunk(cx, cz);
        double[] best = null;
        double distance = Double.POSITIVE_INFINITY;
        for (int x = record.x - RADIUS; x <= record.x + RADIUS; x++)
            for (int z = record.z - RADIUS; z <= record.z + RADIUS; z++)
                for (int y = Math.max(1, record.y - VERTICAL); y
                    <= Math.min(world.getActualHeight() - 2, record.y + VERTICAL); y++) {
                        Block floor = world.getBlock(x, y - 1, z);
                        if (!floor.isSideSolid(world, x, y - 1, z, ForgeDirection.UP) || hazardous(floor)) continue;
                        AxisAlignedBB support = floor.getCollisionBoundingBoxFromPool(world, x, y - 1, z);
                        double half = Math.max(0.6, player.width) / 2.0 + 0.05;
                        double standingHeight = Math.max(1.8, player.height) + 0.05;
                        double px = x + 0.5, pz = z + 0.5;
                        if (support == null || support.minX > px - half
                            || support.maxX < px + half
                            || support.minZ > pz - half
                            || support.maxZ < pz + half) continue;
                        double feet = support.maxY;
                        double score = (px - record.x - 0.5) * (px - record.x - 0.5)
                            + (pz - record.z - 0.5) * (pz - record.z - 0.5)
                            + (feet - record.y) * (feet - record.y);
                        if (score >= distance) continue;
                        AxisAlignedBB body = AxisAlignedBB
                            .getBoundingBox(px - half, feet, pz - half, px + half, feet + standingHeight, pz + half);
                        if (world.isAnyLiquid(body) || !world.getCollidingBoundingBoxes(player, body)
                            .isEmpty()) continue;
                        boolean danger = false;
                        for (int by = (int) Math.floor(feet); by < feet + standingHeight; by++)
                            if (hazardous(world.getBlock(x, by, z))) {
                                danger = true;
                                break;
                            }
                        if (!danger) {
                            best = new double[] { px, feet, pz };
                            distance = score;
                        }
                    }
        return best;
    }

    private static boolean hazardous(Block block) {
        return block.getMaterial()
            .isLiquid() || block == Blocks.fire
            || block == Blocks.cactus
            || block == Blocks.web
            || block == Blocks.portal
            || block == Blocks.end_portal;
    }

    private static void report(EntityPlayerMP player, String key) {
        player.addChatMessage(new ChatComponentTranslation("gtnhadjust.locator." + key));
    }

    /** Cross-dimensional arrival at the prevalidated location, with vanilla portal creation completely bypassed. */
    private static final class DirectTeleporter extends Teleporter {

        private final double[] arrival;

        DirectTeleporter(WorldServer world, double[] arrival) {
            super(world);
            this.arrival = arrival;
        }

        @Override
        public void placeInPortal(Entity entity, double x, double y, double z, float yaw) {
            entity.setLocationAndAngles(arrival[0], arrival[1], arrival[2], yaw, entity.rotationPitch);
            entity.motionX = entity.motionY = entity.motionZ = 0;
        }
    }

    public static final class Highlight implements IMessage {

        int dimension, x, y, z;

        public Highlight() {}

        Highlight(MachineLocatorRecord record) {
            dimension = record.dimension;
            x = record.x;
            y = record.y;
            z = record.z;
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(dimension);
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            dimension = buf.readInt();
            x = buf.readInt();
            y = buf.readInt();
            z = buf.readInt();
        }
    }

    public static final class Handler implements IMessageHandler<Highlight, IMessage> {

        @Override
        public IMessage onMessage(Highlight message, MessageContext context) {
            MachineLocatorHighlight.show(message);
            return null;
        }
    }
}
