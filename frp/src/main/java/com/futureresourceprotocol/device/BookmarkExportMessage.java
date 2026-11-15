package com.futureresourceprotocol.device;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** A consistent, server-created snapshot captured when the export button is pressed. */
public final class BookmarkExportMessage implements IMessage {

    public long requestId;
    public MarkSyncMessage marks = new MarkSyncMessage();
    public Map<String, long[]> debts = new LinkedHashMap<>();

    public BookmarkExportMessage() {}

    private BookmarkExportMessage(long id, MarkSyncMessage marks, Map<String, long[]> debts) {
        this.requestId = id;
        this.marks = marks;
        this.debts = debts;
    }

    public static void sendSnapshot(EntityPlayerMP player, TerminalContainer container, WorldProtocol world, long id) {
        List<ItemStack> marks = world.markedItems();
        int parts = Math.max(1, (marks.size() + MarkSyncMessage.PER_PART - 1) / MarkSyncMessage.PER_PART);
        for (int part = 0; part < parts; part++) {
            int start = part * MarkSyncMessage.PER_PART;
            List<ItemStack> items = new ArrayList<>(
                marks.subList(start, Math.min(marks.size(), start + MarkSyncMessage.PER_PART)));
            Map<String, long[]> debts = new LinkedHashMap<>();
            for (ItemStack item : items) {
                String key = WorldProtocol.key(item);
                if (key != null) debts.put(key, new long[] { world.debt(key, false), world.debt(key, true) });
            }
            MarkSyncMessage data = new MarkSyncMessage(
                container.windowId,
                container.session(),
                world.markRevision(),
                part,
                parts,
                Math.min(world.markCount(), TerminalData.MAX),
                world.isValid(),
                items);
            FutureResourceProtocol.NETWORK.sendTo(new BookmarkExportMessage(id, data, debts), player);
        }
        FutureResourceProtocol.debug(
            "bookmark snapshot player={} marks={} revision={} parts={} request={}",
            player.getCommandSenderName(),
            marks.size(),
            world.markRevision(),
            parts,
            id);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(requestId);
        marks.toBytes(buf);
        buf.writeByte(debts.size());
        for (Map.Entry<String, long[]> entry : debts.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, entry.getKey());
            buf.writeLong(entry.getValue()[0]);
            buf.writeLong(entry.getValue()[1]);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        requestId = buf.readLong();
        marks = new MarkSyncMessage();
        marks.fromBytes(buf);
        int count = buf.readUnsignedByte();
        if (count > MarkSyncMessage.PER_PART) throw new IllegalArgumentException("Invalid FRP export size");
        debts = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String key = ByteBufUtils.readUTF8String(buf);
            long uhv = buf.readLong(), uxv = buf.readLong();
            if (uhv < 0 || uxv < 0) throw new IllegalArgumentException("Invalid FRP debt");
            debts.put(key, new long[] { uhv, uxv });
        }
    }

    public static final class Handler implements IMessageHandler<BookmarkExportMessage, IMessage> {

        @Override
        public IMessage onMessage(BookmarkExportMessage message, MessageContext ctx) {
            ClientDebtSync.receiveExport(message);
            return null;
        }
    }
}
