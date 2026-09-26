package net.icxd.dungeons.command.commands.user;

import org.bukkit.ChatColor;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;

/** {@code /showextrastats}: the "EXTRA STATS" at the end of a dungeon run. */
@CommandParameters(aliases = "showextrastats", description = "Shows the extra stats of the dungeon you just finished")
public class ShowExtraStatsCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        RunManager runs = Dungeons.getRunManager();
        DungeonRun run = runs == null || source.getPlayer() == null ? null : runs.runOf(source.getPlayer());
        if (run == null) {
            source.send(ChatColor.RED + "You're not in a dungeon!");
            return;
        }
        run.showExtraStats(source.getPlayer());
    }
}
