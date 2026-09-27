package net.icxd.dungeons.stats;

import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.session.Vitality;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Every second: health, speed, reach, mana and vitality regeneration and the action bar, from each player's stats. */
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

            PlayerAttributes.apply(player, stats);

            // 2% of the pool a second, rounded up, and never more than the pool (it shrinks when gear comes off).
            int manaPool = session.maxMana();
            int mana = session.getMana() < 0 ? manaPool : session.getMana();
            session.setMana(Math.min(manaPool, mana + (mana < manaPool ? Damage.manaRegen(manaPool) : 0)));
            Vitality.regenerate(player);

            sendActionBar(player);
        }
    }

    /**
     * Health, Defense (or what's shown in its place for a moment, like a skill's XP gain) and mana, or
     * Vitality while they hold an item that spends it (see {@link Vitality#shown}: how Hypixel's reads
     * isn't recorded, so it's mana's, in Vitality's colour and symbol). Sent every second, and at once when
     * what's in Defense's place changes.
     */
    public static void sendActionBar(Player player) {
        PlayerSession session = PlayerSession.of(player);
        Stats stats = session.stats();
        Replacement defense = session.getDefenseReplacement();
        Replacement manaText = session.getManaReplacement();
        Utils.sendActionText(player, "&c" + (int) PlayerHealth.get(player) + "/" + (int) PlayerHealth.max(player) + "❤     &a" +
                (defense == null ? (stats.has(Stat.DEFENSE) ? (int) stats.get(Stat.DEFENSE) + "❈ Defense     " : "") : defense.text() + "     ") +
                (manaText != null ? manaText.text()
                        : Vitality.shown(player) ? "&4" + (int) Vitality.get(player) + "/" + (int) Vitality.max(player) + "♨ Vitality"
                        : "&b" + session.getMana() + "/" + session.maxMana() + "✎ Mana"));
    }
}
