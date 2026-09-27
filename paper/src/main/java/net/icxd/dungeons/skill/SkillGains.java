package net.icxd.dungeons.skill;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.StatsRunnable;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Replacement;
import net.icxd.dungeons.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Skill XP coming in: Combat XP for killing SkyBlock's mobs, and what every gain shows (the action
 * bar's "+60.2 Combat (35.46%)" for 2 seconds, the tab list's skill) and gives (each level's message,
 * coins and stats). Main thread.
 */
public final class SkillGains implements Listener {
    /** Champion's extra Combat XP by level, in percent (the wiki's table, the same as enchantments.json's lore). */
    private static final double[] CHAMPION = {3, 3.78, 4.56, 5.33, 6.11, 6.89, 7.67, 8.44, 9.22, 10};
    /** The XP part of the action bar stays about 2 seconds after the last gain (research skills.md 2.1). */
    static final long SHOWN_MILLIS = 2_000;

    /**
     * The killer gets the mob's Combat XP with their Combat Wisdom and the Champion on what they hold
     * (research skills.md 5.1). Only the killer: how a party shares it is UNKNOWN (the recordings are
     * solo). There are no Private Islands here, where mobs would give none. Kill Combo (a pet's) isn't
     * in the plugin.
     */
    @EventHandler
    public void onMobDeath(SkyBlockMobDeathEvent event) {
        Player killer = event.killer();
        if (killer == null || !killer.isOnline()) return;
        double base = event.variant().combatXp();
        if (base <= 0) return;
        double wisdom = PlayerSession.of(killer).stats().get(Stat.COMBAT_WISDOM);
        int champion = Combat.heldEnchantments(killer).getOrDefault("champion", 0);
        give(killer, Skill.COMBAT, combatXp(base, wisdom, champion));
    }

    /** A kill's Combat XP: base x (1 + Combat Wisdom / 100) x (1 + Champion's percent / 100). */
    public static double combatXp(double base, double wisdom, int champion) {
        return base * (1 + wisdom / 100) * (1 + champion(champion) / 100);
    }

    /** Champion's extra Combat XP at this level, in percent (0 without it). */
    public static double champion(int level) {
        return level < 1 ? 0 : CHAMPION[Math.min(level, CHAMPION.length) - 1];
    }

    /**
     * Gives a player skill XP on the profile they play on, with what it shows and what its levels
     * give. Null (and nothing) while their data isn't here to keep it.
     */
    public static Skills.Gain give(Player player, Skill skill, double amount) {
        User user = User.ifLoaded(player.getUniqueId());
        if (user == null || user.isReleased() || !(amount > 0)) return null;
        Skills.Gain gain = Skills.add(user.profile(), skill, amount);
        PlayerSession session = PlayerSession.of(player);
        session.setLastSkill(skill);
        Replacement shown = Replacement.forMillis(SkillText.actionBar(gain), SHOWN_MILLIS);
        session.setDefenseReplacement(shown);
        StatsRunnable.sendActionBar(player);
        // The action bar is otherwise redrawn once a second, which would keep the XP up to a second too long.
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> endShown(player, shown), SHOWN_MILLIS / 50);
        if (gain.leveledUp()) {
            for (int level = gain.oldLevel() + 1; level <= gain.newLevel(); level++) {
                for (String line : SkillText.levelUp(skill, level)) player.sendMessage(Text.line(line));
                payCoins(user, skill.coins(level));
            }
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            user.save();
        }
        return gain;
    }

    /**
     * Brings Defense back once a gain's XP has been shown, unless something newer (another gain) is
     * showing in its place. Clears it outright, as the task can run a moment before its time is up.
     */
    private static void endShown(Player player, Replacement shown) {
        if (!player.isOnline()) return;
        PlayerSession session = PlayerSession.of(player);
        Replacement current = session.getDefenseReplacement();
        if (current != null && current != shown) return;
        session.setDefenseReplacement(null);
        StatsRunnable.sendActionBar(player);
    }

    /** A level's coins, into the purse of the profile they play on: the one place skills pay coins. */
    static void payCoins(User user, int coins) {
        if (coins > 0) Purse.add(user, coins);
    }
}
