package net.icxd.dungeons.proxy;

import static com.mongodb.client.model.Filters.eq;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;
import org.slf4j.Logger;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.icxd.dungeons.common.Rank;

/**
 * Names and ranks of players the proxy has seen, from the users collection. Ranks decide the
 * prefixes in party messages and who may use the proxy's staff commands: STAFF gets every
 * permission, everyone else none of Velocity's own commands (so no {@code /server} past the
 * dungeon queue).
 */
public final class Profiles {
    public record Profile(UUID uuid, String name, Rank rank) {
        /** Rank prefix and name, like {@code §b[MVP§6+§b] Name} or {@code §7Name}. */
        public String display() {
            return rank.getPrefix() + name;
        }
    }

    private final ProxyServer proxy;
    private final Logger logger;
    private final MongoCollection<Document> users;
    private final Map<UUID, Profile> profiles = new ConcurrentHashMap<>();

    Profiles(ProxyServer proxy, Logger logger, MongoCollection<Document> users) {
        this.proxy = proxy;
        this.logger = logger;
        this.users = users;
    }

    @Subscribe
    public EventTask onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        profiles.put(player.getUniqueId(), new Profile(player.getUniqueId(), player.getUsername(), get(player.getUniqueId()).rank()));
        return EventTask.async(() -> load(player));
    }

    /** Ranks change on the servers (/pd); pick that up whenever someone changes server. */
    @Subscribe
    public EventTask onServerConnected(ServerPostConnectEvent event) {
        return EventTask.async(() -> load(event.getPlayer()));
    }

    @Subscribe
    public void onPermissionsSetup(PermissionsSetupEvent event) {
        if (!(event.getSubject() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        event.setProvider(subject -> permission -> {
            if (get(uuid).rank().isEqualOrStrongerThan(Rank.STAFF)) return Tristate.TRUE;
            return permission.startsWith("velocity.command.") ? Tristate.FALSE : Tristate.UNDEFINED;
        });
    }

    private void load(Player player) {
        if (users == null) return;
        try {
            Document doc = users.find(eq("uuid", player.getUniqueId().toString())).projection(Projections.include("rank")).first();
            Rank rank = doc == null ? Rank.DEFAULT : Rank.getRank(doc.getString("rank"));
            profiles.put(player.getUniqueId(), new Profile(player.getUniqueId(), player.getUsername(), rank));
        } catch (RuntimeException e) {
            logger.warn("Couldn't load {}'s rank: {}", player.getUsername(), e.toString());
        }
    }

    /** Someone the proxy has seen this session (their name and rank as of then), or a stand-in. */
    public Profile get(UUID uuid) {
        Profile profile = profiles.get(uuid);
        if (profile != null) return profile;
        String name = proxy.getPlayer(uuid).map(Player::getUsername).orElse(uuid.toString().substring(0, 8));
        return new Profile(uuid, name, Rank.DEFAULT);
    }

    public String display(UUID uuid) {
        return get(uuid).display();
    }

    /** An online player by name, ignoring case. */
    public Optional<Player> online(String name) {
        return proxy.getPlayer(name);
    }
}
