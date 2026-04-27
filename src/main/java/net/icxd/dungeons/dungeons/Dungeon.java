package net.icxd.dungeons.dungeons;

import lombok.Getter;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

@Getter
public class Dungeon {
    private UUID id;
    private Player owner;
    private ArrayList<Player> players;
    private ArrayList<Player> deadPlayers;
    private DungeonFloor floor;
    private DungeonState state;

    private long prepareTime;
    private long startTime;
    private long endTime;

    public Dungeon(Player owner, DungeonFloor floor) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.players = new ArrayList<>();
        this.deadPlayers = new ArrayList<>();
        this.floor = floor;
        this.state = DungeonState.NONE;

        this.prepareTime = 0;
        this.startTime = 0;
        this.endTime = 0;
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

        deadPlayers.forEach(this::revivePlayer);
        deadPlayers.clear();



        // TODO: Fix message
        send("&aDungeon " + this.id + " has ended and took " + Utils.formatTime(this.endTime - this.startTime) + " to complete!");
    }

    public void killPlayer(Player player) {
        players.remove(player);
        deadPlayers.add(player);
        // TODO: Fix message
        player.sendMessage(Utils.color("&cYou have died!"));
    }

    public void revivePlayer(Player player) {
        deadPlayers.remove(player);
        players.add(player);
        // TODO: Fix message
        send("&a" + player.getName() + " has been revived!");
    }

    private void send(String message) {
        players.forEach(player -> player.sendMessage(Utils.color(message)));
        deadPlayers.forEach(player -> player.sendMessage(Utils.color(message)));
    }

}
