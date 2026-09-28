package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.storage.Bag;
import net.icxd.dungeons.storage.Storage;

/** {@code /sacks} and {@code /sax} (the wiki's Commands page, and "Also accessible via /sacks" in Your Bags): the Sack of Sacks. */
@CommandParameters(description = "Opens your Sack of Sacks", aliases = "sax")
public class SacksCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        Storage.openBag(source.getPlayer(), Bag.SACK_OF_SACKS);
    }
}
