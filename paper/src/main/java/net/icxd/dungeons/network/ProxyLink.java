package net.icxd.dungeons.network;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import net.icxd.dungeons.common.ProxyMessage;
import net.icxd.dungeons.user.UserStore;

/**
 * This server's side of the proxy's plugin-message channel (see {@link ProxyMessage}): the proxy
 * asks for a player's data to be handed off before moving them, and to be taken back if the move
 * fails. Servers can ask the proxy to send a player somewhere.
 */
public final class ProxyLink implements PluginMessageListener {
    /** If a move fails without the proxy saying so, the data is taken back after this anyway. */
    private static final long FALLBACK_RECLAIM_TICKS = 15 * 20;

    private final Plugin plugin;
    private final UserStore users;

    public ProxyLink(Plugin plugin, UserStore users) {
        this.plugin = plugin;
        this.users = users;
        Messenger messenger = plugin.getServer().getMessenger();
        messenger.registerIncomingPluginChannel(plugin, ProxyMessage.CHANNEL, this);
        messenger.registerOutgoingPluginChannel(plugin, ProxyMessage.CHANNEL);
    }

    /** Main thread. */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!channel.equals(ProxyMessage.CHANNEL)) return;
        ProxyMessage message = ProxyMessage.decode(data);
        if (message == null || !message.player().equals(player.getUniqueId())) return;
        switch (message.kind()) {
            case HANDOFF -> users.handOff(player).whenComplete((done, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                send(player, new ProxyMessage(ProxyMessage.Kind.HANDED_OFF, player.getUniqueId(), message.argument()));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) users.reclaim(player);
                }, FALLBACK_RECLAIM_TICKS);
            }));
            case RECLAIM -> users.reclaim(player);
            default -> {
            }
        }
    }

    /** Asks the proxy to send the player to a server type ({@code LOBBY}) or {@code server:NAME}. */
    public void send(Player player, String destination) {
        send(player, new ProxyMessage(ProxyMessage.Kind.SEND, player.getUniqueId(), destination));
    }

    private void send(Player player, ProxyMessage message) {
        player.sendPluginMessage(plugin, ProxyMessage.CHANNEL, message.encode());
    }
}
