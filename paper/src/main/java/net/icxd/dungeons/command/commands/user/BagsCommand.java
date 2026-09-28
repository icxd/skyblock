package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.storage.Storage;

/** {@code /bags} ("Also accessible via /bags", the SkyBlock Menu's item says): Your Bags. */
@CommandParameters(description = "Opens Your Bags")
public class BagsCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        Storage.openBags(source.getPlayer());
    }
}
