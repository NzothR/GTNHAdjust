package com.futureresourceprotocol.integration.ae2;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.security.IActionHost;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Refresh loaded grids when the shared protocol changes advance permissions. */
public final class AdvanceCellRefresh {

    private static final Set<TileEntity> HOSTS = Collections
        .synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private static volatile Thread serverThread;
    private WorldServer previousWorld;
    private boolean previousUhv;
    private boolean previousTerminated;

    static void track(Object host) {
        if (host instanceof TileEntity && host instanceof IActionHost) {
            TileEntity tile = (TileEntity) host;
            if (tile.getWorldObj() != null && !tile.getWorldObj().isRemote) HOSTS.add(tile);
        }
    }

    public static void startServer() {
        serverThread = Thread.currentThread();
    }

    public static void stopServer() {
        serverThread = null;
        HOSTS.clear();
    }

    static boolean isServerThread() {
        return serverThread != null && Thread.currentThread() == serverThread;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return;
        WorldServer world = server.worldServerForDimension(0);
        if (world == null) return;
        WorldProtocol protocol = WorldProtocol.get();
        boolean uhv = protocol.isUhvMilestone();
        boolean terminated = protocol.isTerminated();
        if (world == previousWorld && uhv == previousUhv && terminated == previousTerminated) return;
        previousWorld = world;
        previousUhv = uhv;
        previousTerminated = terminated;
        Set<IGrid> grids = Collections.newSetFromMap(new IdentityHashMap<>());
        TileEntity[] hosts;
        synchronized (HOSTS) {
            hosts = HOSTS.toArray(new TileEntity[0]);
        }
        for (TileEntity tile : hosts) {
            if (tile == null || tile.isInvalid() || tile.getWorldObj() == null || tile.getWorldObj().isRemote) continue;
            IGridNode node = ((IActionHost) tile).getActionableNode();
            if (node != null && node.getGrid() != null) grids.add(node.getGrid());
        }
        for (IGrid grid : grids) grid.postEvent(new MENetworkCellArrayUpdate());
        FutureResourceProtocol.debug("AE cell refresh grids={} UHV={} terminated={}", grids.size(), uhv, terminated);
    }
}
