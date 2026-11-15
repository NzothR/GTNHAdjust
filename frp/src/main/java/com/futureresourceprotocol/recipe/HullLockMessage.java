package com.futureresourceprotocol.recipe;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Server-authoritative hull permissions, sent on login and whenever the lock state changes. */
public final class HullLockMessage implements IMessage {

    public boolean enabled;
    public int mask;

    public HullLockMessage() {}

    public HullLockMessage(boolean enabled, int mask) {
        this.enabled = enabled;
        this.mask = mask;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeByte(mask);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        enabled = buf.readBoolean();
        mask = buf.readUnsignedByte();
        if ((mask & ~(HullRecipeLocks.UHV | HullRecipeLocks.UXV)) != 0)
            throw new IllegalArgumentException("Invalid hull lock state");
    }

    public static final class Handler implements IMessageHandler<HullLockMessage, IMessage> {

        @Override
        public IMessage onMessage(HullLockMessage message, MessageContext ctx) {
            ClientHullLocks.receive(message);
            return null;
        }
    }
}
