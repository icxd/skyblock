package net.icxd.dungeons.dungeons;

import lombok.Getter;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One dungeon run. Players are kept by UUID, so someone who reconnects is still in it.
 */
@Getter
public class Dungeon {
    private final UUID id;
    private final UUID owner;
    private final Set<UUID> players;
    private final Set<UUID> deadPlayers;
    private final DungeonFloor floor;
    private DungeonState state;

    private long prepareTime;
    private long startTime;
    private long endTime;

    public Dungeon(Player owner, DungeonFloor floor) {
        this.id = UUID.randomUUID();
        this.owner = owner.getUniqueId();
        this.players = new LinkedHashSet<>();
        this.deadPlayers = new LinkedHashSet<>();
        this.floor = floor;
        this.state = DungeonState.NONE;

        this.prepareTime = 0;
        this.startTime = 0;
        this.endTime = 0;
    }

    /** Alive or dead. */
    public boolean isMember(UUID player) {
        return players.contains(player) || deadPlayers.contains(player);
    }

    public void prepare() {
        this.state = DungeonState.PREPARING;
        this.prepareTime = System.currentTimeMillis();
        // TODO: Fix message
        send("&aDungeon " + this.id + " is preparing...");
    }

    public void start() {
        this.state = DungeonState.STARTED;
        this.startTime = System.currentTimeMillis();
        // TODO: Fix message
        send("&aDungeon " + this.id + " has started!");
    }

    public void end() {
        this.state = DungeonState.ENDED;
        this.endTime = System.currentTimeMillis();

        for (UUID dead : Set.copyOf(deadPlayers)) revivePlayer(dead);

        // TODO: Fix message
        send("&aDungeon " + this.id + " has ended and took " + Utils.formatTime(this.endTime - this.startTime) + " to complete!");
    }

    public void killPlayer(Player player) {
        players.remove(player.getUniqueId());
        deadPlayers.add(player.getUniqueId());
        // TODO: Fix message
        player.sendMessage(Utils.color("&cYou have died!"));
    }

    public void revivePlayer(UUID player) {
        deadPlayers.remove(player);
        players.add(player);
        Player online = Bukkit.getPlayer(player);
        // TODO: Fix message
        send("&a" + (online != null ? online.getName() : player) + " has been revived!");
    }

    /** To every member who's online. */
    private void send(String message) {
        String colored = Utils.color(message);
        for (UUID member : players) {
            Player online = Bukkit.getPlayer(member);
            if (online != null) online.sendMessage(colored);
        }
        for (UUID member : deadPlayers) {
            Player online = Bukkit.getPlayer(member);
            if (online != null) online.sendMessage(colored);
        }
    }
}
