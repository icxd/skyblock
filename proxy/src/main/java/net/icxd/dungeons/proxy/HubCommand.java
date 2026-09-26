package net.icxd.dungeons.proxy;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.icxd.dungeons.common.ServerType;

/** {@code /hub} ({@code /lobby}, {@code /l}): to the emptiest hub server. */
final class HubCommand implements SimpleCommand {
    private final Transfers transfers;
    private final ServerDirectory directory;

    HubCommand(Transfers transfers, ServerDirectory directory) {
        this.transfers = transfers;
        this.directory = directory;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            Chat.send(invocation.source(), "§cOnly players can go to the Hub.");
            return;
        }
        boolean onHub = player.getCurrentServer()
                .flatMap(c -> directory.typeOf(c.getServer()))
                .map(type -> type == ServerType.LOBBY)
                .orElse(false);
        if (onHub) {
            Chat.send(player, "§cYou are already in the Hub!");
            return;
        }
        transfers.sendTo(player, ServerType.LOBBY);
    }
}
