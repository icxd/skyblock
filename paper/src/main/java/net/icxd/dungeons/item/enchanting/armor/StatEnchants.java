package net.icxd.dungeons.item.enchanting.armor;

import net.icxd.dungeons.collection.CollectionData;
import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.combat.CombatState;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.reforge.ArmorReforgeBonuses;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.SkillGains;
import net.icxd.dungeons.stats.PlayerStats;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.storage.AccessoryBag;
import net.icxd.dungeons.user.User;
import org.bson.Document;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The armor and equipment enchantments whose stats depend on something else, added to a player's stats (a
 * {@link PlayerStats#addModifier} after the set bonuses', so the Stats menu and combat both see them): Respite,
 * Cayenne, The One, Quantum, Wisdom, Hecatomb's Health, Habanero Tactics' Combat Wisdom, and last Legion's share
 * more of the Combat stats (with the reforges' Renowned and Perfect, see {@link ArmorReforgeBonuses}). The plain
 * ones ("Grants +75 Health") are their items' own (ItemStats). Nothing here may ask for their stats themselves.
 * Main thread.
 */
final class StatEnchants {
    static final String RESPITE = "respite";
    static final String CAYENNE = "cayenne";
    static final String THE_ONE = "the_one";
    static final String QUANTUM = "quantum";
    static final String WISDOM = "wisdom";
    static final String LEGION = "legion";
    static final String HECATOMB = "hecatomb";
    /**
     * The Stats & Equipment menu's Combat Stats, taken as the enchantments' and reforges' "all Combat stats"
     * (UNKNOWN), as the dragon sets' Superior Blood takes them (DragonSets.COMBAT).
     */
    static final List<Stat> COMBAT = List.of(Stat.HEALTH, Stat.DEFENSE, Stat.TRUE_DEFENSE, Stat.STRENGTH, Stat.CRIT_CHANCE,
            Stat.CRIT_DAMAGE, Stat.ATTACK_SPEED, Stat.FEROCITY, Stat.SWING_RANGE, Stat.INTELLIGENCE, Stat.ABILITY_DAMAGE, Stat.HEALTH_REGEN,
            Stat.VITALITY, Stat.MENDING);
    /** Quantum's "random Wisdom stat": each skill's Wisdom, in the skills' order. */
    static final List<Stat> WISDOMS;
    /**
     * Whose week Quantum's weekdays and weekends are: UNKNOWN (the enchantment's text is all there is), taken as
     * the zone the sidebar's date is in, as the dungeons' daily runs are (RunEnd).
     */
    static final ZoneId QUANTUM_ZONE = ZoneId.of("America/New_York");
    /** How long a player's count of maxed collections (The One) is kept before it's counted again. */
    private static final long MAXED_MILLIS = 1_000;

    static {
        List<Stat> wisdoms = new ArrayList<>();
        for (Skill skill : Skill.values()) wisdoms.add(SkillGains.wisdom(skill));
        WISDOMS = List.copyOf(wisdoms);
    }

    /** Their maxed collections, and when they were counted. */
    private record Maxed(long at, int count) {
    }

    private static final Map<UUID, Maxed> MAXED = new HashMap<>();
    /** The weekend Quantum's Wisdom was last picked for (its Saturday), and which. */
    private static long pickedFor = Long.MIN_VALUE;
    private static Stat picked;

    private StatEnchants() {
    }

    static void stats(Player player, Stats stats) {
        List<WornEnchants.Piece> pieces = WornEnchants.of(player);
        if (pieces.isEmpty()) return;
        double legion = 0;
        for (WornEnchants.Piece piece : pieces) {
            if (piece.enchantments().isEmpty()) continue;
            int level;
            // "Grants +15 Health Regen while out of combat" (see CombatState).
            if ((level = piece.level(RESPITE)) > 0 && !CombatState.inCombat(player)) {
                stats.add(Stat.HEALTH_REGEN, EnchantNumbers.get(RESPITE, level, 0));
            }
            if ((level = piece.level(CAYENNE)) > 0) {
                cayenne(stats, EnchantNumbers.of(CAYENNE, level), digits(AccessoryBag.accessoryPower(player)));
            }
            if ((level = piece.level(THE_ONE)) > 0) theOne(stats, EnchantNumbers.of(THE_ONE, level), maxedCollections(player));
            if ((level = piece.level(QUANTUM)) > 0) quantum(stats, EnchantNumbers.of(QUANTUM, level), LocalDate.now(QUANTUM_ZONE));
            if ((level = piece.level(WISDOM)) > 0) {
                stats.add(Stat.INTELLIGENCE, wisdom(EnchantNumbers.of(WISDOM, level), player.getLevel()));
            }
            if ((level = piece.level(HECATOMB)) > 0) {
                stats.add(Stat.HEALTH, hecatombHealth(EnchantNumbers.of(HECATOMB, level), catacombsLevel(player)));
            }
            if ((level = piece.level(Habanero.ID)) > 0 && Habanero.holdsSlayerWeapon(player)) {
                stats.add(Stat.COMBAT_WISDOM, EnchantNumbers.get(Habanero.ID, level, 2));
            }
            if ((level = piece.level(LEGION)) > 0) legion += legion(EnchantNumbers.of(LEGION, level), player);
        }
        raise(stats, legion + ArmorReforgeBonuses.combatPercent(pieces), ArmorReforgeBonuses.defensePercent(pieces));
    }

    /**
     * Every Combat stat and Magic Find {@code combat} percent more, and Defense {@code defense} percent more on top
     * (Perfect's): the shares add up (UNKNOWN whether Hypixel's do), on the stats as everything before makes them.
     */
    static void raise(Stats stats, double combat, double defense) {
        if (combat == 0 && defense == 0) return;
        for (Stat stat : COMBAT) {
            double percent = stat == Stat.DEFENSE ? combat + defense : combat;
            if (percent != 0) stats.set(stat, stats.get(stat) * (1 + percent / 100));
        }
        if (combat != 0) stats.set(Stat.MAGIC_FIND, stats.get(Stat.MAGIC_FIND) * (1 + combat / 100));
    }

    /**
     * Cayenne: "Grants +1 Health and +0.5 True Defense per digit in your Accessory Power" (the wiki: four Cayenne V
     * are +16 Health, +8 True Defense).
     */
    static void cayenne(Stats stats, double[] n, int digits) {
        if (n.length < 2) return;
        stats.add(Stat.HEALTH, n[0] * digits);
        stats.add(Stat.TRUE_DEFENSE, n[1] * digits);
    }

    /** How many digits a number has (1,234 has 4); none for none. */
    static int digits(int power) {
        int digits = 0;
        for (int left = Math.max(0, power); left > 0; left /= 10) digits++;
        return digits;
    }

    /**
     * The One: "Grants +1 Health and +0.2 Strength per maxed out collection" (its text's count, "&k73", is the
     * book's, and shows as it is). UNKNOWN whether boss collections count: they don't (as for SkyBlock Leveling's
     * collection tiers).
     */
    static void theOne(Stats stats, double[] n, int maxed) {
        if (n.length < 2) return;
        stats.add(Stat.HEALTH, n[0] * maxed);
        stats.add(Stat.STRENGTH, n[1] * maxed);
    }

    /** Their collections at their last tier, counted at most once a second. */
    private static int maxedCollections(Player player) {
        long now = System.currentTimeMillis();
        Maxed maxed = MAXED.get(player.getUniqueId());
        if (maxed != null && now - maxed.at() < MAXED_MILLIS) return maxed.count();
        User user = User.ifLoaded(player.getUniqueId());
        int count = user == null ? 0 : maxedCollections(user.profile(), Collections.data().collections().values());
        if (player.isOnline()) MAXED.put(player.getUniqueId(), new Maxed(now, count));
        return count;
    }

    /** How many of these collections the profile has at their last tier. */
    static int maxedCollections(Document profile, Iterable<CollectionData.Collection> collections) {
        int maxed = 0;
        for (CollectionData.Collection collection : collections) {
            int tiers = collection.tiers().size();
            if (tiers > 0 && Collections.tier(collection, Collections.count(profile, collection.id())) >= tiers) maxed++;
        }
        return maxed;
    }

    /**
     * Quantum: "Grants +3 Vitality on weekdays and +2 of a random Wisdom stat on weekends"; the Wisdom is "the
     * same for everyone" (the wiki), picked for each weekend from its date (see {@link #weekendWisdom}).
     */
    static void quantum(Stats stats, double[] n, LocalDate day) {
        if (n.length < 2) return;
        if (weekend(day)) stats.add(weekendWisdom(day), n[1]);
        else stats.add(Stat.VITALITY, n[0]);
    }

    static boolean weekend(LocalDate day) {
        return day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /**
     * The Wisdom Quantum gives this weekend: one of {@link #WISDOMS} at random, the same for everyone and for both
     * its days (from its Saturday's date). UNKNOWN how Hypixel picks it.
     */
    static Stat weekendWisdom(LocalDate day) {
        long saturday = day.getDayOfWeek() == DayOfWeek.SUNDAY ? day.toEpochDay() - 1 : day.toEpochDay();
        if (saturday != pickedFor) {
            picked = WISDOMS.get(new Random(saturday).nextInt(WISDOMS.size()));
            pickedFor = saturday;
        }
        return picked;
    }

    /** Wisdom: "Gain 5 Intelligence for every 5 levels of exp you have on you. Capped at 100 Intelligence." */
    static double wisdom(double[] n, int expLevels) {
        if (n.length < 3 || n[1] <= 0) return 0;
        return Math.min(n[2], n[0] * Math.floor(Math.max(0, expLevels) / n[1]));
    }

    /** Hecatomb: "Grants +8❤ per 10 Catacombs levels" (their level for stats, at most 50). */
    static double hecatombHealth(double[] n, int catacombsLevel) {
        if (n.length < 4 || n[3] <= 0) return 0;
        return n[2] * Math.floor(Math.max(0, catacombsLevel) / n[3]);
    }

    private static int catacombsLevel(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        return user == null ? 0 : DungeonProfile.catacombsStatLevel(user);
    }

    /**
     * Legion: "Increases all Combat stats and Magic Find by 0.35% per player within 30 blocks of you, up to 20
     * players", in percent. UNKNOWN: "players ... of you" is taken as the others (alive, and not a spectator);
     * each piece's adds up.
     */
    static double legion(double[] n, Player player) {
        if (n.length < 3) return 0;
        return legion(n[0], othersNear(player, n[1]), n[2]);
    }

    /** Legion's percent: {@code perPlayer} for each of the players near, up to {@code most}. */
    static double legion(double perPlayer, int players, double most) {
        return perPlayer * Math.min(Math.max(0, players), most);
    }

    private static int othersNear(Player player, double radius) {
        int count = 0;
        for (Player other : player.getWorld().getPlayers()) {
            if (other == player || other.isDead() || other.getGameMode() == GameMode.SPECTATOR) continue;
            if (other.getLocation().distanceSquared(player.getLocation()) <= radius * radius) count++;
        }
        return count;
    }

    /** They've left. */
    static void forget(UUID player) {
        MAXED.remove(player);
    }
}
