package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static net.icxd.dungeons.item.bonus.TestPieces.fullSet;
import static net.icxd.dungeons.item.bonus.TestPieces.item;
import static net.icxd.dungeons.item.bonus.TestPieces.worn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Catacombs' sets in numbers: Dungeon Lord, Adaptive, which pieces count, Vindicate, the Wither pieces, the pieces'
 * own text, kills per run.
 */
class DungeonSetsTest {
    private static final double EPSILON = 1e-9;

    /** Dungeon Lord: per whole minute, +5 Strength and +10 Crit Damage (Skeleton Lord), +10 and +10 Defense (Zombie Lord). */
    @Test
    void dungeonLord() {
        List<Worn.Piece> skeleton = pieces("SKELETON_LORD", fullSet("Dungeon Lord", 4));
        Stats three = DungeonSets.DungeonLord.perMinute(skeleton, 4, 3);
        assertEquals(15, three.get(Stat.STRENGTH), EPSILON);
        assertEquals(30, three.get(Stat.CRIT_DAMAGE), EPSILON);
        assertEquals(0, three.get(Stat.DEFENSE), EPSILON);
        Stats zombie = DungeonSets.DungeonLord.perMinute(pieces("ZOMBIE_LORD", fullSet("Dungeon Lord", 4)), 4, 2);
        assertEquals(20, zombie.get(Stat.STRENGTH), EPSILON);
        assertEquals(20, zombie.get(Stat.DEFENSE), EPSILON);
        assertEquals(0, DungeonSets.DungeonLord.minutes(59_999));
        assertEquals(1, DungeonSets.DungeonLord.minutes(60_000));
    }

    /** Efficient training: +2% for every 5 Catacombs levels; Adaptive's class bonuses, a piece's and the belt's. */
    @Test
    void adaptive() {
        assertEquals(0, DungeonSets.EfficientTraining.share(4), EPSILON);
        assertEquals(0.02, DungeonSets.EfficientTraining.share(5), EPSILON);
        assertEquals(0.2, DungeonSets.EfficientTraining.share(50), EPSILON);
        assertEquals(new Stats().set(Stat.STRENGTH, 20), DungeonSets.AdaptiveClasses.bonus(DungeonClass.BERSERK, false));
        assertEquals(new Stats().set(Stat.MENDING, 5).set(Stat.HEALTH, 40), DungeonSets.AdaptiveClasses.bonus(DungeonClass.HEALER, false));
        assertEquals(new Stats().set(Stat.CRIT_CHANCE, 2).set(Stat.CRIT_DAMAGE, 5), DungeonSets.AdaptiveClasses.bonus(DungeonClass.ARCHER, true));
        assertEquals(new Stats().set(Stat.HEALTH, 5).set(Stat.DEFENSE, 10), DungeonSets.AdaptiveClasses.bonus(DungeonClass.TANK, true));
        assertTrue(new DungeonSets.AdaptiveClasses().item("STARRED_ADAPTIVE_HELMET"));
        assertFalse(new DungeonSets.AdaptiveClasses().item("ADAPTIVE_BLADE"));
    }

    /** Which items' own text counts: whole armor pieces of these sets only. */
    @Test
    void piecesOfSets() {
        assertTrue(DungeonSets.armorOf("SKELETON_MASTER_CHESTPLATE", "SKELETON_MASTER"));
        assertTrue(DungeonSets.armorOf("POWER_WITHER_BOOTS", "WITHER", "POWER_WITHER"));
        assertFalse(DungeonSets.armorOf("POWER_WITHER_BOOTS", "WITHER"));
        assertFalse(DungeonSets.armorOf("WITHER_GOGGLES", "WITHER"));
        assertFalse(DungeonSets.armorOf("ROTTEN_FLESH", "ROTTEN"));
        assertTrue(new DungeonSets.WitherPieces().item("WITHER_HELMET"));
        assertTrue(new DungeonSets.ArrowPieces().item("SPEED_WITHER_LEGGINGS"));
        assertFalse(new DungeonSets.ArrowPieces().item("TANK_WITHER_LEGGINGS"));
    }

    /** Vindicate's whole fifties; the Wither pieces' 10% each off withers' hits, a Crypt Witherlord piece's 5%. */
    @Test
    void vindicateAndWither() {
        assertEquals(0, DungeonSets.Vindicate.speed(49), EPSILON);
        assertEquals(12, DungeonSets.Vindicate.speed(612), EPSILON);
        List<Worn.Piece> wither = pieces("WITHER", fullSet("Witherborn", 4));
        assertEquals(0.6, DungeonSets.WitherPieces.factor(wither), EPSILON);
        assertEquals(0.9, DungeonSets.WitherPieces.factor(wither.subList(0, 1)), EPSILON);
        assertEquals(0.85, DungeonSets.WitherPieces.factor(List.of(wither.get(0), worn(item("CRYPT_WITHERLORD_BOOTS")))), EPSILON);
        assertTrue(new DungeonSets.WitherPieces().item("CRYPT_WITHERLORD_HELMET"));
    }

    /** The Mender helmets' stats in a run, the Stone to Steel Chestplates' share and reach, the Sniper Helmet's steps. */
    @Test
    void piecesOwnText() {
        assertEquals(new Stats().set(Stat.MENDING, 50), DungeonSets.MenderPieces.of("MENDER_HELMET"));
        assertEquals(new Stats().set(Stat.MENDING, 80).set(Stat.VITALITY, 80), DungeonSets.MenderPieces.of("MENDER_CROWN"));
        assertTrue(new DungeonSets.MenderPieces().item("MENDER_FEDORA"));
        assertFalse(new DungeonSets.MenderPieces().item("MENDER_SWORD"));
        assertEquals(0.08, DungeonSets.GuardChestplates.share("METAL_CHESTPLATE"), EPSILON);
        assertFalse(new DungeonSets.GuardChestplates().item("IRON_CHESTPLATE"));
        assertEquals(30, DungeonSets.GuardChestplates.range(DungeonClass.TANK), EPSILON);
        assertEquals(10, DungeonSets.GuardChestplates.range(DungeonClass.HEALER), EPSILON);
        assertEquals(0, DungeonSets.SniperHelmet.extra(21.9), EPSILON);
        assertEquals(1, DungeonSets.SniperHelmet.extra(22), EPSILON);
        assertEquals(15, DungeonSets.SniperHelmet.extra(51), EPSILON);
        Bonus sniper = new DungeonSets.SniperHelmet();
        Bonus.Active worn = new Bonus.Active(sniper, null, 1, List.of());
        Damage.Target target = new Damage.Target(100, 100, 0, 0, Set.of(), 0);
        assertEquals(new Combat.HitBuff(5, 1), sniper.hit(null, worn, arrow(30), target));
        assertNull(sniper.hit(null, worn, arrow(10), target));
    }

    /** An arrow's hit that flew this far. */
    private static Damage.Attacker arrow(double travelled) {
        return new Damage.Attacker(100, 0, 0, 0, 0, 100, null, true, travelled, 1);
    }

    /** The skeletons' arrows: +5 additive a piece, +25 for a whole Skeleton Master set; nothing for melee. */
    @Test
    void arrows() {
        Damage.Target target = new Damage.Target(100, 100, 0, 0, Set.of(), 0);
        Bonus pieces = new DungeonSets.ArrowPieces();
        Bonus.Active three = new Bonus.Active(pieces, null, 3, List.of());
        assertEquals(new Combat.HitBuff(15, 1), pieces.hit(null, three, true, target));
        assertNull(pieces.hit(null, three, false, target));
        Bonus master = new DungeonSets.ArrowSet("Skeleton Master");
        assertEquals(new Combat.HitBuff(25, 1), master.hit(null, new Bonus.Active(master, null, 4, List.of()), true, target));
    }

    /** Strength per kill: the run's count, none in another run. */
    @Test
    void killsPerRun() {
        RunCounter counter = new RunCounter();
        Object run = new Object();
        counter.add(run);
        counter.add(run);
        assertEquals(2, counter.get(run));
        assertEquals(0, counter.get(new Object()));
        assertEquals(0, counter.get(null));
        Object next = new Object();
        assertEquals(1, counter.add(next));
        assertEquals(0, counter.get(run));
    }

    /** Four made-up pieces of a set, SET_HELMET to SET_BOOTS, with this block. */
    private static List<Worn.Piece> pieces(String set, String block) {
        return List.of(worn(item(set + "_HELMET", block)), worn(item(set + "_CHESTPLATE", block)), worn(item(set + "_LEGGINGS", block)),
                worn(item(set + "_BOOTS", block)));
    }
}
