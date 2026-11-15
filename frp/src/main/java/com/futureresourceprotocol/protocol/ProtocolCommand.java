package com.futureresourceprotocol.protocol;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;

import com.futureresourceprotocol.FutureResourceProtocol;

import gregtech.api.enums.ItemList;

/** Operator-only development interface; all item operations use real held stacks. */
public final class ProtocolCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "frp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/frp status|held|allowall [on|off|toggle]|milestone uhv|milestone uxv confirm";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    private void say(ICommandSender sender, String key, Object... args) {
        sender.addChatMessage(new ChatComponentTranslation(key, args));
    }

    private ChatComponentTranslation yesNo(boolean value) {
        return new ChatComponentTranslation(value ? "frp.value.yes" : "frp.value.no");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        WorldProtocol world = WorldProtocol.get();
        if (args.length == 1 && args[0].equals("status")) {
            say(
                sender,
                "frp.chat.command_status",
                world.stageDebt(false),
                world.stageDebt(true),
                yesNo(world.isUhvMilestone()),
                yesNo(world.isTerminated()));
            return;
        }
        if (args.length >= 1 && args[0].equals("allowall")) {
            if (args.length == 1) {
                say(sender, "frp.chat.allow_all_status", yesNo(world.allowAllItems()));
            } else if (args.length == 2 && (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off")
                || args[1].equalsIgnoreCase("toggle"))) {
                    boolean enabled = args[1].equalsIgnoreCase("toggle") ? !world.allowAllItems()
                        : args[1].equalsIgnoreCase("on");
                    if (world.setAllowAllItems(enabled)) {
                        say(sender, "frp.chat.allow_all_status", yesNo(enabled));
                        FutureResourceProtocol.LOG
                            .info("FRP operator {} set world allowAllItems={}", sender.getCommandSenderName(), enabled);
                    } else say(sender, "frp.chat.allow_all_invalid");
                } else say(sender, "frp.chat.command_usage", getCommandUsage(sender));
            return;
        }
        if (!(sender instanceof EntityPlayerMP)) {
            say(sender, "frp.chat.command_player");
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        ItemStack held = player.getHeldItem();
        if (held == null) {
            say(sender, "frp.chat.command_hold");
            return;
        }
        String key = WorldProtocol.key(held);
        if (args.length == 1 && args[0].equals("held")) {
            Boolean classification = world.cachedClass(key);
            String classKey = classification == null ? "frp.value.unset"
                : classification ? "frp.value.uxv" : "frp.value.uhv";
            say(
                sender,
                "frp.chat.command_held",
                key,
                world.debt(key, false),
                world.debt(key, true),
                new ChatComponentTranslation(classKey));
        } else if ((args.length == 2 || args.length == 3) && args[0].equals("milestone")) {
            boolean uhv = args[1].equals("uhv"), uxv = args[1].equals("uxv");
            if (uxv && (args.length != 3 || !args[2].equals("confirm"))) {
                say(sender, "frp.chat.command_confirm");
                return;
            }
            if (!(uhv || uxv) || !(uhv ? ItemList.Hull_MAX : ItemList.Hull_UXV).isStackEqual(held, false, true)) {
                say(sender, "frp.chat.command_hull");
                return;
            }
            boolean ok = uhv ? world.completeUhv() : world.completeUxv();
            if (ok) {
                held.stackSize--;
                if (held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                player.inventory.markDirty();
                FutureResourceProtocol.LOG
                    .info("FRP operator {} submitted {} milestone", sender.getCommandSenderName(), args[1]);
            }
            say(sender, ok ? "frp.chat.command_milestone_done" : "frp.chat.command_milestone_failed");
        } else say(sender, "frp.chat.command_usage", getCommandUsage(sender));
    }
}
