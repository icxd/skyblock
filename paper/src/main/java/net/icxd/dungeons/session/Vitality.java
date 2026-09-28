package net.icxd.dungeons.session;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Utils;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A player's Vitality: since the Healing Revamp (0.26.1) a pool that healing abilities spend, as other
 * abilities spend mana. "Your Vitality stat defines the maximum amount of Vitality (the resource) you
 * have. Activating an ability that costs Vitality depletes the resource", and it "no longer boosts healing
 * as a multiplier" (0.26.1's release notes, and its June 10 alpha; the wiki's Vitality). It's kept on their session with
 * its fractions (the stats menu: "You will regenerate 5.2 Vitality per second" at 104). Main thread.
 */
public final class Vitality {
    /** "Vitality regenerates at a rate of 5% per second, based on your Vitality stat." */
    static final double REGEN_SHARE = 0.05;
    /** The profile's flag: they've used an item that costs Vitality, so the action bar shows it (see {@link #shown}). */
    static final String SHOWN = "vitalityShown";
    /** The bold green rule around it, as around Hypixel's other announcements (64 of them in its picture too). */
    private static final String RULE = "&a&l" + "▬".repeat(64);
    /**
     * "When you first use an ability that costs Vitality, a stat message unlock will appear, explaining how
     * Vitality works" (0.26.1's release notes), as their picture of it reads. Its colours there are the
     * SkyBlock resource pack's own, so these are the nearest codes: gold, gray, and Vitality's dark red as
     * in lore and the stats menu.
     */
    static final List<String> DISCOVERED = List.of(RULE, "&6&lNEW STAT DISCOVERED! &4♨ Vitality", "",
            "&4♨ Vitality &7is a resource for healing abilities. The more &4♨ Vitality &7you have, the more healing you will have available.",
            "", RULE);
    /** What stops Vitality regenerating (see {@link #addRegenPause}). */
    private static final List<Predicate<Player>> PAUSES = new ArrayList<>();
    private static final List<Spent> SPENT = new ArrayList<>();

    /** Something that happens when a player spends Vitality (see {@link #addSpentListener}). */
    @FunctionalInterface
    public interface Spent {
        void spent(Player player, double amount);
    }

    private Vitality() {
    }

    /**
     * Adds something that happens whenever a player spends Vitality (the "... Vitality" enchantments: "4% of
     * Vitality used becomes Defense"). Every Vitality cost goes through {@link #spend}.
     */
    public static void addSpentListener(Spent listener) {
        SPENT.add(listener);
    }

    /** The pool's size: the Vitality stat. */
    public static double max(Player player) {
        return Math.max(0, PlayerSession.of(player).stats().get(Stat.VITALITY));
    }

    /** What's in it now: full until they've spent some, never more than the pool (which shrinks when gear comes off). */
    public static double get(Player player) {
        double vitality = PlayerSession.of(player).getVitality();
        double max = max(player);
        return vitality < 0 ? max : Math.min(vitality, max);
    }

    /** Whether they have this much to spend. */
    public static boolean has(Player player, double cost) {
        return cost <= 0 || get(player) >= cost;
    }

    /** Takes this much if they have it; returns whether they did (nothing is taken if not). */
    public static boolean spend(Player player, double cost) {
        if (cost <= 0) return true;
        double vitality = get(player);
        if (vitality < cost) return false;
        PlayerSession.of(player).setVitality(vitality - cost);
        User user = User.ifLoaded(player.getUniqueId());
        if (user != null && markShown(user.profile())) {
            // Saved now, as other one-time profile changes are, so a crash before the autosave can't show it twice.
            user.save();
            for (String line : DISCOVERED) player.sendMessage(Utils.color(line));
        }
        for (Spent listener : SPENT) listener.spent(player, cost);
        return true;
    }

    /** Stops a player's Vitality regenerating while it says so ("You cannot ... regenerate Vitality while the veil is up"). */
    public static void addRegenPause(Predicate<Player> pause) {
        PAUSES.add(pause);
    }

    /** A second's regeneration: 5% of the pool, never past it; none while something pauses it. */
    public static void regenerate(Player player) {
        for (Predicate<Player> pause : PAUSES) if (pause.test(player)) return;
        double max = max(player);
        PlayerSession.of(player).setVitality(Math.min(max, get(player) + regenPerSecond(max)));
    }

    /** 5% of the pool a second: +5 at 100, +6 at 120, +10 at 200. */
    public static double regenPerSecond(double max) {
        return Math.max(0, max) * REGEN_SHARE;
    }

    /**
     * Whether the action bar shows their Vitality: "Vitality is now permanently displayed after you first
     * use an item that costs Vitality" (0.26.1's release candidate, July 15, in place of the June 10 alpha's
     * "replaces Mana in your action bar when holding an item that consumes Vitality"; the wiki's Vitality:
     * "After using any healing ability for the first time, a Vitality display appears on the Player's action
     * bar"). "Permanently" is taken as kept on their profile.
     */
    public static boolean shown(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        return user != null && shown(user.profile());
    }

    static boolean shown(Document profile) {
        return profile != null && Boolean.TRUE.equals(profile.getBoolean(SHOWN));
    }

    /** From now on their action bar shows Vitality (they've spent some); whether that's new. */
    static boolean markShown(Document profile) {
        if (profile == null || shown(profile)) return false;
        profile.put(SHOWN, true);
        return true;
    }
}
