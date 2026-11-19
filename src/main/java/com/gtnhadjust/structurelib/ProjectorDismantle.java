package com.gtnhadjust.structurelib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.gtnewhorizon.structurelib.item.ItemDebugConstructableTrigger;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.ISpecialItemBlock;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElement.BlocksToPlace;
import com.gtnewhorizon.structurelib.structure.IStructureElement.PlaceResult;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnhadjust.GTNHAdjust;

import cpw.mods.fml.common.Loader;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

public final class ProjectorDismantle {

    public static final String CHANNEL = "dismantle";
    private static final ThreadLocal<ProjectorDismantle> CURRENT = new ThreadLocal<>();
    private static final int MAX_PROBED_TIER = 64;

    private final EntityPlayerMP player;
    private final int controllerX;
    private final int controllerY;
    private final int controllerZ;
    private final boolean includeHatches;
    private final boolean hasAe = Loader.isModLoaded("appliedenergistics2");
    private final Map<IStructureElement<?>, Collection<String>> channels = new IdentityHashMap<>();
    private int remaining;
    private List<ItemStack> hatchDrops;

    private ProjectorDismantle(EntityPlayerMP player, ItemStack trigger, int budget, int x, int y, int z) {
        this.player = player;
        controllerX = x;
        controllerY = y;
        controllerZ = z;
        includeHatches = hasChannelOne(trigger, "gt_hatch");
        remaining = budget;
    }

    private static boolean hasChannelOne(ItemStack trigger, String channel) {
        return ChannelDataAccessor.hasSubChannel(trigger, channel)
            && ChannelDataAccessor.getChannelData(trigger, channel) == 1;
    }

    public static boolean isEnabled(ItemStack trigger) {
        return trigger.getItem() instanceof ItemConstructableTrigger
            && !(trigger.getItem() instanceof ItemDebugConstructableTrigger)
            && hasChannelOne(trigger, CHANNEL);
    }

    public static ProjectorDismantle current() {
        return CURRENT.get();
    }

    /** Share the original configuration budget across every piece visited during this click. */
    public static void run(ISurvivalConstructable target, ItemStack trigger, int budget, ISurvivalBuildEnvironment env,
        int x, int y, int z, Runnable traversal) {
        if (!(env.getActor() instanceof EntityPlayerMP player) || CURRENT.get() != null) return;
        MTEMultiBlockBase machine = target instanceof MTEMultiBlockBase multi ? multi : null;
        IGregTechTileEntity tile = machine == null ? null : machine.getBaseMetaTileEntity();
        if (tile != null && tile.isActive()) {
            player.addChatMessage(new ChatComponentTranslation("gtnhadjust.dismantle.active"));
            return;
        }
        boolean formed = machine != null && machine.mMachine;
        ProjectorDismantle scope = new ProjectorDismantle(player, trigger, budget, x, y, z);
        CURRENT.set(scope);
        try {
            if (machine != null) machine.mMachine = false;
            traversal.run();
        } finally {
            CURRENT.remove();
            if (machine != null) {
                if (scope.remaining < budget) machine.checkStructure(true, tile);
                else machine.mMachine = formed;
            }
        }
        player.addChatMessage(new ChatComponentTranslation("gtnhadjust.dismantle.removed", budget - scope.remaining));
    }

    /** Handle only inventory drops emitted by the current hatch's native break call. */
    public boolean captureHatchDrop(ItemStack stack) {
        if (hatchDrops == null) return false;
        hatchDrops.add(stack);
        return true;
    }

    /** Match the actual state against the structure's placement choices before breaking it. */
    public <T> PlaceResult visit(IStructureElement<T> element, T context, World world, int x, int y, int z,
        ItemStack trigger, AutoPlaceEnvironment env) {
        if (remaining <= 0) return PlaceResult.STOP;
        if ((x == controllerX && y == controllerY && z == controllerZ) || !world.blockExists(x, y, z)
            || world.isAirBlock(x, y, z)) return PlaceResult.SKIP;
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        TileEntity tile = world.getTileEntity(x, y, z);
        Fluid fluid = fluidAt(block);
        if (tile instanceof IGregTechTileEntity gt) {
            IMetaTileEntity hatch = gt.getMetaTileEntity();
            if (!includeHatches || hatch == null
                || hatch instanceof MTEMultiBlockBase
                || !matches(
                    element.getBlocksToPlace(context, world, x, y, z, trigger, env),
                    hatch.getStackForm(1),
                    null))
                return PlaceResult.SKIP;
        } else {
            Item item = Item.getItemFromBlock(block);
            int itemMeta = item instanceof ISpecialItemBlock special ? special.getItemMetaFromBlockMeta(block, meta)
                : meta;
            ItemStack actual = item == null ? null : new ItemStack(item, 1, itemMeta);
            if (!matchesBlock(element, context, world, x, y, z, trigger, env, actual, fluid)) return PlaceResult.SKIP;
        }
        if (block.getBlockHardness(world, x, y, z) < 0 || !world.canMineBlock(player, x, y, z)
            || !player.canPlayerEdit(x, y, z, ForgeDirection.UP.ordinal(), trigger)) return PlaceResult.SKIP;
        BlockEvent.BreakEvent event = ForgeHooks
            .onBlockBreakEvent(world, player.theItemInWorldManager.getGameType(), player, x, y, z);
        if (event == null || event.isCanceled()
            || world.getBlock(x, y, z) != block
            || world.getBlockMetadata(x, y, z) != meta
            || world.getTileEntity(x, y, z) != tile) return PlaceResult.SKIP;

        FluidStack recovered = fluid == null ? null : sourceFluid(block, fluid, world, x, y, z, meta);
        boolean hatch = tile instanceof IGregTechTileEntity;
        List<ItemStack> drops = fluid == null && !hatch ? block.getDrops(world, x, y, z, meta, 0) : new ArrayList<>();
        hatchDrops = hatch ? drops : null;
        boolean removed;
        try {
            removed = fluid == null && !hatch ? block.removedByPlayer(world, player, x, y, z, false)
                : world.setBlockToAir(x, y, z);
        } finally {
            hatchDrops = null;
        }
        if (!removed) return PlaceResult.SKIP;
        remaining--;
        if (hatch) drops.addAll(block.getDrops(world, x, y, z, meta, 0));
        for (ItemStack drop : drops) deliver(drop);
        if (hasAe && recovered != null) AeAutoPlaceItemSource.returnFluidToNetwork(player, recovered);
        if (GTNHAdjust.debug) GTNHAdjust.LOG.info("[hologram] dismantled {}:{} at {},{},{}", block, meta, x, y, z);
        return remaining == 0 ? PlaceResult.ACCEPT_STOP : PlaceResult.ACCEPT;
    }

    private <T> boolean matchesBlock(IStructureElement<T> element, T context, World world, int x, int y, int z,
        ItemStack trigger, AutoPlaceEnvironment env, ItemStack actual, Fluid fluid) {
        BlocksToPlace selected = element.getBlocksToPlace(context, world, x, y, z, trigger, env);
        if (matches(selected, actual, fluid)) return true;
        if (actual == null) return false;
        Collection<String> tierChannels = channels.computeIfAbsent(element, HologramReplacement::channels);
        ItemStack alternate = trigger.copy();
        for (String channel : tierChannels) {
            if ("gt_hatch".equals(channel)) continue;
            for (int tier = 1; tier <= MAX_PROBED_TIER; tier++) {
                ChannelDataAccessor.setChannelData(alternate, channel, tier);
                if (matches(element.getBlocksToPlace(context, world, x, y, z, alternate, env), actual, fluid))
                    return true;
            }
            alternate = trigger.copy();
        }
        // Older tiered elements take their tier from the projector's stack size.
        if (tierChannels.isEmpty() && selected != null && selected.getStacks() != null) {
            for (ItemStack candidate : selected.getStacks()) {
                if (candidate == null || candidate.getItem() != actual.getItem()) continue;
                for (int tier = 1; tier <= MAX_PROBED_TIER; tier++) {
                    alternate.stackSize = tier;
                    if (matches(element.getBlocksToPlace(context, world, x, y, z, alternate, env), actual, fluid))
                        return true;
                }
                break;
            }
        }
        return false;
    }

    private static boolean matches(BlocksToPlace choices, ItemStack actual, Fluid fluid) {
        if (choices == null) return false;
        if (fluid == null) return actual != null && choices.getPredicate()
            .test(actual);
        if (choices.getStacks() == null) return false;
        for (ItemStack candidate : choices.getStacks()) {
            if (candidate != null && fluidAt(Block.getBlockFromItem(candidate.getItem())) == fluid) return true;
        }
        return false;
    }

    private static Fluid fluidAt(Block block) {
        if (block == Blocks.water || block == Blocks.flowing_water) return FluidRegistry.WATER;
        if (block == Blocks.lava || block == Blocks.flowing_lava) return FluidRegistry.LAVA;
        return FluidRegistry.lookupFluidForBlock(block);
    }

    private static FluidStack sourceFluid(Block block, Fluid fluid, World world, int x, int y, int z, int meta) {
        if (block instanceof IFluidBlock fluidBlock) return fluidBlock.drain(world, x, y, z, false);
        return meta == 0 ? new FluidStack(fluid, 1000) : null;
    }

    private void deliver(ItemStack drop) {
        ItemStack left = hasAe ? AeAutoPlaceItemSource.returnToNetwork(player, drop) : drop;
        if (left != null && left.stackSize > 0 && !player.inventory.addItemStackToInventory(left) && left.stackSize > 0)
            player.dropPlayerItemWithRandomChoice(left, false);
    }
}
