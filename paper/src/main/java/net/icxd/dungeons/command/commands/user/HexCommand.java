package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.hex.Hex;

/**
 * {@code /hex} (and {@code /hecks}, {@code /thehex}, the wiki's Commands): The Hex, empty, for anyone. Hypixel's
 * needs the Cookie Buff and the Prosperous Museum rank, a Sandbox profile nothing (see HexRequirements); not in a
 * dungeon.
 */
@CommandParameters(aliases = "hecks,thehex", description = "Opens The Hex")
public class HexCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null) return;
        Hex.open(source.getPlayer());
    }
}
