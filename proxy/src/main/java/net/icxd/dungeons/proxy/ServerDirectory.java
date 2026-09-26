package net.icxd.dungeons.proxy;

import static com.mongodb.client.model.Filters.ne;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;
import org.slf4j.Logger;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.icxd.dungeons.common.Runs;
import net.icxd.dungeons.common.ServerType;

/**
 * What each backend server is and how busy it is, from the heartbeats the Paper servers write to
 * the {@code servers} collection and the dungeon runs in {@code runs}. Refreshed every couple of
 * seconds; lookups never touch the database. Without a database it knows nothing, and players go
 * where Velocity's own settings send them.
 */
public final class ServerDirectory {
    /** A backend as its last heartbeat described it. */
    public record Server(String name, ServerType type, int players, boolean up, int runs) {
    }

    private final ProxyServer proxy;
    private final Logger logger;
    private final MongoCollection<Document> servers;
    private final MongoCollection<Document> runs;
    private volatile Map<String, Server> known = Map.of();
    /** Runs started since the last refresh, so two parties in a row don't both pick the same "empty" server. */
    private final Map<String, Integer> justStarted = new ConcurrentHashMap<>();
    private boolean warned;

    ServerDirectory(ProxyServer proxy, Logger logger, MongoCollection<Document> servers, MongoCollection<Document> runs) {
        this.proxy = proxy;
        this.logger = logger;
        this.servers = servers;
        this.runs = runs;
    }

    /** Off the proxy's threads (it waits on the database). */
    void refresh() {
        if (servers == null) return;
        try {
            Map<String, Integer> active = new HashMap<>();
            for (Document run : runs.find(ne(Runs.STATE, Runs.ENDED)).projection(Projections.include(Runs.SERVER))) {
                active.merge(run.getString(Runs.SERVER), 1, Integer::sum);
            }
            long now = System.currentTimeMillis();
            Map<String, Server> fresh = new HashMap<>();
            for (Document doc : servers.find()) {
                String name = doc.getString("_id");
                ServerType type;
                try {
                    type = ServerType.valueOf(doc.getString("type"));
                } catch (IllegalArgumentException | NullPointerException e) {
                    continue;
                }
                Date seen = doc.getDate("seen");
                boolean up = seen != null && now - seen.getTime() < Runs.SERVER_STOPPED_AFTER_MILLIS;
                Number players = doc.get("players", Number.class);
                fresh.put(name, new Server(name, type, players == null ? 0 : players.intValue(), up, active.getOrDefault(name, 0)));
            }
            known = fresh;
            justStarted.clear();
            warned = false;
        } catch (RuntimeException e) {
            if (!warned) logger.warn("Couldn't read the server list from MongoDB: {}", e.toString());
            warned = true;
        }
    }

    public Optional<Server> get(String name) {
        return Optional.ofNullable(known.get(name));
    }

    public Optional<ServerType> typeOf(RegisteredServer server) {
        return get(server.getServerInfo().getName()).map(Server::type);
    }

    /** Running servers of this type that the proxy knows about. */
    public List<Server> up(ServerType type) {
        return known.values().stream()
                .filter(s -> s.type() == type && s.up() && proxy.getServer(s.name()).isPresent())
                .toList();
    }

    /** The emptiest running server of this type, other than {@code except} (may be null). */
    public Optional<RegisteredServer> leastLoaded(ServerType type, RegisteredServer except) {
        String skip = except == null ? null : except.getServerInfo().getName();
        return up(type).stream()
                .filter(s -> !s.name().equals(skip))
                .min(Comparator.comparingInt(Server::players))
                .flatMap(s -> proxy.getServer(s.name()));
    }

    /** The dungeon server with the fewest runs, if one has room for another. */
    public Optional<RegisteredServer> dungeonServerWithRoom(int maxRuns) {
        return up(ServerType.DUNGEONS).stream()
                .filter(s -> runs(s) < maxRuns)
                .min(Comparator.comparingInt(this::runs).thenComparingInt(Server::players))
                .flatMap(s -> proxy.getServer(s.name()));
    }

    public void countRun(String server) {
        justStarted.merge(server, 1, Integer::sum);
    }

    private int runs(Server server) {
        return server.runs() + justStarted.getOrDefault(server.name(), 0);
    }
}
