package net.icxd.dungeons.scoreboard;

import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.SkyBlockServer;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.economy.Coins;
import net.icxd.dungeons.region.RegionType;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.SkyBlockTime;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The sidebar, updated every second. Each player has their own scoreboard. */
public class ScoreboardRunnable implements Runnable {
    /** The date is Hypixel's, in US Eastern time: "09/26/26". */
    private static final ZoneId HYPIXEL_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yy");
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    /** How much the purse and bits just changed. Main thread. */
    private static final Map<UUID, SidebarChange> coinChanges = new HashMap<>();
    private static final Map<UUID, SidebarChange> bitChanges = new HashMap<>();

    /** Their purse and bits start over (another profile's aren't a change). Main thread. */
    public static void forget(UUID player) {
        coinChanges.remove(player);
        bitChanges.remove(player);
    }

    @Override
    public void run() {
        boards.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        coinChanges.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        bitChanges.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        for (Player player : Bukkit.getOnlinePlayers()) {
            User user = User.cached(player.getUniqueId());
            if (user == null || !user.isLoaded()) continue;
            show(player, lines(player, user));
        }
    }

    private List<String> lines(Player player, User user) {
        SkyBlockServer server = Dungeons.getSkyBlockServer();
        String dateLine = "&7" + DATE.format(ZonedDateTime.now(HYPIXEL_ZONE)) + " &8" + server.getName();
        SkyBlockTime time = SkyBlockTime.now();
        String season = "&f " + time.date();
        String clock = " &7" + time.clock();
        RunManager runs = Dungeons.getRunManager();
        DungeonRun run = runs == null ? null : runs.runOf(player);
        UUID id = player.getUniqueId();
        if (run != null) {
            // The run's sidebar has no purse, and what they earn in it isn't a change after it: the
            // first purse after a run shows none (research coins.md 1.2, R1 04:37.0, R2 04:04.6).
            forget(id);
            // Indoors, without the sun or moon.
            return run.sidebar(player, dateLine, season, clock);
        }
        clock += time.isDay() ? " &e\u2600" : " &b\u263d";

        double coinsNow = user.getCoins();
        int bitsNow = user.getBits();
        String coins = purseLine(coinsNow, coinChanges.computeIfAbsent(id, i -> new SidebarChange()).update(coinsNow));
        Double bitsChange = bitChanges.computeIfAbsent(id, i -> new SidebarChange()).update(bitsNow);

        List<String> lines = new ArrayList<>();
        lines.add(dateLine);
        lines.add("&0");
        if (server.getServerType() == ServerType.DUNGEONS) {
            lines.add("&7 \u23e3 &c" + server.getServerType().getDisplayName());
        } else if (server.getServerType() == ServerType.DUNGEON_HUB) {
            lines.add(season);
            lines.add(clock);
            lines.add("&7 \u23e3 &c" + server.getServerType().getDisplayName());
        } else {
            lines.add(season);
            lines.add(clock);
            RegionType region = PlayerSession.of(player).getRegion();
            lines.add("&7 \u23e3 &7" + (region != null ? region : RegionType.getRegionType(player.getLocation())).displayName());
        }
        lines.add("&7");
        lines.add(coins);
        // No bits, no line (a new profile's sidebar).
        if (bitsNow > 0) lines.add(bitsLine(bitsNow, bitsChange));
        lines.add("&8");
        lines.add("&ewww.hypixel.net");
        return lines;
    }

    /** "&fPurse: &657,690,425", then " &e(+5)" for a change (research coins.md 1.1). */
    static String purseLine(double coins, Double change) {
        return "&fPurse: &6" + Coins.sidebar(coins) + (change == null ? "" : " &e(" + Coins.signed(change) + ")");
    }

    /** "&fBits: &b14,321", then " &3(+545)" for a change. */
    static String bitsLine(int bits, Double change) {
        return "&fBits: &b" + Utils.getFormattedNumber(bits) + (change == null ? "" : " &3(" + Coins.signed(change) + ")");
    }

    private void show(Player player, List<String> lines) {
        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), id -> {
            Scoreboard created = Bukkit.getScoreboardManager().getNewScoreboard();
            // TODO: add animation for the title.
            Objective objective = created.registerNewObjective("sidebar", Criteria.DUMMY, Text.line("&e&lSKYBLOCK"));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            return created;
        });
        Objective objective = board.getObjective("sidebar");
        Set<String> shown = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            String entry = Utils.color(lines.get(i));
            // Each line is a score entry, so blank lines (and any other repeats) need telling apart.
            while (shown.contains(entry)) entry += "\u00a7r";
            objective.getScore(entry).setScore(lines.size() - i);
            shown.add(entry);
        }
        for (String entry : board.getEntries()) {
            if (!shown.contains(entry)) board.resetScores(entry);
        }
        if (player.getScoreboard() != board) player.setScoreboard(board);
    }
}
