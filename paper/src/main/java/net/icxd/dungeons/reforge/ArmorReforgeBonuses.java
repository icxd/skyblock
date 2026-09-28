package net.icxd.dungeons.reforge;

import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.item.enchanting.armor.EnchantNumbers;
import net.icxd.dungeons.item.enchanting.armor.WornEnchants;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.Mana;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the armor and equipment reforges do past their stats (REFORGES.md's "Later"), with the numbers of their
 * bonus's text at the piece's rarity, each piece's adding up:
 * <ul>
 *   <li>Renowned: "Increases all Combat stats and Magic Find by +1%", and Perfect: "Increases Defense by +2%"
 *   (with Legion's, in the armor enchantments' stats, StatEnchants);</li>
 *   <li>Undead: "Decreases damage taken from Undead mobs by 2%", and Cubic: "... from Nether mobs by 2%";</li>
 *   <li>Ridiculous, the helmet's: "Fart when you sneak" ({@link #sneaked});</li>
 *   <li>Blood-Soaked, the cloak's: "Increase the enchantment effects of Life Steal, Vampirism and Drain by 1 level",
 *   for those enchantments to ask ({@link #enchantLevelBonus}).</li>
 * </ul>
 * Hyper's Speed after teleporting, Loving's ability damage and Empowered's Mending are the weapon enchantments
 * part's (EFFECTS.md). Main thread.
 */
public final class ArmorReforgeBonuses {
    public static final String RENOWNED = "renowned";
    public static final String PERFECT = "perfect";
    public static final String UNDEAD = "undead";
    public static final String CUBIC = "cubic";
    public static final String RIDICULOUS = "ridiculous";
    public static final String BLOOD_SOAKED = "blood_soaked";
    /** The enchantments Blood-Soaked strengthens (Drain's id is Syphon's). */
    static final Set<String> BLOOD_SOAKED_ENCHANTMENTS = Set.of("life_steal", "vampirism", "syphon");
    /**
     * Cubic's "Nether mobs": no mob type has that name. UNKNOWN: taken as the types the wiki's Mob Types says are
     * "commonly found in the Crimson Isle" (the Nether's island): Infernal, Magmatic and Arcane.
     */
    static final Set<MobType> NETHER = Set.of(MobType.INFERNAL, MobType.MAGMATIC, MobType.ARCANE);
    private static final String RIDICULOUS_KEY = "reforge:" + RIDICULOUS;
    /** The numbers in each bonus's lines, by the lines (the table's own lists, kept while it's in use). */
    private static final Map<List<String>, double[]> NUMBERS = new IdentityHashMap<>();

    private ArmorReforgeBonuses() {
    }

    /** Once, at startup. */
    public static void register() {
        PlayerDamage.addTakenFrom(ArmorReforgeBonuses::takenFrom);
    }

    /** The numbers of a piece's reforge bonus at its rarity; none without one. */
    static double[] numbers(WornEnchants.Piece piece) {
        if (piece.reforge() == null) return new double[0];
        List<String> lines = piece.reforge().bonusLines(piece.rarity());
        return NUMBERS.computeIfAbsent(lines, l -> EnchantNumbers.numbers(String.join(" ", l)));
    }

    /** The first number of each piece's bonus with this reforge, added up. */
    static double sum(List<WornEnchants.Piece> pieces, String reforge) {
        double sum = 0;
        for (WornEnchants.Piece piece : pieces) {
            if (!piece.reforged(reforge)) continue;
            double[] n = numbers(piece);
            if (n.length > 0) sum += n[0];
        }
        return sum;
    }

    /** Renowned's share more of every Combat stat and Magic Find, in percent. */
    public static double combatPercent(List<WornEnchants.Piece> pieces) {
        return sum(pieces, RENOWNED);
    }

    /** Perfect's share more Defense, in percent. */
    public static double defensePercent(List<WornEnchants.Piece> pieces) {
        return sum(pieces, PERFECT);
    }

    /** What's left of a hit by {@code by} with Undead's and Cubic's cuts (they add up, UNKNOWN), never below none. */
    private static double takenFrom(Player player, Entity by) {
        List<WornEnchants.Piece> pieces = WornEnchants.of(player);
        if (pieces.isEmpty()) return 1;
        double undead = sum(pieces, UNDEAD);
        double cubic = sum(pieces, CUBIC);
        if (undead == 0 && cubic == 0) return 1;
        Set<MobType> types = types(PlayerDamage.attacker(by));
        return taken(types.contains(MobType.UNDEAD) ? undead : 0, nether(types) ? cubic : 0);
    }

    /** The factor on a hit with these cuts, in percent. */
    static double taken(double undead, double cubic) {
        return Math.max(0, 1 - (undead + cubic) / 100);
    }

    static boolean nether(Set<MobType> types) {
        for (MobType type : types) if (NETHER.contains(type)) return true;
        return false;
    }

    /** The types of one of SkyBlock's mobs; none for anything else. */
    private static Set<MobType> types(Entity mob) {
        Mobs.Live live = Mobs.of(mob);
        if (live != null) return live.type().getTypes();
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(mob);
        return dungeonMob == null ? Set.of() : dungeonMob.types();
    }

    /**
     * Ridiculous: "Fart when you sneak. Reduces your Crit Chance by 20% for 20s but grants +30 Defense for 5s and
     * +50 mana. Requires at least 20% Crit Chance to activate." The Crit Chance is taken off as 20 of it (as the
     * "at least 20%" says), and it doesn't go again while that lasts: the wiki's Red Nose has "The special bonus
     * no longer reduces your Crit chance by 20% each use", and a fart each sneak would be 50 mana each (UNKNOWN
     * whether Hypixel has a wait: this is the least). Its sound is UNKNOWN: a low note. Not the dead's or a
     * dungeon ghost's.
     */
    public static void sneaked(Player player) {
        if (player.isDead() || player.isInvulnerable()) return;
        WornEnchants.Piece helmet = null;
        for (WornEnchants.Piece piece : WornEnchants.of(player)) {
            if (piece.reforged(RIDICULOUS)) {
                helmet = piece;
                break;
            }
        }
        if (helmet == null) return;
        double[] n = numbers(helmet);
        if (n.length < 6) return;
        PlayerSession session = PlayerSession.of(player);
        if (session.cooldownLeft(RIDICULOUS_KEY) > 0 || session.stats().get(Stat.CRIT_CHANCE) < n[5]) return;
        session.startCooldown(RIDICULOUS_KEY, (long) (n[1] * 1000));
        session.buff(RIDICULOUS_KEY + ":crit", new Stats().set(Stat.CRIT_CHANCE, -n[0]), (long) (n[1] * 1000));
        session.buff(RIDICULOUS_KEY + ":defense", new Stats().set(Stat.DEFENSE, n[2]), (long) (n[3] * 1000));
        session.setMana(Math.min(session.maxMana(), Mana.get(player) + (int) n[4]));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);
    }

    /**
     * Blood-Soaked's extra levels of this enchantment (by its id: Life Steal, Vampirism, Drain's "syphon") on their
     * weapon: 1 for each piece with it they wear (a cloak), 0 for any other enchantment. The weapon enchantments
     * add it to their level.
     */
    public static int enchantLevelBonus(Player player, String enchantment) {
        if (!BLOOD_SOAKED_ENCHANTMENTS.contains(enchantment)) return 0;
        int levels = 0;
        for (WornEnchants.Piece piece : WornEnchants.of(player)) {
            if (!piece.reforged(BLOOD_SOAKED)) continue;
            double[] n = numbers(piece);
            levels += n.length > 0 ? (int) n[0] : 0;
        }
        return levels;
    }
}
