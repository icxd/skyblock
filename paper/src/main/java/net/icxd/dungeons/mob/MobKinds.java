package net.icxd.dungeons.mob;

import net.icxd.dungeons.mob.mobs.Bladesoul;
import net.icxd.dungeons.mob.mobs.MagmaCube;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.icxd.dungeons.common.DungeonFloor.ENTRANCE;
import static net.icxd.dungeons.mob.MobKind.Gear;
import static net.icxd.dungeons.mob.MobKind.NameStyle;
import static net.icxd.dungeons.mob.MobKind.Piece;
import static net.icxd.dungeons.mob.MobType.ARCANE;
import static net.icxd.dungeons.mob.MobType.CUBIC;
import static net.icxd.dungeons.mob.MobType.HUMANOID;
import static net.icxd.dungeons.mob.MobType.INFERNAL;
import static net.icxd.dungeons.mob.MobType.SKELETAL;
import static net.icxd.dungeons.mob.MobType.SUBTERRANEAN;
import static net.icxd.dungeons.mob.MobType.TIMID;
import static net.icxd.dungeons.mob.MobType.UNDEAD;
import static net.icxd.dungeons.mob.MobType.WITHER;

/**
 * Every kind of SkyBlock mob there is (research mobs.md 3): the Entrance's, from the wiki's mob pages
 * (hypixelskyblock.minecraft.wiki, 2026 values, which the recorded health matches) and the recordings
 * (entity, gear and dye colours, movement speed attributes, Combat XP where the wiki's is wrong), and
 * the Hub's two test mobs. Coins are the wiki's base per kill; drops the wiki's, as item ids.
 *
 * <p>Magic resistance: "10% for melee mobs, 30% for ranged mobs, and 50-80% for magic mobs" (the wiki's
 * Catacombs Mobs), and the Crypt Lurker's 10%, the Souleater's and Dreadlord's 50% from their pages.
 * Not here yet: the Sniper, Cellar Spider, Lonely Spider and secret bats (listed for the Entrance by the
 * wiki, never seen in the recordings), and the Watcher's undeads (their own classes in the Blood Room).
 */
public final class MobKinds {
    // Drop tiers: the wiki's drop rows have c, u, r, l and rng; which of Hypixel's tiers each stands for is
    // UNKNOWN, so they're taken in order.
    private static final MobDropType C = MobDropType.COMMON;
    private static final MobDropType U = MobDropType.OCCASIONAL;
    private static final MobDropType R = MobDropType.RARE;
    private static final MobDropType L = MobDropType.VERY_RARE;
    private static final MobDropType RNG = MobDropType.RNGESUS_INCARNATE;
    /** "1/3M" on the wiki, as a percentage. */
    private static final double ONE_IN_3M = 100 / 3_000_000.0;

    /** "Rotten Armor Piece", "Heavy Armor Piece" and the like: one piece of the set (which one is UNKNOWN, so each is as likely). */
    private static MobDrop armorPiece(String set, double chance) {
        return MobDrop.oneOf(U, chance, set + "_HELMET", set + "_CHESTPLATE", set + "_LEGGINGS", set + "_BOOTS");
    }

    private static MobDrop drop(String item, MobDropType type, double chance) {
        return new MobDrop(item, type, chance);
    }

    private static MobDrop drop(String item, MobDropType type, double chance, int count) {
        return new MobDrop(item, type, chance, count, count);
    }

    private static final Piece BOW = Piece.of(Material.BOW);

    public static final MobKind ZOMBIE_GRUNT = MobKind.builder("ZOMBIE_GRUNT", "Zombie Grunt", EntityType.ZOMBIE)
            .types(UNDEAD).speed(0.37).magicResistance(0.1).behaviour(Behaviours::melee)
            // Rotten Armor, as recorded on 90 of 91.
            .gear(Gear.leather(null, 0x9e7003, 0x017d31, 0x017d31, 0x9e7003))
            .variant(ENTRANCE, 40, 7_000, 201, 0, 40, 1,
                    armorPiece("ROTTEN", 5), drop("ENCHANTED_ROTTEN_FLESH", U, 2), drop("PREMIUM_FLESH", L, 0.1))
            .build();

    public static final MobKind SKELETON_GRUNT = MobKind.builder("SKELETON_GRUNT", "Skeleton Grunt", EntityType.SKELETON)
            .types(SKELETAL).speed(0.25).magicResistance(0.3).behaviour(() -> Behaviours.archer(true))
            .gear(Gear.leather(BOW, 0xe1eb34, 0xe1eb34, 0xe1eb34, 0xe1eb34))
            // Combat XP 40, as recorded (the wiki says 70).
            .variant(ENTRANCE, 40, 14_000, 226, 0, 40, 1,
                    armorPiece("SKELETON_GRUNT", 5), drop("ENCHANTED_BONE", U, 2), drop("DYE_BONE", RNG, ONE_IN_3M))
            .build();

    /** The wiki: "The Lv40 Tank Zombie has 2000 Defense"; the recorded hits on them were 1/20 of those on others. */
    public static final MobKind TANK_ZOMBIE = MobKind.builder("TANK_ZOMBIE", "Tank Zombie", EntityType.ZOMBIE)
            .types(UNDEAD).speed(0.24).magicResistance(0.1).behaviour(Behaviours::tank)
            // Heavy Armor.
            .gear(Gear.leather(null, 0xffffff, 0x828282, 0x828282, 0xffffff))
            .variant(ENTRANCE, 40, 1_200, 201, 2_000, 40, 1, armorPiece("HEAVY", 5), drop("PREMIUM_FLESH", L, 0.1))
            .build();

    public static final MobKind CRYPT_LURKER = MobKind.builder("CRYPT_LURKER", "Crypt Lurker", EntityType.ZOMBIE)
            .types(UNDEAD, SUBTERRANEAN).speed(0.29).magicResistance(0.1).behaviour(Behaviours::boneThrower)
            .gear(Gear.holding(Material.BONE))
            .variant(ENTRANCE, 41, 9_000, 291, 0, 40, 1, drop("PREMIUM_FLESH", L, 0.1))
            .build();

    public static final MobKind SCARED_SKELETON = MobKind.builder("SCARED_SKELETON", "Scared Skeleton", EntityType.SKELETON)
            .types(SKELETAL, TIMID).speed(0.25).magicResistance(0.3).behaviour(Behaviours::scared)
            .gear(new Gear(BOW, null, null, null, null))
            .variant(ENTRANCE, 42, 12_000, 226, 0, 40, 1,
                    drop("ENCHANTED_BONE", U, 5), drop("MACHINE_GUN_BOW", U, 5), drop("DYE_BONE", RNG, ONE_IN_3M))
            .build();

    /** It wears the Crypt Dreadlord's skin (as recorded). */
    public static final MobKind CRYPT_SOULEATER = MobKind.builder("CRYPT_SOULEATER", "Crypt Souleater", EntityType.MANNEQUIN)
            .types(UNDEAD, WITHER, SUBTERRANEAN).skin("Crypt Dreadlord").speed(0.24).magicResistance(0.5)
            .behaviour(() -> Behaviours.avatar(true)).gear(new Gear(BOW, null, null, null, null))
            .variant(ENTRANCE, 45, 13_000, 327, 0, 45, 1, drop("CRYPT_BOW", U, 5))
            .build();

    public static final MobKind CRYPT_DREADLORD = MobKind.builder("CRYPT_DREADLORD", "Crypt Dreadlord", EntityType.MANNEQUIN)
            .types(UNDEAD, WITHER, SUBTERRANEAN).skin("Crypt Dreadlord").speed(0.26).magicResistance(0.5)
            .behaviour(() -> Behaviours.avatar(true)).gear(Gear.holding(Material.IRON_SWORD))
            .variant(ENTRANCE, 47, 14_000, 327, 0, 61, 1, drop("CRYPT_DREADLORD_SWORD", U, 5))
            .build();

    /** From skeleton skulls; its name tag has its level and max health, and it's never room-scaled (always 25,000). */
    public static final MobKind UNDEAD_SKELETON = MobKind.builder("UNDEAD_SKELETON", "Undead Skeleton", EntityType.SKELETON)
            .types(UNDEAD, SKELETAL).style(NameStyle.LEVELED).speed(0.35).magicResistance(0.3).notRoomScaled()
            .behaviour(() -> Behaviours.archer(false))
            .gear(new Gear(BOW, null, null, null, null))
            .variant(ENTRANCE, 40, 25_000, 720, 0, 36, 1, drop("ENCHANTED_BONE", U, 2), drop("DYE_BONE", RNG, ONE_IN_3M))
            .build();

    /**
     * From a blown-up crypt, never room-scaled (always 22,500). Combat XP 40 (the wiki; what the recordings
     * show fits it too); coins 1. It also drops 1 Undead Essence and 1 more half the time, which isn't an
     * item (no essences yet).
     */
    public static final MobKind CRYPT_UNDEAD = MobKind.builder("CRYPT_UNDEAD", "Crypt Undead", EntityType.MANNEQUIN)
            .types(UNDEAD, SUBTERRANEAN).skin("Crypt Undead").speed(0.35).magicResistance(0.1).notRoomScaled()
            .behaviour(() -> Behaviours.avatar(false))
            .gear(Gear.holding(Material.BONE))
            .variant(ENTRANCE, 25, 22_500, 936, 0, 40, 1, drop("REVIVE_STONE", U, 22), drop("PREMIUM_FLESH", L, 0.1))
            .build();

    /**
     * The Young one, as recorded (the wiki also has Unstable, Holy and Superior ones), at Lv80 and Lv90 (the
     * recorded one: 130k). Combat XP is the wiki's (the recordings can't tell). Dragon Essence (3, +1 70%, +1
     * 50%) isn't an item yet.
     */
    public static final MobKind LOST_ADVENTURER = MobKind.builder("LOST_ADVENTURER", "Lost Adventurer", EntityType.MANNEQUIN)
            .types(HUMANOID, ARCANE).style(NameStyle.MINIBOSS).skin("Lost Adventurer").speed(0.36).magicResistance(0.1)
            .behaviour(() -> Behaviours.avatar(false))
            .gear(new Gear(Piece.of(Material.DIAMOND_SWORD).glinted(), Piece.head("Young Dragon Helmet"),
                    Piece.dyed(Material.LEATHER_CHESTPLATE, 0xdde4f0).glinted(), Piece.dyed(Material.LEATHER_LEGGINGS, 0xdde4f0).glinted(),
                    Piece.dyed(Material.LEATHER_BOOTS, 0xdde4f0).glinted()))
            .variant(ENTRANCE, 80, 40_000, 226, 100, 100, 1, youngDrops())
            .variant(ENTRANCE, 90, 130_000, 539, 100, 110, 1, youngDrops())
            .build();

    private static MobDrop[] youngDrops() {
        return new MobDrop[] {drop("YOUNG_FRAGMENT", C, 100, 3), drop("YOUNG_FRAGMENT", C, 70), drop("YOUNG_FRAGMENT", C, 50),
                drop("BEATING_HEART", L, 0.1)};
    }

    private static final Piece[] PERFECT_IV = {Piece.of(Material.DIAMOND_HELMET).glinted(), Piece.of(Material.DIAMOND_CHESTPLATE).glinted(),
            Piece.of(Material.DIAMOND_LEGGINGS).glinted(), Piece.of(Material.DIAMOND_BOOTS).glinted()};

    /**
     * Lv80 (recorded, in Perfect Armor Tier IV with a Diamond Pickaxe), Lv90 (recorded on the Entrance though
     * the wiki has it on Floor I only, in Tier V with a Diamond Sword) and Lv100 (the wiki; unrecorded, so
     * dressed as Lv80). Combat XP is the wiki's. Diamond Essence isn't an item yet.
     */
    public static final MobKind ANGRY_ARCHAEOLOGIST = MobKind.builder("ANGRY_ARCHAEOLOGIST", "Angry Archaeologist", EntityType.MANNEQUIN)
            .types(HUMANOID, SUBTERRANEAN).style(NameStyle.MINIBOSS).skin("Angry Archaeologist").speed(0.34).magicResistance(0.1)
            .behaviour(() -> Behaviours.avatar(false))
            .gear(new Gear(Piece.of(Material.DIAMOND_PICKAXE), PERFECT_IV[0], PERFECT_IV[1], PERFECT_IV[2], PERFECT_IV[3]))
            .variant(ENTRANCE, 80, 8_500, 201, 900, 100, 1, archaeologistDrops())
            .variant(ENTRANCE, 90, 12_000, 288, 900, 105, 1,
                    new Gear(Piece.of(Material.DIAMOND_SWORD).glinted(), PERFECT_IV[0], PERFECT_IV[1], PERFECT_IV[2], PERFECT_IV[3]),
                    archaeologistDrops())
            .variant(ENTRANCE, 100, 21_000, 466, 1_000, 110, 1, archaeologistDrops())
            .build();

    private static MobDrop[] archaeologistDrops() {
        return new MobDrop[] {drop("DIAMOND_ATOM", R, 0.2), drop("BEATING_HEART", L, 0.1)};
    }

    /**
     * The Hub's test mobs keep the numbers they've had. Coins (20) and Combat XP (120) are the wiki's Lv75
     * Crimson Isle Magma Cube's.
     */
    public static final MobKind MAGMA_CUBE = MobKind.builder("MAGMA_CUBE", "Magma Cube", EntityType.MAGMA_CUBE)
            .types(CUBIC, INFERNAL).style(NameStyle.HUB).behaviour(MagmaCube::new)
            .variant(null, 75, 1_000_000, 0, 0, 120, 20, drop("DARK_CLAYMORE", RNG, 100))
            .build();

    /** Coins (1,000) and Combat XP (4,000) are the wiki's. */
    public static final MobKind BLADESOUL = MobKind.builder("BLADESOUL", "&8&lBladesoul", EntityType.SKELETON)
            .types(WITHER, SKELETAL, ARCANE).style(NameStyle.BOSS).behaviour(Bladesoul::new).gear(Gear.holding(Material.GOLDEN_AXE))
            .variant(null, 200, 50_000_000, 4_000, 0, 4_000, 1_000)
            .build();

    /** The Entrance's, in the order of research mobs.md 3. */
    public static final List<MobKind> ENTRANCE_KINDS = List.of(ZOMBIE_GRUNT, SKELETON_GRUNT, TANK_ZOMBIE, CRYPT_LURKER, SCARED_SKELETON,
            CRYPT_SOULEATER, CRYPT_DREADLORD, UNDEAD_SKELETON, CRYPT_UNDEAD, LOST_ADVENTURER, ANGRY_ARCHAEOLOGIST);

    private static final Map<String, MobKind> BY_ID = new LinkedHashMap<>();

    static {
        for (MobKind kind : ENTRANCE_KINDS) BY_ID.put(kind.id(), kind);
        BY_ID.put(MAGMA_CUBE.id(), MAGMA_CUBE);
        BY_ID.put(BLADESOUL.id(), BLADESOUL);
    }

    private MobKinds() {
    }

    /** Every kind, by id. */
    public static Map<String, MobKind> all() {
        return java.util.Collections.unmodifiableMap(BY_ID);
    }

    /** "ZOMBIE_GRUNT", "zombie_grunt": the kind; null for none. */
    public static MobKind get(String id) {
        return id == null ? null : BY_ID.get(id.toUpperCase(Locale.ROOT));
    }
}
