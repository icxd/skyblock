package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.menu.StatsMenu;

/** {@code /stats}: Stats & Equipment ("Also accessible via /stats", the SkyBlock Menu's item says). */
@CommandParameters(description = "Opens Stats & Equipment")
public class StatsCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        new StatsMenu(source.getPlayer()).open(source.getPlayer());
    }
}
