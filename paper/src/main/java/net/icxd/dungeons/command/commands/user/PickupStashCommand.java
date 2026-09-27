package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.user.ItemStash;

/** {@code /pickupstash}: what's in your item stash, into your inventory (as much as fits). */
@CommandParameters(aliases = "pickupstash", description = "Picks up the items in your item stash")
public class PickupStashCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() != null) ItemStash.pickUp(source.getPlayer());
    }
}
