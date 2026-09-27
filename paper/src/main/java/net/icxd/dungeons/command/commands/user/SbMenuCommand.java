package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.menu.SkyBlockMenu;

/** {@code /sbmenu} and {@code /viewsbmenu} (Hypixel's two, the fandom wiki's Commands page): the SkyBlock Menu. */
@CommandParameters(description = "Opens the SkyBlock Menu", aliases = "viewsbmenu")
public class SbMenuCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        new SkyBlockMenu(source.getPlayer()).open(source.getPlayer());
    }
}
