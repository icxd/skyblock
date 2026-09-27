package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.profile.ProfileManagementMenu;

/** {@code /profiles}: Profile Management, to make, switch and delete profiles. */
@CommandParameters(description = "Opens Profile Management")
public class ProfilesCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        new ProfileManagementMenu(source.getPlayer()).open(source.getPlayer());
    }
}
