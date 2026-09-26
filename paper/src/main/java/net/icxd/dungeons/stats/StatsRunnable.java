package net.icxd.dungeons.stats;

import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/** Every second: health, speed, mana regeneration and the action bar, from each player's stats. */
public class StatsRunnable implements Runnable {
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setSaturation(999999999);
            player.setFoodLevel(20);

            PlayerSession session = PlayerSession.of(player);
            Stats stats = session.stats();

            // The server caps max health (1024 by default, spigot.yml's settings.attribute.maxHealth.max).
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            maxHealth.setBaseValue(stats.get(Stat.HEALTH));
            double max = maxHealth.getValue();
            // Health regeneration: 1.5 + 1% of max health a second.
            if (player.getHealth() <= max) {
                player.setHealth(Math.min(max, player.getHealth() + 1.5 + ((int) max * 0.01)));
            }

            player.setWalkSpeed(Math.min((float) (stats.get(Stat.SPEED) / 5.0) / 100.0f, 1.0f));
            player.setHealthScale(Math.min(40.0, 20.0 + ((stats.get(Stat.HEALTH) - 100.0) / 25.0)));

            // 2% of the pool a second, and never more than the pool (it shrinks when gear comes off).
            int manaPool = Utils.doubleToInt(100.0 + stats.get(Stat.INTELLIGENCE));
            int mana = session.getMana() < 0 ? manaPool : session.getMana();
            session.setMana(Math.min(manaPool, mana + (mana < manaPool ? manaPool / 50 : 0)));

            Replacement defense = session.getDefenseReplacement();
            Replacement manaText = session.getManaReplacement();
            Utils.sendActionText(player, "&c" + (int) player.getHealth() + "/" + (int) stats.get(Stat.HEALTH) + "❤     &a" +
                    (defense == null ? (stats.has(Stat.DEFENSE) ? (int) stats.get(Stat.DEFENSE) + "❈ Defense     " : "") : defense.text() + "     ") +
                    (manaText == null ? "&b" + session.getMana() + "/" + manaPool + "✎ Mana" : manaText.text()));
        }
    }
}
