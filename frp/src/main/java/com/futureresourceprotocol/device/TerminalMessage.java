package com.futureresourceprotocol.device;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;
import com.futureresourceprotocol.recipe.RecipeIndex;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class TerminalMessage implements IMessage {

    private int action;
    private int windowId;
    private ItemStack stack;

    public TerminalMessage() {}

    public TerminalMessage(int action, int windowId, ItemStack stack) {
        this.action = action;
        this.windowId = windowId;
        this.stack = stack;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readUnsignedByte();
        windowId = buf.readInt();
        stack = ByteBufUtils.readItemStack(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(windowId);
        ByteBufUtils.writeItemStack(buf, stack);
    }

    public static final class Handler implements IMessageHandler<TerminalMessage, IMessage> {

        private static final Queue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();

        @Override
        public IMessage onMessage(TerminalMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            QUEUE.add(() -> handle(player, message));
            return null;
        }

        @SubscribeEvent
        public void onTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            Runnable task;
            while ((task = QUEUE.poll()) != null) {
                try {
                    task.run();
                } catch (RuntimeException ex) {
                    FutureResourceProtocol.LOG.error("FRP terminal request failed", ex);
                }
            }
        }

        private static void handle(EntityPlayerMP player, TerminalMessage message) {
            if (!(player.openContainer instanceof TerminalContainer)) return;
            TerminalContainer container = (TerminalContainer) player.openContainer;
            if (container.windowId != message.windowId || !container.canInteractWith(player)
                || message.stack == null
                || message.action < 0
                || message.action > 3) return;
            ItemStack terminal = player.inventory.getStackInSlot(container.heldSlot);
            List<ItemStack> marks = TerminalData.read(terminal);
            ItemStack requested = message.stack.copy();
            requested.stackSize = 1;
            int index = -1;
            for (int i = 0; i < marks.size(); i++) if (TerminalData.same(marks.get(i), requested)) {
                index = i;
                break;
            }
            WorldProtocol world = WorldProtocol.get();
            String key = WorldProtocol.key(requested);
            FutureResourceProtocol.debug(
                "request player={} action={} key={} markIndex={} count={} marking={}",
                player.getCommandSenderName(),
                message.action,
                key,
                index,
                marks.size(),
                container.marking);
            if (message.action == 0 && container.marking) {
                boolean eligible = RecipeIndex.eligible(requested);
                boolean umv = eligible && RecipeIndex.classify(requested, world);
                if (!eligible || !world.canAdvance(umv) || index >= 0 || marks.size() >= TerminalData.MAX) {
                    player.addChatMessage(
                        new ChatComponentText("[FRP] Cannot mark: unavailable, forbidden, duplicate, or full."));
                    FutureResourceProtocol.debug(
                        "mark rejected eligible={} umv={} permission={} duplicate={} size={}",
                        eligible,
                        umv,
                        world.canAdvance(umv),
                        index >= 0,
                        marks.size());
                    return;
                }
                marks.add(requested);
                if (!TerminalData.fitsNetwork(terminal, marks)) {
                    player.addChatMessage(new ChatComponentText("[FRP] Mark data is too large to safely synchronize."));
                    FutureResourceProtocol.debug("mark rejected: terminal NBT exceeds safe network size key={}", key);
                    return;
                }
                TerminalData.write(terminal, marks);
                FutureResourceProtocol.debug("marked {} class={} size={}", key, umv ? "UMV" : "UHV", marks.size());
            } else if (message.action == 1 && container.marking && index >= 0) {
                marks.remove(index);
                TerminalData.write(terminal, marks);
                FutureResourceProtocol.debug("removed mark {} size={}", key, marks.size());
            } else if ((message.action == 2 || message.action == 3) && !container.marking && index >= 0) {
                requested = marks.get(index)
                    .copy();
                boolean eligible = RecipeIndex.eligible(requested);
                boolean umv = eligible && RecipeIndex.classify(requested, world);
                int amount = message.action == 2 ? 1 : requested.getMaxStackSize();
                if (!eligible || !world.canRecord(key, umv, amount)) {
                    player.addChatMessage(new ChatComponentText("[FRP] Advance unavailable or protocol locked."));
                    FutureResourceProtocol
                        .debug("advance rejected eligible={} umv={} requested={}", eligible, umv, amount);
                    return;
                }
                long before = inventoryCount(player, requested);
                requested.stackSize = amount;
                player.inventory.addItemStackToInventory(requested);
                int delivered = (int) Math.max(0, Math.min(amount, inventoryCount(player, requested) - before));
                if (delivered > 0) world.advance(key, umv, delivered);
                FutureResourceProtocol.debug(
                    "delivery player={} key={} requested={} delivered={} remainder={}",
                    player.getCommandSenderName(),
                    key,
                    amount,
                    delivered,
                    requested.stackSize);
                if (delivered == 0) player.addChatMessage(new ChatComponentText("[FRP] Inventory is full."));
            }
            player.inventory.markDirty();
            player.openContainer.detectAndSendChanges();
        }

        private static long inventoryCount(EntityPlayerMP player, ItemStack item) {
            long total = 0;
            for (ItemStack stored : player.inventory.mainInventory)
                if (TerminalData.same(stored, item)) total += stored.stackSize;
            return total;
        }
    }
}
