package com.futureresourceprotocol.integration.ae2;

import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.device.TerminalData;
import com.futureresourceprotocol.protocol.WorldProtocol;
import com.futureresourceprotocol.recipe.RecipeIndex;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;

/** Extract-only inventory. Missing world context fails closed. */
public final class AdvanceCellInventory implements IMEInventoryHandler<IAEItemStack> {

    private static final long DISPLAY = Integer.MAX_VALUE;
    private final ItemStack cell;
    private final ISaveProvider host;

    public AdvanceCellInventory(ItemStack cell, ISaveProvider host) {
        this.cell = cell;
        this.host = host;
        AdvanceCellRefresh.track(host);
    }

    private WorldProtocol world() {
        if (!(host instanceof TileEntity) || !AdvanceCellRefresh.isServerThread()) return null;
        TileEntity tile = (TileEntity) host;
        World world = tile.getWorldObj();
        MinecraftServer server = MinecraftServer.getServer();
        if (tile.isInvalid() || world == null
            || world.isRemote
            || server == null
            || server.worldServerForDimension(world.provider.dimensionId) != world) return null;
        return WorldProtocol.get();
    }

    private boolean permitted(WorldProtocol world, ItemStack stack) {
        return world != null && RecipeIndex.eligible(stack) && world.canAdvance(RecipeIndex.classify(stack, world));
    }

    @Override
    public IAEItemStack injectItems(IAEItemStack input, Actionable mode, BaseActionSource src) {
        return input;
    }

    @Override
    public IAEItemStack extractItems(IAEItemStack request, Actionable mode, BaseActionSource src) {
        if (request == null || request.getStackSize() <= 0) return null;
        WorldProtocol world = world();
        ItemStack stack = request.getItemStack();
        boolean marked = false;
        for (ItemStack candidate : TerminalData.read(cell)) if (TerminalData.same(candidate, stack)) {
            marked = true;
            break;
        }
        if (!marked || !permitted(world, stack)) return null;
        String key = WorldProtocol.key(stack);
        boolean umv = RecipeIndex.classify(stack, world);
        long amount = request.getStackSize();
        if (!world.canRecord(key, umv, amount)) return null;
        if (mode == Actionable.MODULATE) {
            world.advance(key, umv, amount);
            FutureResourceProtocol.debug(
                "AE advance key={} count={} class={} host={}",
                key,
                amount,
                umv ? "UMV" : "UHV",
                host.getClass()
                    .getName());
        }
        return request.copy();
    }

    @Override
    public IItemList<IAEItemStack> getAvailableItems(IItemList<IAEItemStack> out, int iteration) {
        WorldProtocol world = world();
        if (world == null) return out;
        for (ItemStack stack : TerminalData.read(cell)) if (permitted(world, stack)) {
            IAEItemStack entry = AEApi.instance()
                .storage()
                .createItemStack(stack);
            if (entry != null) out.add(entry.setStackSize(DISPLAY));
        }
        return out;
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    @Override
    public AccessRestriction getAccess() {
        return AccessRestriction.READ;
    }

    @Override
    public boolean isPrioritized(IAEItemStack item) {
        return false;
    }

    @Override
    public boolean canAccept(IAEItemStack item) {
        return false;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public int getSlot() {
        return 0;
    }

    @Override
    public boolean validForPass(int pass) {
        return true;
    }
}
