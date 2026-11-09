package com.gtnhadjust.quest;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;

import betterquesting.api.network.QuestingPacket;
import betterquesting.api.questing.IQuest;
import betterquesting.network.PacketSender;
import betterquesting.network.PacketTypeRegistry;
import betterquesting.questing.QuestDatabase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class QuestAeNetwork {

    private static final ResourceLocation ID = new ResourceLocation("gtnhadjust", "quest_ae_detect");
    private static final ThreadLocal<EntityPlayerMP> CURRENT = new ThreadLocal<>();

    private QuestAeNetwork() {}

    public static void init() {
        PacketTypeRegistry.INSTANCE.registerServerHandler(ID, message -> {
            EntityPlayerMP player = message.getSecond();
            NBTTagCompound nbt = message.getFirst();
            if (!nbt.hasKey("most", 4) || !nbt.hasKey("least", 4)) return;
            UUID id = new UUID(nbt.getLong("most"), nbt.getLong("least"));
            IQuest quest = QuestDatabase.INSTANCE.get(id);
            if (quest == null || !quest.canSubmit(player)) return;
            if (CURRENT.get() != null) return;
            CURRENT.set(player);
            try {
                quest.detect(player);
            } finally {
                CURRENT.remove();
            }
        });
    }

    public static EntityPlayerMP current() {
        return CURRENT.get();
    }

    @SideOnly(Side.CLIENT)
    public static void request(UUID quest) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setLong("most", quest.getMostSignificantBits());
        nbt.setLong("least", quest.getLeastSignificantBits());
        PacketSender.INSTANCE.sendToServer(new QuestingPacket(ID, nbt));
    }

    public static void unavailable(EntityPlayerMP player) {
        player.addChatMessage(new ChatComponentTranslation("gtnhadjust.quest.ae_unavailable"));
    }
}
