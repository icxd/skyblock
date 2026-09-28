package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.storage.Storage;

/** {@code /loadouts}, {@code /loadout} and {@code /ld} (the wiki's Loadouts): Loadouts. */
@CommandParameters(description = "Opens your Loadouts", aliases = "loadout,ld")
public class LoadoutsCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        Storage.openLoadouts(source.getPlayer());
    }
}
