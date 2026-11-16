package com.futureresourceprotocol.device;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.protocol.WorldProtocol;
import com.futureresourceprotocol.recipe.RecipeIndex;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Bounded item batches from the selected client bookmark page; the server checks every mark. */
public final class BookmarkImportMessage implements IMessage {

    public static final int BATCH_SIZE = 32;
    private int windowId;
    private String session;
    private boolean first, last;
    private boolean structure;
    private List<ItemStack> items;

    public BookmarkImportMessage() {}

    public BookmarkImportMessage(int windowId, String session, boolean first, boolean last, List<ItemStack> items) {
        this(windowId, session, first, last, items, false);
    }

    /** The source flag applies independent feature/mode rules to the same bounded mark-only transport. */
    public BookmarkImportMessage(int windowId, String session, boolean first, boolean last, List<ItemStack> items,
        boolean structure) {
        this.windowId = windowId;
        this.session = session;
        this.first = first;
        this.last = last;
        this.items = items;
        this.structure = structure;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(windowId);
        ByteBufUtils.writeUTF8String(buf, session);
        buf.writeBoolean(first);
        buf.writeBoolean(last);
        buf.writeBoolean(structure);
        buf.writeByte(items.size());
        for (ItemStack item : items) {
            ItemStack identity = item.copy();
            identity.setTagCompound(null);
            ByteBufUtils.writeItemStack(buf, identity);
            ByteBufUtils.writeTag(buf, item.getTagCompound());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        windowId = buf.readInt();
        session = ByteBufUtils.readUTF8String(buf);
        first = buf.readBoolean();
        last = buf.readBoolean();
        structure = buf.readBoolean();
        int count = buf.readUnsignedByte();
        if (count > BATCH_SIZE) throw new IllegalArgumentException("Invalid FRP bookmark import batch");
        items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ItemStack item = ByteBufUtils.readItemStack(buf);
            net.minecraft.nbt.NBTTagCompound tag = ByteBufUtils.readTag(buf);
            if (item != null) {
                item.stackSize = 1;
                item.setTagCompound(tag);
                items.add(item);
            }
        }
    }

    public static final class Handler implements IMessageHandler<BookmarkImportMessage, IMessage> {

        @Override
        public IMessage onMessage(BookmarkImportMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            TerminalMessage.Handler.enqueue(() -> importMarks(player, message));
            return null;
        }

        /** Import references only, without borrowing resources or changing existing timestamps. */
        private void importMarks(EntityPlayerMP player, BookmarkImportMessage message) {
            if (!(player.openContainer instanceof TerminalContainer)) return;
            TerminalContainer container = (TerminalContainer) player.openContainer;
            boolean allowed = message.structure ? FutureResourceProtocol.structureOverlayMarksEnabled
                : FutureResourceProtocol.neiBookmarkImportEnabled && container.marking;
            if (!allowed || container.windowId != message.windowId
                || !container.session()
                    .equals(message.session)
                || !container.canInteractWith(player)) return;
            WorldProtocol world = WorldProtocol.get();
            if (message.first) {
                container.importAdded = 0;
                container.importSkipped = 0;
            }
            for (ItemStack item : message.items) {
                if (TerminalData.safeTag(item) && RecipeIndex.eligible(item, world)
                    && world.canAdvance(RecipeIndex.classify(item, world))
                    && world.addMark(item)) container.importAdded++;
                else container.importSkipped++;
            }
            if (message.last) {
                player.addChatMessage(
                    new ChatComponentTranslation(
                        message.structure ? "frp.chat.structure_imported" : "frp.chat.bookmark_imported",
                        container.importAdded,
                        container.importSkipped));
                FutureResourceProtocol.debug(
                    "bookmark import player={} added={} skipped={}",
                    player.getCommandSenderName(),
                    container.importAdded,
                    container.importSkipped);
            }
            container.detectAndSendChanges();
        }
    }
}
