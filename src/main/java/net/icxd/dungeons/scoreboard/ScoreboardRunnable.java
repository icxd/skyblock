package net.icxd.dungeons.scoreboard;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.SkyBlockServer;
import net.icxd.dungeons.region.Region;
import net.icxd.dungeons.region.RegionType;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The sidebar, updated every second. Each player has their own scoreboard. */
public class ScoreboardRunnable implements Runnable {
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final HashMap<UUID, Integer> coinsCache = new HashMap<>();
    private final HashMap<UUID, Integer> bitsCache = new HashMap<>();

    @Override
    public void run() {
        boards.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        coinsCache.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        bitsCache.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        for (Player player : Bukkit.getOnlinePlayers()) {
            User user = User.cached(player.getUniqueId());
            if (user == null || !user.isLoaded()) continue;
            show(player, lines(player, user));
        }
    }

    private List<String> lines(Player player, User user) {
        UUID id = player.getUniqueId();
        int coinsNow = user.getDocument().getInteger("coins");
        int bitsNow = user.getDocument().getInteger("bits");
        StringBuilder coins = new StringBuilder("&fPurse: &6").append(Utils.getFormattedNumber(coinsCache.getOrDefault(id, coinsNow)));
        StringBuilder bits = new StringBuilder("&fBits: &b").append(Utils.getFormattedNumber(bitsCache.getOrDefault(id, bitsNow)));
        Integer oldCoins = coinsCache.put(id, coinsNow);
        Integer oldBits = bitsCache.put(id, bitsNow);
        if (oldCoins != null && oldCoins != coinsNow) {
            int difference = coinsNow - oldCoins;
            coins.append(" &e(").append(difference > 0 ? "+" : "").append(Utils.getFormattedNumber(difference)).append(")");
        }
        if (oldBits != null && oldBits != bitsNow) {
            int difference = bitsNow - oldBits;
            bits.append(" &3(").append(difference > 0 ? "+" : "").append(Utils.getFormattedNumber(difference)).append(")");
        }

        SkyBlockServer server = Dungeons.getSkyBlockServer();
        List<String> lines = new ArrayList<>();
        lines.add("&7" + Utils.getDateFormatted(new Date()) + " &8" + server.getName());
        lines.add("&0");
        if (server.getServerType() == SkyBlockServer.Type.DUNGEONS) {
            lines.add("&7 \u23e3 &c" + server.getServerType().getDisplayName());
        } else {
            lines.add("&fEarly Summer 23rd");
            lines.add("&e \u2600 &79:30am");
            Region region = Region.regionCache.get(id);
            lines.add("&7 \u23e3 &7" + (region != null ? region : new Region(RegionType.getRegionType(player.getLocation()))));
        }
        lines.add("&7");
        lines.add(coins.toString());
        lines.add(bits.toString());
        lines.add("&8");
        lines.add("&ewww.hypixel.net");
        return lines;
    }

    private void show(Player player, List<String> lines) {
        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), id -> {
            Scoreboard created = Bukkit.getScoreboardManager().getNewScoreboard();
            // TODO: add animation for the title.
            Objective objective = created.registerNewObjective("sidebar", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName(Utils.color("&e&lSKYBLOCK"));
            return created;
        });
        Objective objective = board.getObjective("sidebar");
        Set<String> shown = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            String entry = Utils.color(lines.get(i));
            objective.getScore(entry).setScore(lines.size() - i);
            shown.add(entry);
        }
        for (String entry : board.getEntries()) {
            if (!shown.contains(entry)) board.resetScores(entry);
        }
        if (player.getScoreboard() != board) player.setScoreboard(board);
    }
}
