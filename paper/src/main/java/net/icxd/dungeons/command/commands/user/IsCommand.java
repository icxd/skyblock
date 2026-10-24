package net.icxd.dungeons.command.commands.user;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.island.Islands;
import net.icxd.dungeons.listeners.InventorySyncListener;

/** {@code /is} ({@code /island}): to the island of the profile they play on, here or on an island server. */
@CommandParameters(description = "Warps you to your island", aliases = "island")
public class IsCommand extends SCommand {
    @Override
    public void run(CommandSource source, String[] args) {
        if (source.getPlayer() == null || InventorySyncListener.frozen(source.getPlayer())) return;
        if (Islands.isOwnIsland(source.getPlayer(), source.getPlayer().getWorld())) {
            source.getPlayer().teleport(Islands.spawn(source.getPlayer().getWorld()));
            return;
        }
        Islands.send(source.getPlayer());
    }
}
