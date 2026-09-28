package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.leveling.LevelingMenu;

/** {@code /levels} and Hypixel's other names for it (the fandom wiki's Commands page): SkyBlock Leveling. */
@CommandParameters(description = "Opens SkyBlock Leveling", aliases = "skyblocklevels, skyblocklevel, sblevels, sblevel, level")
public class LevelsCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        new LevelingMenu(source.getPlayer()).open(source.getPlayer());
    }
}
