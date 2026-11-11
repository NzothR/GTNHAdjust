package com.gtnhadjust.structurelib;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElement.BlocksToPlace;
import com.gtnewhorizon.structurelib.structure.IStructureElement.PlaceResult;
import com.gtnhadjust.GTNHAdjust;

import cpw.mods.fml.common.Loader;
import gregtech.api.util.GTUtility;

/** Places explicitly identified source-fluid structure elements using player or wireless AE supplies. */
public final class HologramFluidPlacement {

    private static final int SOURCE_AMOUNT = 1000;

    private HologramFluidPlacement() {}

    public static <T> PlaceResult tryPlace(IStructureElement<T> element, T controller, World world, int x, int y, int z,
        ItemStack trigger, AutoPlaceEnvironment env) {
        if (!(env.getActor() instanceof EntityPlayerMP player) || !world.blockExists(x, y, z)) return null;
        BlocksToPlace candidates = element.getBlocksToPlace(controller, world, x, y, z, trigger, env);
        if (candidates == null || candidates.getStacks() == null) return null;
        Fluid fluid = null;
        for (ItemStack candidate : candidates.getStacks()) {
            if (candidate == null || candidate.getItem() == null || candidate.getItemDamage() != 0) return null;
            Block block = Block.getBlockFromItem(candidate.getItem());
            Fluid next = FluidRegistry.lookupFluidForBlock(block);
            if (next == null || next.getBlock() == null || (fluid != null && fluid != next)) return null;
            fluid = next;
        }
        if (fluid == null) return null;
        Block existing = world.getBlock(x, y, z);
        if (FluidRegistry.lookupFluidForBlock(existing) == fluid && world.getBlockMetadata(x, y, z) == 0)
            return PlaceResult.SKIP;
        // Flowing copies of the requested fluid may occupy later structure positions.
        boolean sameFlow = (fluid == FluidRegistry.WATER && existing == Blocks.flowing_water)
            || (fluid == FluidRegistry.LAVA && existing == Blocks.flowing_lava)
            || (FluidRegistry.lookupFluidForBlock(existing) == fluid && world.getBlockMetadata(x, y, z) != 0);
        if ((!world.isAirBlock(x, y, z) && !sameFlow) || world.getTileEntity(x, y, z) != null)
            return PlaceResult.REJECT;
        if (!world.canMineBlock(player, x, y, z)
            || !player.canPlayerEdit(x, y, z, ForgeDirection.UP.ordinal(), trigger)) return PlaceResult.REJECT;
        if (fluid == FluidRegistry.WATER && world.provider.isHellWorld) return PlaceResult.REJECT;
        FluidStack wanted = new FluidStack(fluid, SOURCE_AMOUNT);
        int slot = findContainer(player, wanted);
        boolean network = slot < 0 && GTNHAdjust.hologramAeMaterials
            && Loader.isModLoaded("appliedenergistics2")
            && AeAutoPlaceItemSource.takeFluid(player, wanted, true);
        if (slot < 0 && !network) {
            env.getChatter()
                .accept(new ChatComponentText("[全息投影仪] 缺少流体：" + fluid.getLocalizedName(wanted) + " (1000 mB)"));
            if (GTNHAdjust.debug)
                GTNHAdjust.LOG.info("[hologram] missing fluid {} at {},{},{}", fluid.getName(), x, y, z);
            return PlaceResult.REJECT;
        }
        BlockSnapshot before = BlockSnapshot.getBlockSnapshot(world, x, y, z);
        if (!world.setBlock(x, y, z, fluid.getBlock(), 0, 3)) return PlaceResult.REJECT;
        if (ForgeEventFactory.onPlayerBlockPlace(player, before, ForgeDirection.UP)
            .isCanceled()) {
            before.restore(true, false);
            return PlaceResult.REJECT;
        }
        if (network && !AeAutoPlaceItemSource.takeFluid(player, wanted, false)) {
            before.restore(true, false);
            return PlaceResult.REJECT;
        }
        if (!network) consumeContainer(player, slot, wanted);
        if (GTNHAdjust.debug) GTNHAdjust.LOG
            .info("[hologram] placed {} at {},{},{} from {}", fluid.getName(), x, y, z, network ? "AE" : "inventory");
        return PlaceResult.ACCEPT;
    }

    /** Select a container that can supply a whole source block without losing excess fluid. */
    private static int findContainer(EntityPlayerMP player, FluidStack wanted) {
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (stack == null) continue;
            FluidStack contained = GTUtility.getFluidForFilledItem(stack, true);
            if (contained == null || !contained.isFluidEqual(wanted) || contained.amount < SOURCE_AMOUNT) continue;
            if (stack.getItem() instanceof IFluidContainerItem container) {
                ItemStack one = stack.copy();
                one.stackSize = 1;
                FluidStack drained = container.drain(one, SOURCE_AMOUNT, false);
                if (drained != null && drained.amount == SOURCE_AMOUNT && drained.isFluidEqual(wanted)) return i;
            } else if (contained.amount == SOURCE_AMOUNT) return i;
        }
        return -1;
    }

    /** Drain exactly one container and return its empty or partially filled remainder. */
    private static void consumeContainer(EntityPlayerMP player, int slot, FluidStack wanted) {
        ItemStack stack = player.inventory.mainInventory[slot];
        ItemStack one = stack.copy();
        one.stackSize = 1;
        ItemStack remainder;
        if (one.getItem() instanceof IFluidContainerItem container) {
            container.drain(one, wanted.amount, true);
            remainder = one;
        } else {
            remainder = FluidContainerRegistry.drainFluidContainer(one);
        }
        if (--stack.stackSize == 0) player.inventory.mainInventory[slot] = remainder;
        else if (remainder != null && !player.inventory.addItemStackToInventory(remainder))
            player.dropPlayerItemWithRandomChoice(remainder, false);
        player.inventory.markDirty();
    }
}
