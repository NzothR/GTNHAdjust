package com.futureresourceprotocol.device;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.util.ChatComponentTranslation;

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
        net.minecraft.nbt.NBTTagCompound fullTag = ByteBufUtils.readTag(buf);
        if (stack != null) stack.setTagCompound(fullTag);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(windowId);
        ItemStack identity = stack == null ? null : stack.copy();
        if (identity != null) identity.setTagCompound(null);
        ByteBufUtils.writeItemStack(buf, identity);
        ByteBufUtils.writeTag(buf, stack == null ? null : stack.getTagCompound());
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
                || message.action > AdvanceInteraction.INVENTORY_ONE) return;
            ItemStack requested = message.stack.copy();
            requested.stackSize = 1;
            WorldProtocol world = WorldProtocol.get();
            boolean marked = world.hasMark(requested);
            String key = WorldProtocol.key(requested);
            FutureResourceProtocol.debug(
                "request player={} action={} key={} marked={} count={} marking={} bypass={}",
                player.getCommandSenderName(),
                message.action,
                key,
                marked,
                world.markCount(),
                container.marking,
                world.allowAllItems());
            if (message.action == 0 && container.marking) {
                boolean eligible = RecipeIndex.eligible(requested, world);
                boolean umv = eligible && RecipeIndex.classify(requested, world);
                if (!eligible || !world.canAdvance(umv) || marked || world.markCount() >= WorldProtocol.MAX_MARKS) {
                    String reason = marked ? "frp.chat.mark_duplicate"
                        : world.markCount() >= WorldProtocol.MAX_MARKS ? "frp.chat.mark_full"
                            : !eligible ? "frp.chat.mark_ineligible" : "frp.chat.mark_locked";
                    player.addChatMessage(new ChatComponentTranslation(reason));
                    FutureResourceProtocol.debug(
                        "mark rejected eligible={} umv={} permission={} duplicate={} size={}",
                        eligible,
                        umv,
                        world.canAdvance(umv),
                        marked,
                        world.markCount());
                    return;
                }
                if (!TerminalData.safeTag(requested)) {
                    player.addChatMessage(new ChatComponentTranslation("frp.chat.item_too_large"));
                    FutureResourceProtocol.debug("mark rejected: item NBT exceeds safe network size key={}", key);
                    return;
                }
                world.addMark(requested);
                FutureResourceProtocol.debug("marked {} class={} size={}", key, umv ? "UMV" : "UHV", world.markCount());
            } else if (message.action == 1 && container.marking && marked) {
                if (!world.removeMark(requested)) {
                    player.addChatMessage(new ChatComponentTranslation("frp.chat.mark_has_debt"));
                    FutureResourceProtocol.debug(
                        "remove mark rejected key={} UHV={} UMV={}",
                        key,
                        world.debt(key, false),
                        world.debt(key, true));
                    return;
                }
                FutureResourceProtocol.debug("removed mark {} size={}", key, world.markCount());
            } else if (message.action == AdvanceInteraction.REPAY_CURSOR && !container.marking
                && FutureResourceProtocol.portableRepaymentEnabled) {
                    ItemStack cursor = player.inventory.getItemStack();
                    if (!TerminalData.same(cursor, message.stack)) return;
                    long paid = world.settle(WorldProtocol.key(cursor), cursor.stackSize);
                    cursor.stackSize -= (int) paid;
                    if (cursor.stackSize == 0) player.inventory.setItemStack(null);
                    player.playerNetServerHandler
                        .sendPacket(new S2FPacketSetSlot(-1, -1, player.inventory.getItemStack()));
                    player.addChatMessage(new ChatComponentTranslation("frp.chat.repaid", paid));
                    FutureResourceProtocol.debug(
                        "cursor repayment player={} key={} paid={} remaining={}",
                        player.getCommandSenderName(),
                        key,
                        paid,
                        cursor.stackSize);
                } else if (AdvanceInteraction.isAdvance(message.action) && !container.marking && marked) {
                    boolean eligible = RecipeIndex.eligible(requested, world);
                    boolean umv = eligible && RecipeIndex.classify(requested, world);
                    int amount = AdvanceInteraction.amount(message.action, requested.getMaxStackSize());
                    ItemStack cursor = player.inventory.getItemStack();
                    if (AdvanceInteraction.toCursor(message.action) && cursor != null) {
                        if (!TerminalData.same(cursor, requested)) {
                            player.addChatMessage(new ChatComponentTranslation("frp.chat.cursor_mismatch"));
                            return;
                        }
                        amount = Math.min(amount, Math.max(0, cursor.getMaxStackSize() - cursor.stackSize));
                    }
                    if (amount <= 0) {
                        player.addChatMessage(new ChatComponentTranslation("frp.chat.cursor_full"));
                        return;
                    }
                    if (!eligible || !world.canRecord(key, umv, amount)) {
                        player.addChatMessage(new ChatComponentTranslation("frp.chat.advance_locked"));
                        FutureResourceProtocol
                            .debug("advance rejected eligible={} umv={} requested={}", eligible, umv, amount);
                        return;
                    }
                    int delivered;
                    if (AdvanceInteraction.toCursor(message.action)) {
                        if (cursor == null) {
                            requested.stackSize = amount;
                            player.inventory.setItemStack(requested);
                        } else cursor.stackSize += amount;
                        delivered = amount;
                    } else {
                        long before = inventoryCount(player, requested);
                        requested.stackSize = amount;
                        player.inventory.addItemStackToInventory(requested);
                        delivered = (int) Math.max(0, Math.min(amount, inventoryCount(player, requested) - before));
                    }
                    if (delivered > 0) world.advance(key, umv, delivered);
                    if (AdvanceInteraction.toCursor(message.action)) player.playerNetServerHandler
                        .sendPacket(new S2FPacketSetSlot(-1, -1, player.inventory.getItemStack()));
                    FutureResourceProtocol.debug(
                        "delivery player={} key={} requested={} delivered={} remainder={} destination={}",
                        player.getCommandSenderName(),
                        key,
                        amount,
                        delivered,
                        AdvanceInteraction.toCursor(message.action) ? 0 : requested.stackSize,
                        AdvanceInteraction.toCursor(message.action) ? "cursor" : "inventory");
                    if (delivered == 0) player.addChatMessage(new ChatComponentTranslation("frp.chat.inventory_full"));
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
