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
    private static final ResourceLocation CRAFT_ID = new ResourceLocation("gtnhadjust", "quest_ae_craft");
    private static final ResourceLocation SNAPSHOT_ID = new ResourceLocation("gtnhadjust", "quest_ae_patterns");
    private static final ThreadLocal<EntityPlayerMP> CURRENT = new ThreadLocal<>();

    private QuestAeNetwork() {}

    public static void init() {
        if (cpw.mods.fml.common.FMLCommonHandler.instance()
            .getSide()
            .isClient()) registerClient();
        PacketTypeRegistry.INSTANCE.registerServerHandler(SNAPSHOT_ID, message -> {
            EntityPlayerMP player = message.getSecond();
            NBTTagCompound data = message.getFirst();
            if (!data.hasKey("most", 4) || !data.hasKey("least", 4)) return;
            IQuest quest = QuestDatabase.INSTANCE.get(new UUID(data.getLong("most"), data.getLong("least")));
            if (quest == null || !quest.isUnlocked(betterquesting.api.api.QuestingAPI.getQuestingUUID(player))) return;
            NBTTagCompound reply = bq_standard.tasks.QuestAeDetector.craftableSnapshot(player, quest);
            reply.setLong("most", data.getLong("most"));
            reply.setLong("least", data.getLong("least"));
            reply.setLong("nonce", data.getLong("nonce"));
            PacketSender.INSTANCE.sendToPlayers(new QuestingPacket(SNAPSHOT_ID, reply), player);
        });
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(new QuestCraftQueue());
        PacketTypeRegistry.INSTANCE.registerServerHandler(CRAFT_ID, message -> {
            EntityPlayerMP player = message.getSecond();
            NBTTagCompound nbt = message.getFirst();
            if (!nbt.hasKey("most", 4) || !nbt.hasKey("least", 4)) return;
            UUID id = new UUID(nbt.getLong("most"), nbt.getLong("least"));
            IQuest quest = QuestDatabase.INSTANCE.get(id);
            if (quest != null && quest.canSubmit(player)) QuestCraftQueue.start(player, id, quest);
        });
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

    @SideOnly(Side.CLIENT)
    private static void registerClient() {
        PacketTypeRegistry.INSTANCE.registerClientHandler(SNAPSHOT_ID, QuestCraftSlots::acceptSnapshot);
    }

    @SideOnly(Side.CLIENT)
    public static void requestSnapshot(UUID quest, long nonce) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setLong("most", quest.getMostSignificantBits());
        nbt.setLong("least", quest.getLeastSignificantBits());
        nbt.setLong("nonce", nonce);
        PacketSender.INSTANCE.sendToServer(new QuestingPacket(SNAPSHOT_ID, nbt));
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

    @SideOnly(Side.CLIENT)
    public static void startCraftQueue(UUID quest) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setLong("most", quest.getMostSignificantBits());
        nbt.setLong("least", quest.getLeastSignificantBits());
        PacketSender.INSTANCE.sendToServer(new QuestingPacket(CRAFT_ID, nbt));
    }

    public static void unavailable(EntityPlayerMP player) {
        player.addChatMessage(new ChatComponentTranslation("gtnhadjust.quest.ae_unavailable"));
    }
}
