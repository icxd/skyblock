package net.icxd.dungeons.stats;

import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
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

            // Health regeneration: 1% of max health and 1.5 a second, times Health Regen / 100.
            double maxHealth = PlayerHealth.max(player);
            if (!player.isDead()) PlayerHealth.heal(player, Damage.healthRegen(maxHealth, stats.get(Stat.HEALTH_REGEN)));

            player.setWalkSpeed(Math.min((float) (stats.get(Stat.SPEED) / 5.0) / 100.0f, 1.0f));

            // 2% of the pool a second, rounded up, and never more than the pool (it shrinks when gear comes off).
            int manaPool = session.maxMana();
            int mana = session.getMana() < 0 ? manaPool : session.getMana();
            session.setMana(Math.min(manaPool, mana + (mana < manaPool ? Damage.manaRegen(manaPool) : 0)));

            Replacement defense = session.getDefenseReplacement();
            Replacement manaText = session.getManaReplacement();
            Utils.sendActionText(player, "&c" + (int) PlayerHealth.get(player) + "/" + (int) maxHealth + "❤     &a" +
                    (defense == null ? (stats.has(Stat.DEFENSE) ? (int) stats.get(Stat.DEFENSE) + "❈ Defense     " : "") : defense.text() + "     ") +
                    (manaText == null ? "&b" + session.getMana() + "/" + manaPool + "✎ Mana" : manaText.text()));
        }
    }
}
