package net.icxd.dungeons.item.ability.utility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.session.Vitality;

/**
 * What abilities do to the hits players take and deal, each for a while, by what gave it: damage
 * immunity ("Grants 1 second of immunity after teleporting", "damage immunity for 3 seconds"), not being
 * able to attack ("You cannot attack ... while the veil is up", "can't deal damage"), their hits doing less
 * ("Halves your damage for 2s afterwards") and the mobs an Enrage taunted hitting them for less. Immunity
 * takes whole SkyBlock hits (see {@link #register}) and vanilla damage ({@link UtilityListener}); the veil
 * and the saves from death come after it on a hit's way. Main thread.
 */
final class Protection {
    /** Until when, by player and then by what gave it. */
    private static final Map<UUID, Map<String, Long>> IMMUNE = new HashMap<>();
    private static final Map<UUID, Map<String, Long>> NO_ATTACK = new HashMap<>();
    /** What their hits are multiplied by until when, by player and then by what gave it. */
    private static final Map<UUID, Map<String, double[]>> DEALT = new HashMap<>();
    /** The mobs taunted against them: until when, and the factor on those mobs' hits. */
    private static final Map<UUID, Map<UUID, double[]>> TAUNTED = new HashMap<>();

    private Protection() {
    }

    /** On every hit's way to a player's health: immunity, then taunted mobs' less, then the veil, then the saves from death. */
    static void register() {
        PlayerDamage.addShield((player, taken, by) -> immune(player) ? 0 : taken * tauntedFactor(player, by));
        PlayerDamage.addShield(CreeperVeil::absorb);
        PlayerDamage.addShield(LastStand::left);
        Combat.addMultiplier((player, ranged) -> dealtFactor(player.getUniqueId(), System.currentTimeMillis()));
        Vitality.addRegenPause(CreeperVeil::isUp);
    }

    static void immunity(Player player, String source, long millis) {
        put(IMMUNE, player.getUniqueId(), source, System.currentTimeMillis() + millis);
    }

    static void endImmunity(Player player, String source) {
        Map<String, Long> sources = IMMUNE.get(player.getUniqueId());
        if (sources != null) sources.remove(source);
    }

    static boolean immune(Player player) {
        return active(IMMUNE.get(player.getUniqueId()), System.currentTimeMillis());
    }

    static void noAttack(Player player, String source, long millis) {
        put(NO_ATTACK, player.getUniqueId(), source, System.currentTimeMillis() + millis);
    }

    static void endNoAttack(Player player, String source) {
        Map<String, Long> sources = NO_ATTACK.get(player.getUniqueId());
        if (sources != null) sources.remove(source);
    }

    static boolean cantAttack(Player player) {
        return active(NO_ATTACK.get(player.getUniqueId()), System.currentTimeMillis());
    }

    /** Their hits are times {@code factor} for {@code millis} from {@code fromMillis} on (a delay: "afterwards"). */
    static void dealt(Player player, String source, double factor, long fromMillis, long millis) {
        dealt(player.getUniqueId(), source, factor, fromMillis, millis);
    }

    static void dealt(UUID player, String source, double factor, long fromMillis, long millis) {
        DEALT.computeIfAbsent(player, id -> new HashMap<>()).put(source, new double[] {factor, fromMillis, fromMillis + millis});
    }

    /** Those mobs' hits on them are times {@code factor} for {@code millis}. */
    static void taunted(Player player, Entity mob, double factor, long millis) {
        TAUNTED.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>())
                .put(mob.getUniqueId(), new double[] {factor, System.currentTimeMillis() + millis});
    }

    /** Whether any of these runs past {@code now}. */
    static boolean active(Map<String, Long> until, long now) {
        if (until == null) return false;
        until.values().removeIf(end -> end <= now);
        return !until.isEmpty();
    }

    /** The product of the factors on their hits that are on at {@code now}. */
    static double dealtFactor(UUID player, long now) {
        Map<String, double[]> factors = DEALT.get(player);
        if (factors == null) return 1;
        factors.values().removeIf(f -> f[2] <= now);
        double product = 1;
        for (double[] f : factors.values()) if (f[1] <= now) product *= f[0];
        return product;
    }

    /** The factor on a hit from {@code by} (or what shot it): a taunted mob's, else 1. */
    private static double tauntedFactor(Player player, Entity by) {
        Map<UUID, double[]> mobs = TAUNTED.get(player.getUniqueId());
        if (mobs == null || by == null) return 1;
        if (by instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) by = shooter;
        long now = System.currentTimeMillis();
        mobs.values().removeIf(f -> f[1] <= now);
        double[] factor = mobs.get(by.getUniqueId());
        return factor == null ? 1 : factor[0];
    }

    /** They've left: nothing of theirs stays. */
    static void forget(UUID player) {
        IMMUNE.remove(player);
        NO_ATTACK.remove(player);
        DEALT.remove(player);
        TAUNTED.remove(player);
    }

    private static void put(Map<UUID, Map<String, Long>> map, UUID player, String source, long until) {
        map.computeIfAbsent(player, id -> new HashMap<>()).put(source, until);
    }
}
