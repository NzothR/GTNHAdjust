package com.gtnhadjust.terminal;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import com.gtnhadjust.GTNHAdjust;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public final class TerminalNotifications {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("gtnha_notice");

    private TerminalNotifications() {}

    public static void init() {
        CHANNEL.registerMessage(Handler.class, Message.class, 0, Side.CLIENT);
        if (FMLCommonHandler.instance()
            .getSide()
            .isClient()) ClientNotice.init();
    }

    public static void report(EntityPlayer player, String message) {
        GTNHAdjust.LOG.info("Quick terminal: {}", message);
        if (!(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP recipient = (EntityPlayerMP) player;
        recipient.addChatMessage(new ChatComponentText(format(message)));
        CHANNEL.sendTo(new Message(message), recipient);
    }

    /** The client resolves the synchronized interface display name in its own language. */
    public static void reportUpload(EntityPlayer player, long id, String rawName, int slot) {
        String fallback = rawName == null || rawName.trim()
            .isEmpty() ? "接口 " + id : rawName;
        String message = "自动上传成功：" + fallback + "，样板槽 " + slot + "。";
        GTNHAdjust.LOG.info("Quick terminal: {} (interface id {})", message, id);
        if (!(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP recipient = (EntityPlayerMP) player;
        recipient.addChatMessage(new ChatComponentText(format(message)));
        CHANNEL.sendTo(new Message("\u0001" + id + "\u0002" + fallback + "\u0003" + slot), recipient);
    }

    public static String format(String message) {
        return (message.startsWith("自动上传成功：") ? "§a" : "§c") + "[样板终端] §f" + message;
    }

    public static final class Message implements IMessage {

        private String text;

        public Message() {}

        Message(String text) {
            this.text = text;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            text = ByteBufUtils.readUTF8String(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, text);
        }
    }

    public static final class Handler implements IMessageHandler<Message, IMessage> {

        @Override
        public IMessage onMessage(Message message, MessageContext context) {
            ClientNotice.show(message.text);
            return null;
        }
    }
}
