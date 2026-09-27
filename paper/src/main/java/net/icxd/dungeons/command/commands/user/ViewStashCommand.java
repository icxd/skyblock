package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.user.ItemStash;

/**
 * {@code /viewstash item}, what the stash reminder's lines run. Hypixel's shows the stash in a menu, which
 * isn't recorded: here it picks the items up, as the lines' hover says ("Click to pickup your items!").
 */
@CommandParameters(aliases = "viewstash", description = "Picks up the items in your item stash")
public class ViewStashCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() != null) ItemStash.pickUp(source.getPlayer());
    }
}
