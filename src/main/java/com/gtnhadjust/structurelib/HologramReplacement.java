package com.gtnhadjust.structurelib;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.BlockEvent;

import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElement.BlocksToPlace;
import com.gtnewhorizon.structurelib.structure.IStructureElement.PlaceResult;
import com.gtnewhorizon.structurelib.structure.IStructureElementChain;
import com.gtnhadjust.GTNHAdjust;

/** Conservative channel-aware replacement; unknown or stateful block types are never removed. */
public final class HologramReplacement {

    private HologramReplacement() {}

    public static <T> PlaceResult tryReplace(IStructureElement<T> element, T controller, World world, int x, int y,
        int z, ItemStack trigger, AutoPlaceEnvironment env) {
        HologramReplacementContext state = HologramReplacementContext.current();
        if (state == null) return null;
        Collection<String> channels = channels(element);
        if (channels.size() != 1) return PlaceResult.SKIP;
        String channel = channels.iterator()
            .next();
        if (!ChannelDataAccessor.hasSubChannel(trigger, channel)) return PlaceResult.SKIP;
        if (!world.blockExists(x, y, z)) return PlaceResult.SKIP;
        Block old = world.getBlock(x, y, z);
        if (old == null || old == Blocks.air || old.isAir(world, x, y, z)) return null;
        if (world.getTileEntity(x, y, z) != null || element.couldBeValid(controller, world, x, y, z, trigger))
            return PlaceResult.SKIP;
        ItemStack target = onlyStack(element.getBlocksToPlace(controller, world, x, y, z, trigger, env));
        if (target == null || !(target.getItem() instanceof ItemBlock) || target.hasTagCompound())
            return PlaceResult.SKIP;
        int oldMeta = world.getBlockMetadata(x, y, z);
        Item oldItem = Item.getItemFromBlock(old);
        if (oldItem == null || old.getBlockHardness(world, x, y, z) < 0) return PlaceResult.SKIP;
        ItemStack oldStack = new ItemStack(oldItem, 1, old.damageDropped(oldMeta));
        boolean knownTier = false;
        for (int tier = 1; tier <= 64; tier++) {
            ItemStack alternate = trigger.copy();
            ChannelDataAccessor.setChannelData(alternate, channel, tier);
            if (!element.couldBeValid(controller, world, x, y, z, alternate)) continue;
            ItemStack expected = onlyStack(element.getBlocksToPlace(controller, world, x, y, z, alternate, env));
            if (sameItem(expected, oldStack)) {
                knownTier = true;
                break;
            }
        }
        if (!knownTier || sameItem(oldStack, target)) return PlaceResult.SKIP;
        ArrayList<ItemStack> drops = old.getDrops(world, x, y, z, oldMeta, 0);
        if (drops == null || drops.size() != 1 || drops.get(0).stackSize != 1 || !sameItem(drops.get(0), oldStack))
            return PlaceResult.SKIP;
        if (!(env.getActor() instanceof EntityPlayerMP player)) return PlaceResult.SKIP;
        state.attempted = true;
        IItemSource source = env.getSource();
        if (!source.takeOne(target, true)) {
            player.addChatMessage(new ChatComponentText("[全息投影仪] 缺少目标等级方块：" + target.getDisplayName()));
            return PlaceResult.REJECT;
        }
        if (!world.canMineBlock(player, x, y, z) || !player.canPlayerEdit(x, y, z, ForgeDirection.UP.ordinal(), target))
            return PlaceResult.REJECT;
        BlockEvent.BreakEvent event = ForgeHooks
            .onBlockBreakEvent(world, player.theItemInWorldManager.getGameType(), player, x, y, z);
        if (event == null || event.isCanceled()
            || world.getBlock(x, y, z) != old
            || world.getBlockMetadata(x, y, z) != oldMeta
            || world.getTileEntity(x, y, z) != null) {
            player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(x, y, z, world));
            return PlaceResult.REJECT;
        }
        if (!source.takeOne(target, false)) {
            player.playerNetServerHandler.sendPacket(new S23PacketBlockChange(x, y, z, world));
            return PlaceResult.REJECT;
        }
        boolean success = ReplacementTransaction.execute(new ReplacementTransaction.Steps() {

            @Override
            public boolean remove() {
                return old.removedByPlayer(world, player, x, y, z, false);
            }

            @Override
            public boolean isEmpty() {
                return world.isAirBlock(x, y, z);
            }

            @Override
            public boolean place() {
                ItemStack placed = target.copy();
                if (!placed.tryPlaceItemIntoWorld(player, world, x, y, z, ForgeDirection.UP.ordinal(), .5f, .5f, .5f))
                    return false;
                return world.getBlock(x, y, z) == Block.getBlockFromItem(target.getItem())
                    && element.couldBeValid(controller, world, x, y, z, trigger);
            }

            @Override
            public boolean isOriginal() {
                return world.getBlock(x, y, z) == old && world.getBlockMetadata(x, y, z) == oldMeta;
            }

            @Override
            public boolean restore() {
                return world.setBlock(x, y, z, old, oldMeta, 3);
            }

            @Override
            public void returnOriginal() {
                giveBack(player, drops.get(0));
            }

            @Override
            public void refundReplacement() {
                giveBack(player, target);
            }

            @Override
            public void markChanged() {
                state.changed = true;
            }
        });
        if (success && GTNHAdjust.debug) GTNHAdjust.LOG
            .info("[hologram] replaced {}:{} -> {} at {},{},{} channel={}", old, oldMeta, target, x, y, z, channel);
        return success ? PlaceResult.ACCEPT : PlaceResult.REJECT;
    }

    private static Collection<String> channels(IStructureElement<?> element) {
        Set<String> result = new LinkedHashSet<>();
        Set<IStructureElement<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        collectChannels(element, result, visited, 0);
        return result;
    }

    private static void collectChannels(IStructureElement<?> element, Set<String> result,
        Set<IStructureElement<?>> visited, int depth) {
        if (element == null || depth > 8 || !visited.add(element)) return;
        result.addAll(element.getChannels());
        if (element instanceof IStructureElementChain<?>chain) {
            for (IStructureElement<?> fallback : chain.fallbacks())
                collectChannels(fallback, result, visited, depth + 1);
        }
        // GT's trigger-transform wrapper delegates placement but omits getChannels().
        for (Class<?> type = element.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                    || !IStructureElement.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    collectChannels((IStructureElement<?>) field.get(element), result, visited, depth + 1);
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    if (GTNHAdjust.debug) GTNHAdjust.LOG.debug("[hologram] cannot inspect structure wrapper", failure);
                }
            }
        }
    }

    private static ItemStack onlyStack(BlocksToPlace blocks) {
        if (blocks == null || blocks.getStacks() == null) return null;
        ItemStack only = null;
        for (ItemStack candidate : blocks.getStacks()) {
            if (candidate == null || candidate.getItem() == null) return null;
            if (only != null && !sameItem(only, candidate)) return null;
            only = candidate.copy();
            only.stackSize = 1;
        }
        return only;
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return a != null && b != null && a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    private static void giveBack(EntityPlayerMP player, ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return;
        ItemStack remaining = stack.copy();
        if (player.inventory.addItemStackToInventory(remaining)) return;
        if (cpw.mods.fml.common.Loader.isModLoaded("appliedenergistics2"))
            remaining = AeAutoPlaceItemSource.returnToNetwork(player, remaining);
        if (remaining != null && remaining.stackSize > 0) player.dropPlayerItemWithRandomChoice(remaining, false);
    }
}
