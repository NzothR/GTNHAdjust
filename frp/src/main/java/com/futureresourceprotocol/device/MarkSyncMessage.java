package com.futureresourceprotocol.device;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.GameRegistry;
import io.netty.buffer.ByteBuf;

/** A bounded part of the authoritative world mark snapshot. */
public final class MarkSyncMessage implements IMessage {

    public static final int PER_PART = 32;
    public int windowId, part, parts, total;
    public long revision;
    public String session;
    public boolean valid;
    public List<ItemStack> items = new ArrayList<>();

    public MarkSyncMessage() {}

    public MarkSyncMessage(int windowId, String session, long revision, int part, int parts, int total, boolean valid,
        List<ItemStack> items) {
        this.windowId = windowId;
        this.session = session;
        this.revision = revision;
        this.part = part;
        this.parts = parts;
        this.total = total;
        this.valid = valid;
        this.items = items;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(windowId);
        ByteBufUtils.writeUTF8String(buf, session);
        buf.writeLong(revision);
        buf.writeShort(part);
        buf.writeShort(parts);
        buf.writeShort(total);
        buf.writeBoolean(valid);
        buf.writeByte(items.size());
        for (ItemStack stack : items) {
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            ByteBufUtils.writeUTF8String(buf, id.toString());
            buf.writeInt(stack.getItemDamage());
            ByteBufUtils.writeTag(buf, stack.getTagCompound());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        windowId = buf.readInt();
        session = ByteBufUtils.readUTF8String(buf);
        revision = buf.readLong();
        part = buf.readUnsignedShort();
        parts = buf.readUnsignedShort();
        total = buf.readUnsignedShort();
        valid = buf.readBoolean();
        int count = buf.readUnsignedByte();
        if (parts < 1 || parts > (TerminalData.MAX + PER_PART - 1) / PER_PART
            || part >= parts
            || total > TerminalData.MAX
            || count > PER_PART) throw new IllegalArgumentException("Invalid FRP mark snapshot");
        items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = ByteBufUtils.readUTF8String(buf);
            int meta = buf.readInt();
            NBTTagCompound tag = ByteBufUtils.readTag(buf);
            int colon = id.indexOf(':');
            Item item = colon > 0 && colon < id.length() - 1
                ? GameRegistry.findItem(id.substring(0, colon), id.substring(colon + 1))
                : null;
            if (item != null) {
                ItemStack stack = new ItemStack(item, 1, meta);
                if (tag != null) stack.setTagCompound(tag);
                items.add(stack);
            }
        }
    }

    public static final class Handler implements IMessageHandler<MarkSyncMessage, IMessage> {

        @Override
        public IMessage onMessage(MarkSyncMessage message, MessageContext context) {
            ClientDebtSync.receiveMarks(message);
            return null;
        }
    }
}
