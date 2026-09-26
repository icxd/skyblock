package net.icxd.dungeons.proxy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import net.icxd.dungeons.common.ProxyMessage;

/**
 * The plugin-message channel to the Paper servers (see {@link ProxyMessage}). Messages ride on a
 * player's connection, so they can only reach a server that player is on.
 */
final class Messenger {
    static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(ProxyMessage.CHANNEL);
    /** How long a move waits for the old server to save the player before going ahead anyway. */
    private static final long HANDOFF_TIMEOUT_SECONDS = 3;

    private final Map<String, CompletableFuture<Boolean>> handoffs = new ConcurrentHashMap<>();
    /** Called with a SEND request's player connection and its destination. */
    private final BiConsumer<ServerConnection, String> onSend;

    Messenger(ProxyServer proxy, BiConsumer<ServerConnection, String> onSend) {
        this.onSend = onSend;
        proxy.getChannelRegistrar().register(CHANNEL);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) return;
        // Never passed on: a client can't talk to the servers on this channel, and servers' messages are for us.
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection from)) return;
        ProxyMessage message = ProxyMessage.decode(event.getData());
        if (message == null || !message.player().equals(from.getPlayer().getUniqueId())) return;
        switch (message.kind()) {
            case HANDED_OFF -> {
                CompletableFuture<Boolean> done = handoffs.remove(message.argument());
                if (done != null) done.complete(true);
            }
            case SEND -> onSend.accept(from, message.argument());
            default -> {
            }
        }
    }

    /**
     * Asks the server the player is on to save and release their data. Completes with true when it
     * has, or false if it didn't answer in time (then the next server's login waits for the data
     * itself, see the Paper plugin's UserStore).
     */
    CompletableFuture<Boolean> handOff(ServerConnection connection) {
        String id = UUID.randomUUID().toString();
        CompletableFuture<Boolean> done = new CompletableFuture<>();
        handoffs.put(id, done);
        UUID player = connection.getPlayer().getUniqueId();
        if (!connection.sendPluginMessage(CHANNEL, new ProxyMessage(ProxyMessage.Kind.HANDOFF, player, id).encode())) {
            handoffs.remove(id);
            return CompletableFuture.completedFuture(false);
        }
        return done.completeOnTimeout(false, HANDOFF_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((ok, error) -> handoffs.remove(id));
    }

    /** The player didn't move after all: the server they're still on takes their data back. */
    void reclaim(ServerConnection connection) {
        UUID player = connection.getPlayer().getUniqueId();
        connection.sendPluginMessage(CHANNEL, new ProxyMessage(ProxyMessage.Kind.RECLAIM, player, "").encode());
    }
}
