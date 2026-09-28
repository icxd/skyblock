package net.icxd.dungeons.item.ability.utility;

import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.session.Absorption;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;

/**
 * Helmets that save their wearer from a hit that would kill them. The Spirit Mask's Second Wind: "Instead of
 * dying, gain +50✦ Speed and damage immunity for 3 seconds", every 30 seconds; the fragged one "Also heals you
 * for 10% of your ❤ Health over 5 seconds" (a heal over time, the first second's a second later: UNKNOWN), with
 * the wiki's "&6Second Wind Activated&a! Your Spirit Mask saved your life!". Bonzo's Mask's Clownin' Around,
 * only in the Catacombs: "Instead of dying, gain damage immunity and +20 ❁ Strength for 3s and fully replenish
 * your health", its 360 second cooldown 3.6 seconds shorter for each Catacombs level (180 at 50: the wiki); how
 * its Strength grows with the level is UNKNOWN (it stays the text's), and so are its message's colours ("Your
 * Bonzo's Mask saved your life!", what mods read, with the mask's name: "⚚ Bonzo's Mask" when fragged). The two
 * have their own cooldowns. The hit that would have killed them does nothing, and nor does anything else for
 * the immunity's seconds (see {@link Protection}).
 */
final class LastStand {
    static final String SECOND_WIND = "Second Wind";
    static final String CLOWNIN_AROUND = "Clownin' Around";
    /** Bonzo's Mask: "reduced by 3.6s for each Catacombs level". */
    private static final double BONZO_PER_LEVEL = 3.6;
    private static final long IMMUNITY_MILLIS = 3_000;

    private LastStand() {
    }

    /** A shield: what's left of a hit once their helmet has saved them from it (nothing), or all of it (their absorption takes a hit first). */
    static double left(Player player, double taken, Entity by) {
        if (taken < PlayerHealth.get(player) + Absorption.get(player)) return taken;
        return saved(player) ? 0 : taken;
    }

    /** Whether their helmet saves them from dying now (and it does what it does). */
    static boolean saved(Player player) {
        Worn helmet = Worn.of(player.getInventory().getHelmet());
        if (helmet == null) return false;
        ItemBlock secondWind = helmet.ability(SECOND_WIND);
        if (secondWind != null) return secondWind(player, secondWind);
        ItemBlock clownin = helmet.ability(CLOWNIN_AROUND);
        return clownin != null && clowninAround(player, helmet, clownin);
    }

    private static boolean secondWind(Player player, ItemBlock block) {
        if (!ready(player, block.name(), (long) (block.cooldown() * 1000))) return false;
        String plain = AbilityText.plain(block.text());
        long millis = (long) AbilityText.millis(plain).orElse(IMMUNITY_MILLIS);
        Protection.immunity(player, block.name(), millis);
        Buffs.give(player, block.name(), AbilityText.stats(plain), millis);
        double share = healShare(plain);
        if (share > 0) {
            int seconds = healSeconds(plain);
            Heals.overTime(player, player, block.name(), share * PlayerHealth.max(player) / seconds, seconds, false);
        }
        player.sendMessage(Utils.color("&6Second Wind Activated&a! Your Spirit Mask saved your life!"));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.6f);
        return true;
    }

    private static boolean clowninAround(Player player, Worn helmet, ItemBlock block) {
        if (!RunManager.inRun(player)) return false;
        User user = User.ifLoaded(player.getUniqueId());
        int level = user == null ? 0 : DungeonProfile.catacombsStatLevel(user);
        if (!ready(player, block.name(), bonzoCooldownMillis(block.cooldown(), level))) return false;
        String plain = AbilityText.plain(block.text());
        long millis = (long) AbilityText.millis(plain).orElse(IMMUNITY_MILLIS);
        Protection.immunity(player, block.name(), millis);
        Buffs.give(player, block.name(), AbilityText.stats(plain), millis);
        PlayerHealth.set(player, PlayerHealth.max(player));
        player.sendMessage(Utils.color("&aYour " + helmet.item().name() + " saved your life!"));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1, 1.2f);
        return true;
    }

    /** Whether the helmet's ability is off its cooldown; if so, its cooldown starts. */
    private static boolean ready(Player player, String ability, long cooldownMillis) {
        PlayerSession session = PlayerSession.of(player);
        String key = "ability:" + ability;
        if (session.cooldownLeft(key) > 0) return false;
        session.startCooldown(key, cooldownMillis);
        return true;
    }

    /** Clownin' Around's cooldown: 360 seconds, 3.6 fewer a Catacombs level (180 at 50). */
    static long bonzoCooldownMillis(double baseSeconds, int catacombsLevel) {
        return (long) (Math.max(0, baseSeconds - BONZO_PER_LEVEL * Math.clamp(catacombsLevel, 0, 50)) * 1000);
    }

    /** "heals you for 10% of your ❤ Health over 5 seconds": 0.1; 0 if it doesn't heal. */
    static double healShare(String plain) {
        return AbilityText.after(plain, "heals you for").orElse(0) / 100;
    }

    /** Over how many seconds: "over 5 seconds"; 1 if it doesn't say. */
    static int healSeconds(String plain) {
        return (int) Math.max(1, AbilityText.after(plain, "over").orElse(1));
    }
}
