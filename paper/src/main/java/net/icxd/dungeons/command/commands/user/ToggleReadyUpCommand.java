package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.dungeons.DungeonProfile;

/** {@code /togglereadyup}: whether you ready up by yourself when you arrive in a dungeon. */
@CommandParameters(aliases = "togglereadyup", description = "Toggles auto ready up in dungeons")
public class ToggleReadyUpCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        DungeonProfile.toggleAutoReadyUp(source.getPlayer(), source.getUser());
    }
}
