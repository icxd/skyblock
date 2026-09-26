package net.icxd.dungeons.stats;

import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsRunnable implements Runnable {
    public static final Map<UUID, Integer> MANA_MAP = new HashMap<>();
    public static final Map<UUID, Replacement> DEFENSE_REPLACEMENT_MAP = new HashMap<>();
    public static final Map<UUID, Replacement> MANA_REPLACEMENT_MAP = new HashMap<>();

    @Override
    public void run() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            player.setSaturation(999999999);
            player.setFoodLevel(20);

            User user = User.getUser(player.getUniqueId());
            Stats stats = new Stats().defaultStats();

            if (player.getInventory().getItemInHand() != null && player.getInventory().getItemInHand().getType() != Material.AIR)
                stats.addFromItemStack(player.getInventory().getItemInHand());
            if (player.getInventory().getHelmet() != null && player.getInventory().getHelmet().getType() != Material.AIR)
                stats.addFromItemStack(player.getInventory().getHelmet());
            if (player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() != Material.AIR)
                stats.addFromItemStack(player.getInventory().getChestplate());
            if (player.getInventory().getLeggings() != null && player.getInventory().getLeggings().getType() != Material.AIR)
                stats.addFromItemStack(player.getInventory().getLeggings());
            if (player.getInventory().getBoots() != null && player.getInventory().getBoots().getType() != Material.AIR)
                stats.addFromItemStack(player.getInventory().getBoots());

            player.setMaxHealth(stats.getHealth());

            // Health regeneration
            if (player.getHealth() <= player.getMaxHealth()) {
                player.setHealth(Math.min(player.getMaxHealth(), (player.getHealth() + 1.5 + ((int) player.getMaxHealth() * 0.01) +
                        ((1.5 + ((int) player.getMaxHealth() * 0.01)) * 0.0))));
            }

            player.setWalkSpeed(Math.min((float) (stats.getWalkSpeed() / 5.0) / 100.0f, 1.0f));
            player.setHealthScale(Math.min(40.0, 20.0 + ((stats.getHealth() - 100.0) / 25.0)));

            Replacement manaReplacement = MANA_REPLACEMENT_MAP.get(player.getUniqueId());

            int manaPool = Utils.doubleToInt(100.0 + stats.getIntelligence());
            if (!MANA_MAP.containsKey(player.getUniqueId())) MANA_MAP.put(player.getUniqueId(), manaPool);
            int mana;
            if ((mana = MANA_MAP.get(player.getUniqueId())) < manaPool) {
                MANA_MAP.remove(player.getUniqueId());
                MANA_MAP.put(player.getUniqueId(), Math.min(manaPool, Math.min(manaPool, mana + manaPool / 50 + (int) ((double) (manaPool / 50) * 0.0))));
            }

            Replacement defenseReplacement = DEFENSE_REPLACEMENT_MAP.get(player.getUniqueId());
            if (defenseReplacement != null && System.currentTimeMillis() >= defenseReplacement.getEnd()) {
                DEFENSE_REPLACEMENT_MAP.remove(player.getUniqueId());
                defenseReplacement = null;
            }
            if ((manaReplacement = MANA_REPLACEMENT_MAP.get(player.getUniqueId())) != null && System.currentTimeMillis() >= manaReplacement.getEnd()) {
                MANA_REPLACEMENT_MAP.remove(player.getUniqueId());
                manaReplacement = null;
            }

            Stats.STATS_CACHE.put(player.getUniqueId(), stats);

            Utils.sendActionText(player, "&c" + (int) player.getHealth() + "/" + (int) stats.getHealth() + "❤     &a" +
                    (defenseReplacement == null ? (stats.getDefense() != 0 ? (int) stats.getDefense() + "❈ Defense     " : "") :
                            defenseReplacement.getReplacement() + "     ") +
                    (manaReplacement == null ? ("&b" + MANA_MAP.get(player.getUniqueId()) + "/" + manaPool + "✎ Mana") :
                            manaReplacement.getReplacement()));

            /*Utils.sendActionBar(player, color + "" + Math.round(player.getHealth() + absorption)
                    + "/" + SUtil.blackMagic(statistics.getMaxHealth().addAll()) + "❤     " +
                    (replacement == null ? (defense != 0 ? "" + ChatColor.GREEN + defense + "❈ Defense     " : "") :
                            replacement.getReplacement() + "     ") +
                    (mreplacement == null ? "" + ChatColor.AQUA + MANA_MAP.get(player.getUniqueId()) + "/" + manaPool + "✎ Mana" :
                            mreplacement.getReplacement() + "     "));*/
        });
    }
}
