package net.icxd.dungeons.stats;

import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.dungeons.instance.DungeonRun;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.session.Absorption;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.session.Vitality;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Every second: health, speed, reach, mana and vitality regeneration and the action bar, from each player's stats. */
public class StatsRunnable implements Runnable {
    /** What else speeds up a player's mana regeneration, as shares of the base that add up (see {@link Damage#manaRegen(int, double)}). */
    private static final List<ToDoubleFunction<Player>> MANA_REGEN = new ArrayList<>();

    /** Adds something that speeds up mana regeneration (a Power Orb's "+50% base mana regen": 0.5). */
    public static void addManaRegenBonus(ToDoubleFunction<Player> bonus) {
        MANA_REGEN.add(bonus);
    }

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
            double bonus = 0;
            for (ToDoubleFunction<Player> more : MANA_REGEN) bonus += more.applyAsDouble(player);
            session.setMana(Math.min(manaPool, mana + (mana < manaPool ? Damage.manaRegen(manaPool, bonus) : 0)));
            Vitality.regenerate(player);

            sendActionBar(player);
        }
    }

    /**
     * Health (gold with absorption, see {@link #health}), Defense (or what's shown in its place for a moment,
     * like a skill's XP gain) and mana, and after it Vitality once they've used an item that spends it (see
     * {@link Vitality#shown}; where
     * Hypixel puts it among the others isn't recorded, so it's after mana), and in a dungeon room with
     * secrets how many of them the team has found. Sent every second, and at once when what's in
     * Defense's place changes (or a secret is found). Numbers are rounded down and grouped in thousands, as
     * recorded ("§c5,238/5,238❤     §a2,446§a❈ Defense     §b1,201/1,201✎ Mana", with Hypixel's own
     * symbols).
     */
    public static void sendActionBar(Player player) {
        PlayerSession session = PlayerSession.of(player);
        Stats stats = session.stats();
        Replacement defense = session.getDefenseReplacement();
        Replacement manaText = session.getManaReplacement();
        DungeonRun run = RunManager.of(player);
        Utils.sendActionText(player, health(PlayerHealth.get(player), Absorption.get(player), PlayerHealth.max(player)) + "     &a" +
                (defense == null ? (stats.has(Stat.DEFENSE) ? number(stats.get(Stat.DEFENSE)) + "❈ Defense     " : "") : defense.text() + "     ") +
                (manaText != null ? manaText.text() : "&b" + ofMax(session.getMana(), session.maxMana()) + "✎ Mana") +
                (Vitality.shown(player) ? "     " + vitality(Vitality.get(player), Vitality.max(player)) : "") +
                (run == null ? "" : run.secretsActionBar(player)));
    }

    /**
     * "&c5,238/5,238❤", or with absorption their health and it together, in gold: "§66,171/4,422❤" (the SkyHanni
     * mod's action bar patterns, ArmorStackDisplay and ItemAbilityCooldown; the Skyblocker mod reads absorption
     * as what the first number has over the max).
     */
    static String health(double health, double absorption, double max) {
        return absorption > 0 ? "&6" + ofMax(health + absorption, max) + "❤" : "&c" + ofMax(health, max) + "❤";
    }

    /**
     * "&4100/100♨": how the Skyblocker mod reads it off Hypixel's action bar, the numbers and then the
     * resource pack's Vitality glyph with no word after it ({@code VITALITY_STATUS} in its StatusBarTracker,
     * where Defense's and Mana's have theirs), ♨ standing in for that glyph; dark red, SkyHanni's colour
     * for Vitality ({@code VITALITY(DARK_RED, ...)} in its SkyblockStat).
     */
    static String vitality(double vitality, double max) {
        return "&4" + ofMax(vitality, max) + "♨";
    }

    /** "5,238/5,238". */
    static String ofMax(double value, double max) {
        return number(value) + "/" + number(max);
    }

    /** Rounded down, thousands grouped: "2,446". */
    static String number(double value) {
        return Utils.getFormattedNumber((int) Math.floor(value));
    }
}
