package com.gtnhadjust;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.config.Configuration;

final class SlamCycleCommand extends CommandBase {

    private final Configuration config;

    SlamCycleCommand(Configuration config) {
        this.config = config;
    }

    @Override
    public String getCommandName() {
        return "gtnhadjust";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/gtnhadjust slam [ticks] — Shielded Lagrangian Annihilation Matrix (SLAM) cycle interval";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    /** Report the active interval or save and apply a new interval immediately. */
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || args.length > 2 || !"slam".equalsIgnoreCase(args[0])) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        if (args.length == 2) {
            int ticks = parseIntBounded(sender, args[1], 1, Integer.MAX_VALUE);
            config.get("antimatterGenerator", "cycleTicks", 20)
                .set(ticks);
            config.save();
            GTNHAdjust.antimatterCycleTicks = ticks;
        }
        sender.addChatMessage(
            new ChatComponentText("SLAM cycle interval: " + GTNHAdjust.antimatterCycleTicks + " ticks"));
    }
}
