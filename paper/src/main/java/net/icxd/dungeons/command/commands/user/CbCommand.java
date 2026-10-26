package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.Callbacks;
import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;

/** {@code /cb <id>}: what a clickable chat line does (see {@link Callbacks}), as Hypixel's. */
@CommandParameters(description = "Runs what a clicked chat line does")
public class CbCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() != null && args.length == 1) Callbacks.run(source.getPlayer(), args[0]);
    }
}
