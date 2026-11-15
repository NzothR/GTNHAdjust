package com.futureresourceprotocol.device;

import java.util.LinkedHashMap;
import java.util.Map;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server-to-client display data. It never changes the authoritative ledger. */
public final class DebtSyncMessage implements IMessage {

    public int windowId;
    public String session;
    public boolean valid;
    public Map<String, long[]> debts = new LinkedHashMap<>();

    public DebtSyncMessage() {}

    public DebtSyncMessage(int windowId, String session, boolean valid, Map<String, long[]> debts) {
        this.windowId = windowId;
        this.session = session;
        this.valid = valid;
        this.debts = debts;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(windowId);
        ByteBufUtils.writeUTF8String(buf, session);
        buf.writeBoolean(valid);
        buf.writeShort(debts.size());
        for (Map.Entry<String, long[]> entry : debts.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, entry.getKey());
            buf.writeLong(entry.getValue()[0]);
            buf.writeLong(entry.getValue()[1]);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        windowId = buf.readInt();
        session = ByteBufUtils.readUTF8String(buf);
        valid = buf.readBoolean();
        int count = buf.readUnsignedShort();
        if (count > TerminalData.MAX) throw new IllegalArgumentException("Too many FRP debt entries");
        debts = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String key = ByteBufUtils.readUTF8String(buf);
            long uhv = buf.readLong(), uxv = buf.readLong();
            if (uhv < 0 || uxv < 0) throw new IllegalArgumentException("Negative FRP debt");
            debts.put(key, new long[] { uhv, uxv });
        }
    }

    public static final class Handler implements IMessageHandler<DebtSyncMessage, IMessage> {

        @Override
        public IMessage onMessage(DebtSyncMessage message, MessageContext context) {
            ClientDebtSync.receive(message);
            return null;
        }
    }
}
