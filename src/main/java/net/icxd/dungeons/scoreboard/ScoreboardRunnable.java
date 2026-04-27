package net.icxd.dungeons.scoreboard;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.region.Region;
import net.icxd.dungeons.region.RegionType;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class ScoreboardRunnable implements Runnable {
    private final HashMap<UUID, Integer> coinsCache = new HashMap<>();
    private final HashMap<UUID, Integer> bitsCache = new HashMap<>();

    @Override
    public void run() {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = scoreboardManager.getNewScoreboard();

        // TODO: add animation for the title.
        String title = "&e&lSKYBLOCK";
        Objective objective = scoreboard.registerNewObjective("sidebar", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(Utils.color(title));

        Bukkit.getOnlinePlayers().forEach(player -> {
            User user = User.getUser(player.getUniqueId());

            StringBuilder coins = new StringBuilder();
            StringBuilder bits = new StringBuilder();

            coins.append("&fPurse: &6").append(Utils.getFormattedNumber(coinsCache.getOrDefault(player.getUniqueId(), user.getDocument().getInteger("coins"))));
            bits.append("&fBits: &b").append(Utils.getFormattedNumber(bitsCache.getOrDefault(player.getUniqueId(), user.getDocument().getInteger("bits"))));

            int oldCoins = coinsCache.getOrDefault(player.getUniqueId(), 0);
            int oldBits = bitsCache.getOrDefault(player.getUniqueId(), 0);
            coinsCache.put(player.getUniqueId(), user.getDocument().getInteger("coins"));
            bitsCache.put(player.getUniqueId(), user.getDocument().getInteger("bits"));

            if (oldCoins != coinsCache.get(player.getUniqueId())) {
                double difference = coinsCache.get(player.getUniqueId()) - oldCoins;
                String sign = difference > 0 ? "+" : "";
                coins.append(" &e(").append(sign).append(Utils.getFormattedNumber((int) difference)).append(")");
            }
            if (oldBits != bitsCache.get(player.getUniqueId())) {
                double difference = bitsCache.get(player.getUniqueId()) - oldBits;
                String sign = difference > 0 ? "+" : "";
                bits.append(" &3(").append(sign).append(Utils.getFormattedNumber((int) difference)).append(")");
            }

            ArrayList<String> lines = new ArrayList<>();
            lines.add("&7" + Utils.getDateFormatted(new Date()) + " &8" + Dungeons.getSkyBlockServer().getName());
            lines.add("&0");
            lines.add("&fEarly Summer 23rd");
            lines.add("&e ☀ &79:30am");
            lines.add("&7 ⏣ &7" + Region.regionCache.getOrDefault(player.getUniqueId(),
                    new Region(RegionType.getRegionType(player.getLocation()))).toString());
            lines.add("&7");
            lines.add(coins.toString());
            lines.add(bits.toString());
            lines.add("&8");
            lines.add("&ewww.hypixel.net");

            for (int i = 0; i < lines.size(); i++)
                objective.getScore(Utils.color(lines.get(i))).setScore(lines.size() - i);
            player.setScoreboard(scoreboard);
        });
    }
}
