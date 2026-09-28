package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.leveling.GuideMenu;

/** {@code /skyblockxp} (the SkyBlock XP Guide's "Also accessible via /skyblockxp"): the guide at their stage. */
@CommandParameters(description = "Opens the SkyBlock XP Guide")
public class SkyBlockXpCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        GuideMenu.openCurrent(source.getPlayer());
    }
}
