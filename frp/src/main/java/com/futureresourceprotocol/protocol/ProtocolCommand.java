package com.futureresourceprotocol.protocol;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

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
        return "/frp status|held|settle|milestone uhv|milestone umv confirm";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    private void say(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText("[FRP] " + text));
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        WorldProtocol world = WorldProtocol.get();
        if (args.length == 1 && args[0].equals("status")) {
            say(
                sender,
                "UHV debt=" + world.stageDebt(false)
                    + ", UMV debt="
                    + world.stageDebt(true)
                    + ", UHV milestone="
                    + world.isUhvMilestone()
                    + ", terminated/locked="
                    + world.isTerminated());
            return;
        }
        if (!(sender instanceof EntityPlayerMP)) {
            say(sender, "A player must hold the item.");
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) sender;
        ItemStack held = player.getHeldItem();
        if (held == null) {
            say(sender, "Hold an item first.");
            return;
        }
        String key = WorldProtocol.key(held);
        if (args.length == 1 && args[0].equals("held")) {
            say(
                sender,
                key + " UHV debt="
                    + world.debt(key, false)
                    + ", UMV debt="
                    + world.debt(key, true)
                    + ", cached class="
                    + world.cachedClass(key));
        } else if (args.length == 1 && args[0].equals("settle")) {
            long used = world.settle(key, held.stackSize);
            held.stackSize -= (int) used;
            if (held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            player.inventory.markDirty();
            say(sender, "Settled " + used + " of " + key);
            FutureResourceProtocol.LOG
                .info("FRP operator {} settled {} of {}", sender.getCommandSenderName(), used, key);
        } else if ((args.length == 2 || args.length == 3) && args[0].equals("milestone")) {
            boolean uhv = args[1].equals("uhv"), umv = args[1].equals("umv");
            if (umv && (args.length != 3 || !args[2].equals("confirm"))) {
                say(sender, "This permanently ends normal advances. Use /frp milestone umv confirm.");
                return;
            }
            if (!(uhv || umv) || !(uhv ? ItemList.Hull_MAX : ItemList.Hull_UMV).isStackEqual(held, false, true)) {
                say(sender, "Hold the matching machine hull.");
                return;
            }
            boolean ok = uhv ? world.completeUhv() : world.completeUmv();
            if (ok) {
                held.stackSize--;
                if (held.stackSize <= 0) player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                player.inventory.markDirty();
                FutureResourceProtocol.LOG
                    .info("FRP operator {} submitted {} milestone", sender.getCommandSenderName(), args[1]);
            }
            say(sender, ok ? "Milestone completed." : "Milestone requirements not met.");
        } else say(sender, getCommandUsage(sender));
    }
}
