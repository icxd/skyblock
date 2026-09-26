package net.icxd.dungeons.proxy;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Sorts.descending;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bson.Document;
import org.slf4j.Logger;

import com.mongodb.client.MongoCollection;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.icxd.dungeons.common.Runs;
import net.icxd.dungeons.common.ServerType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Moving players between servers. Every move (ours, {@code /server}, {@code /send}, other plugins')
 * first has the old server save and release the player's data, so the new server can load it; if
 * the move then fails, the old server takes it back. New players and players kicked from a server
 * go to the emptiest hub, and players who drop out of a dungeon run go back into it.
 */
public final class Transfers {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Messenger messenger;
    private final ServerDirectory directory;
    private final MongoCollection<Document> runs;

    Transfers(ProxyServer proxy, Logger logger, Messenger messenger, ServerDirectory directory, MongoCollection<Document> runs) {
        this.proxy = proxy;
        this.logger = logger;
        this.messenger = messenger;
        this.directory = directory;
        this.runs = runs;
    }

    /** Last, once other plugins have had their say on whether the move happens at all. */
    @Subscribe(order = PostOrder.CUSTOM, priority = Short.MIN_VALUE)
    public EventTask onPreConnect(ServerPreConnectEvent event) {
        RegisteredServer target = event.getResult().getServer().orElse(null);
        Optional<ServerConnection> current = event.getPlayer().getCurrentServer();
        if (target == null || current.isEmpty() || current.get().getServer().equals(target)) return null;
        return EventTask.resumeWhenComplete(messenger.handOff(current.get()));
    }

    @Subscribe
    public void onKicked(KickedFromServerEvent event) {
        Player player = event.getPlayer();
        Optional<ServerConnection> current = player.getCurrentServer();
        if (event.kickedDuringServerConnect()) {
            // Still on the old server, which let go of their data for the move.
            current.filter(c -> !c.getServer().equals(event.getServer())).ifPresent(messenger::reclaim);
            return;
        }
        if (event.kickedDuringLogin()) return;
        // Kicked while playing (the server stopped, or kicked them): off to a hub, unless it was a hub
        // and there's no other.
        Optional<RegisteredServer> hub = directory.leastLoaded(ServerType.LOBBY, event.getServer());
        if (hub.isEmpty()) return;
        String reason = event.getServerKickReason().map(r -> PlainTextComponentSerializer.plainText().serialize(r)).orElse("");
        Component message = Chat.text("§cA kick occurred in your connection, so you have been routed to the Hub.");
        if (!reason.isBlank()) message = Chat.join(message, Component.newline(), Chat.text("§7" + reason));
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(hub.get(), message));
    }

    @Subscribe
    public EventTask onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        return EventTask.async(() -> {
            Optional<RegisteredServer> run = activeRunServer(event.getPlayer());
            if (run.isPresent()) {
                event.setInitialServer(run.get());
                return;
            }
            directory.leastLoaded(ServerType.LOBBY, null).ifPresent(event::setInitialServer);
        });
    }

    /** The dungeon server running a run the player dropped out of, if it's still going. */
    private Optional<RegisteredServer> activeRunServer(Player player) {
        if (runs == null) return Optional.empty();
        try {
            Document run = runs.find(and(eq(Runs.MEMBERS, player.getUniqueId().toString()), in(Runs.STATE, Runs.ASSIGNED, Runs.RUNNING)))
                    .sort(descending(Runs.CREATED)).first();
            if (run == null) return Optional.empty();
            String server = run.getString(Runs.SERVER);
            if (directory.get(server).map(ServerDirectory.Server::up).orElse(false)) return proxy.getServer(server);
        } catch (RuntimeException e) {
            logger.warn("Couldn't look up {}'s dungeon run: {}", player.getUsername(), e.toString());
        }
        return Optional.empty();
    }

    /**
     * Sends a player to a server, telling them if it doesn't work. Completes with whether they got
     * there.
     */
    public CompletableFuture<Boolean> connect(Player player, RegisteredServer target) {
        Chat.send(player, "§7Sending to server " + target.getServerInfo().getName() + "...");
        return player.createConnectionRequest(target).connect().handle((result, error) -> {
            if (error == null && (result.isSuccessful() || result.getStatus() == ConnectionRequestBuilder.Status.ALREADY_CONNECTED)) {
                return true;
            }
            player.getCurrentServer().ifPresent(messenger::reclaim);
            String reason = error != null ? error.getMessage()
                    : result.getReasonComponent().map(r -> PlainTextComponentSerializer.plainText().serialize(r)).orElse(describe(result.getStatus()));
            Chat.send(player, "§cCouldn't send you to " + target.getServerInfo().getName() + (reason == null || reason.isBlank() ? "." : ": " + reason));
            return false;
        });
    }

    private static String describe(ConnectionRequestBuilder.Status status) {
        return switch (status) {
            case CONNECTION_IN_PROGRESS -> "you're already being sent somewhere";
            case CONNECTION_CANCELLED -> "the move was cancelled";
            case SERVER_DISCONNECTED -> "the server isn't reachable";
            default -> "";
        };
    }

    /** To the emptiest server of a type (not the one they're on); a hub if it's the Dungeon Hub and there's none. */
    public CompletableFuture<Boolean> sendTo(Player player, ServerType type) {
        RegisteredServer here = player.getCurrentServer().map(ServerConnection::getServer).orElse(null);
        Optional<RegisteredServer> target = directory.leastLoaded(type, here);
        if (target.isEmpty() && type == ServerType.DUNGEON_HUB) target = directory.leastLoaded(ServerType.LOBBY, here);
        if (target.isEmpty()) {
            Chat.send(player, "§cThere's no " + type.getDisplayName() + " server to send you to right now.");
            return CompletableFuture.completedFuture(false);
        }
        return connect(player, target.get());
    }

    /** A server's SEND request: a server type ({@code LOBBY}, {@code DUNGEON_HUB}) or {@code server:NAME}. */
    void sendRequested(ServerConnection from, String destination) {
        Player player = from.getPlayer();
        if (destination.startsWith("server:")) {
            proxy.getServer(destination.substring("server:".length())).ifPresentOrElse(
                    server -> connect(player, server),
                    () -> logger.warn("{} asked to send {} to unknown server {}", from.getServerInfo().getName(), player.getUsername(), destination));
            return;
        }
        try {
            sendTo(player, ServerType.valueOf(destination));
        } catch (IllegalArgumentException e) {
            logger.warn("{} asked to send {} to {}, which isn't a server type", from.getServerInfo().getName(), player.getUsername(), destination);
        }
    }
}
