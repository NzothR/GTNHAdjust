package com.futureresourceprotocol.device;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

import com.futureresourceprotocol.FutureResourceProtocol;
import com.futureresourceprotocol.integration.ae2.AeDebtRepayment;
import com.futureresourceprotocol.protocol.WorldProtocol;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public final class TerminalControlMessage implements IMessage {

    public static final int REPAY_AE = 1;
    public static final int REMOVE_DEBT_FREE = 2;
    private int windowId;
    private int action;

    public TerminalControlMessage() {}

    public TerminalControlMessage(int windowId, int action) {
        this.windowId = windowId;
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        windowId = buf.readInt();
        action = buf.readUnsignedByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(windowId);
        buf.writeByte(action);
    }

    public static final class Handler implements IMessageHandler<TerminalControlMessage, IMessage> {

        @Override
        public IMessage onMessage(TerminalControlMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            TerminalMessage.Handler.enqueue(() -> handle(player, message));
            return null;
        }

        private void handle(EntityPlayerMP player, TerminalControlMessage message) {
            if (!(player.openContainer instanceof TerminalContainer)) return;
            TerminalContainer container = (TerminalContainer) player.openContainer;
            if (container.windowId != message.windowId || !container.canInteractWith(player)) return;
            WorldProtocol world = WorldProtocol.get();
            if (!world.isValid()) {
                player.addChatMessage(new ChatComponentTranslation("frp.chat.world_invalid"));
                return;
            }
            if (message.action == REMOVE_DEBT_FREE) {
                int removed = world.removeDebtFreeMarks();
                player.addChatMessage(new ChatComponentTranslation("frp.chat.marks_removed", removed));
            } else if (message.action == REPAY_AE && FutureResourceProtocol.aeRepaymentEnabled
                && FutureResourceProtocol.portableRepaymentEnabled) {
                    AeDebtRepayment.repay(player, world);
                } else if (message.action == REPAY_AE) {
                    player.addChatMessage(new ChatComponentTranslation("frp.chat.ae_disabled"));
                }
            player.openContainer.detectAndSendChanges();
        }
    }
}
