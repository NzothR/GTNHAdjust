package com.futureresourceprotocol.device;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class TerminalModeMessage implements IMessage {

    public int windowId;
    public String session;
    public boolean marking;

    public TerminalModeMessage() {}

    public TerminalModeMessage(int windowId, String session, boolean marking) {
        this.windowId = windowId;
        this.session = session;
        this.marking = marking;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        windowId = buf.readInt();
        session = ByteBufUtils.readUTF8String(buf);
        marking = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(windowId);
        ByteBufUtils.writeUTF8String(buf, session);
        buf.writeBoolean(marking);
    }

    public static final class Handler implements IMessageHandler<TerminalModeMessage, IMessage> {

        @Override
        public IMessage onMessage(TerminalModeMessage message, MessageContext ctx) {
            ClientDebtSync.receiveMode(message);
            return null;
        }
    }
}
