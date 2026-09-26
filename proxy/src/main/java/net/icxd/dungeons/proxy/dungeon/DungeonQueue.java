package net.icxd.dungeons.proxy.dungeon;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.slf4j.Logger;

import com.mongodb.client.MongoCollection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.common.Runs;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.proxy.Chat;
import net.icxd.dungeons.proxy.Profiles;
import net.icxd.dungeons.proxy.ServerDirectory;
import net.icxd.dungeons.proxy.Transfers;
import net.icxd.dungeons.proxy.party.Party;
import net.icxd.dungeons.proxy.party.PartyManager;

/**
 * Starting dungeon runs: a party (or a player on their own) goes to the dungeon server with the
 * fewest runs, as long as it has fewer than {@code dungeons.runs-per-server}. The run is written to
 * the {@code runs} collection first, so that server knows who's coming and can set the run up. When
 * every dungeon server is full, parties wait in line.
 */
public final class DungeonQueue {
    public static final int MAX_PLAYERS = 5;
    private static final long WAIT_MINUTES = 5;
    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII"};

    private record Entry(UUID leader, List<UUID> members, int partyVersion, DungeonFloor floor, long since) {
    }

    private final ProxyServer proxy;
    private final Logger logger;
    private final ServerDirectory directory;
    private final Transfers transfers;
    private final PartyManager parties;
    private final Profiles profiles;
    private final MongoCollection<Document> runs;
    private final int runsPerServer;
    private final List<Entry> waiting = new ArrayList<>();

    public DungeonQueue(Object plugin, ProxyServer proxy, Logger logger, ServerDirectory directory, Transfers transfers,
                        PartyManager parties, Profiles profiles, MongoCollection<Document> runs, int runsPerServer) {
        this.proxy = proxy;
        this.logger = logger;
        this.directory = directory;
        this.transfers = transfers;
        this.parties = parties;
        this.profiles = profiles;
        this.runs = runs;
        this.runsPerServer = runsPerServer;
        proxy.getScheduler().buildTask(plugin, this::tick).repeat(2, TimeUnit.SECONDS).schedule();
    }

    /** "The Catacombs, Floor VII", "MM The Catacombs, Floor III", "The Catacombs, Entrance". */
    public static String displayName(DungeonFloor floor) {
        String where = floor.getNumber() == 0 ? "Entrance" : "Floor " + ROMAN[floor.getNumber() - 1];
        return (floor.isMasterMode() ? "MM " : "") + "The Catacombs, " + where;
    }

    public void join(Player player, DungeonFloor floor) {
        UUID uuid = player.getUniqueId();
        Optional<Party.Snapshot> party = parties.partyOf(uuid);
        List<UUID> members = List.of(uuid);
        int version = -1;
        if (party.isPresent()) {
            Party.Snapshot p = party.get();
            if (!p.leader().equals(uuid)) {
                Chat.send(player, "§cOnly the party leader can enter a dungeon!");
                return;
            }
            List<String> offline = p.members().stream()
                    .filter(m -> p.offline().contains(m) || proxy.getPlayer(m).isEmpty())
                    .map(profiles::display)
                    .toList();
            if (!offline.isEmpty()) {
                Chat.send(player, "§cEveryone in the party has to be online to enter a dungeon. Offline: " + String.join("§c, ", offline));
                return;
            }
            if (p.members().size() > MAX_PLAYERS) {
                Chat.send(player, "§cYour party has too many members! A dungeon takes at most " + MAX_PLAYERS + " players.");
                return;
            }
            members = p.members();
            version = p.version();
        }
        if (runs == null) {
            Chat.send(player, "§cDungeons aren't available right now.");
            return;
        }
        synchronized (this) {
            if (waiting.stream().anyMatch(e -> e.leader().equals(uuid))) {
                Chat.send(player, "§cYou're already waiting for a dungeon server.");
                return;
            }
            Entry entry = new Entry(uuid, members, version, floor, System.currentTimeMillis());
            Optional<RegisteredServer> server = directory.dungeonServerWithRoom(runsPerServer);
            if (server.isPresent() && waiting.isEmpty()) {
                start(entry, server.get());
                return;
            }
            waiting.add(entry);
            String why = directory.up(ServerType.DUNGEONS).isEmpty() ? "No dungeon server is running right now" : "Every dungeon server is full right now";
            tell(entry, "§e" + why + ". You're §c#" + waiting.size() + " §ein line for " + displayName(floor) + "§e.");
        }
    }

    /** Parties at the front of the line go as soon as there's room; ones that changed or waited too long drop out. */
    private synchronized void tick() {
        long now = System.currentTimeMillis();
        for (Iterator<Entry> it = waiting.iterator(); it.hasNext(); ) {
            Entry entry = it.next();
            String problem = stale(entry, now);
            if (problem != null) {
                it.remove();
                tell(entry, problem);
                continue;
            }
            Optional<RegisteredServer> server = directory.dungeonServerWithRoom(runsPerServer);
            if (server.isEmpty()) break; // everyone behind keeps their place
            it.remove();
            start(entry, server.get());
        }
    }

    private String stale(Entry entry, long now) {
        if (now - entry.since() > TimeUnit.MINUTES.toMillis(WAIT_MINUTES)) {
            return "§cNo dungeon server had room for " + WAIT_MINUTES + " minutes, so you left the line. Try again in a bit.";
        }
        if (entry.members().stream().anyMatch(m -> proxy.getPlayer(m).isEmpty())) {
            return "§cSomeone in the party went offline, so it left the line for " + displayName(entry.floor()) + "§c.";
        }
        int version = parties.partyOf(entry.leader()).map(Party.Snapshot::version).orElse(-1);
        boolean stillLeader = parties.partyOf(entry.leader()).map(p -> p.leader().equals(entry.leader())).orElse(entry.partyVersion() == -1);
        if (version != entry.partyVersion() || !stillLeader) {
            return "§cThe party changed, so it left the line for " + displayName(entry.floor()) + "§c.";
        }
        return null;
    }

    private void start(Entry entry, RegisteredServer server) {
        String name = server.getServerInfo().getName();
        Document run = new Document("_id", UUID.randomUUID().toString())
                .append(Runs.SERVER, name)
                .append(Runs.FLOOR, entry.floor().name())
                .append(Runs.LEADER, entry.leader().toString())
                .append(Runs.MEMBERS, entry.members().stream().map(UUID::toString).toList())
                .append(Runs.STATE, Runs.ASSIGNED)
                .append(Runs.CREATED, new Date());
        try {
            runs.insertOne(run);
        } catch (RuntimeException e) {
            logger.error("Couldn't save a dungeon run for {}", profiles.get(entry.leader()).name(), e);
            tell(entry, "§cCouldn't start the dungeon, try again in a moment.");
            return;
        }
        directory.countRun(name);
        tell(entry, profiles.display(entry.leader()) + " §eentered §c" + displayName(entry.floor()) + "§e!");
        for (UUID member : entry.members()) proxy.getPlayer(member).ifPresent(p -> transfers.connect(p, server));
    }

    private void tell(Entry entry, String message) {
        for (UUID member : entry.members()) proxy.getPlayer(member).ifPresent(p -> Chat.send(p, message));
    }
}
