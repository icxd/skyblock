package net.icxd.dungeons.dungeons.instance;

import java.util.concurrent.ThreadLocalRandom;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.DungeonClass;

/**
 * The Watcher's undeads (the wiki's Blood Room page). Every name can come up on any floor; their
 * perks only apply from Floor 2, and not all of them have one.
 */
enum UndeadType {
    CANNIBAL("Cannibal", DungeonClass.BERSERK, Perk.NONE),
    FROST("Frost", DungeonClass.MAGE, Perk.NONE),
    MR_DEAD("Mr. Dead", null, Perk.NONE),
    PUTRID("Putrid", DungeonClass.HEALER, Perk.NONE),
    REAPER("Reaper", DungeonClass.ARCHER, Perk.NONE),
    REVOKER("Revoker", null, Perk.NONE),
    TEAR("Tear", null, Perk.NONE),
    VADER("Vader", DungeonClass.TANK, Perk.NONE),
    FLAMER("Flamer", null, Perk.EXPLODES),
    FREAK("Freak", null, Perk.MORE_DAMAGE),
    LEECH("Leech", null, Perk.HEALS),
    MUTE("Mute", null, Perk.NONE),
    OOZE("Ooze", null, Perk.MORE_HEALTH),
    PARASITE("Parasite", null, Perk.SILVERFISH),
    PSYCHO("Psycho", null, Perk.TELEPORTS),
    SKULL("Skull", null, Perk.NONE),
    WALKER("Walker", null, Perk.NONE);

    enum Perk {
        NONE,
        /** Explodes when killed, dealing 1% of nearby players' max health. */
        EXPLODES,
        /** 1.2x damage. */
        MORE_DAMAGE,
        /** Heals 2% of its max health every second. */
        HEALS,
        /** 125% health. */
        MORE_HEALTH,
        /** 3 Parasites (silverfish) when killed. */
        SILVERFISH,
        /** A 1 in 3 chance of teleporting behind whoever hits it. */
        TELEPORTS
        // Mute ("can't hear some sounds for 5 seconds") isn't done: which sounds isn't known.
    }

    /** Per floor: base health and damage (wiki). */
    private static final double[] HEALTH = {14_000, 17_000, 32_000, 41_000, 60_000, 60_000, 75_000, 250_000};
    private static final double[] MASTER_HEALTH = {0, 800_000, 900_000, 1_000_000, 1_500_000, 2_000_000, 3_000_000, 6_000_000};
    private static final double[] DAMAGE = {1_080, 1_600, 2_600, 2_880, 3_600, 4_400, 6_400, 14_400};
    private static final double[] MASTER_DAMAGE = {0, 54_000, 63_000, 72_000, 81_000, 90_000, 99_000, 108_000};

    final String displayName;
    /** Goes for players of this class first, from Floor 2; null for any. */
    final DungeonClass targets;
    final Perk perk;

    UndeadType(String displayName, DungeonClass targets, Perk perk) {
        this.displayName = displayName;
        this.targets = targets;
        this.perk = perk;
    }

    static UndeadType random() {
        UndeadType[] all = values();
        return all[ThreadLocalRandom.current().nextInt(all.length)];
    }

    static boolean perksOn(DungeonFloor floor) {
        return floor.isMasterMode() || floor.getNumber() >= 2;
    }

    /**
     * Health for one summon. On the Entrance it's 12,000 to 20,000 in steps of 1,000, as recorded
     * (the wiki says 14k); other floors use the same spread around the wiki's number until they're
     * recorded.
     */
    double rollHealth(DungeonFloor floor) {
        double base = floor.isMasterMode() ? MASTER_HEALTH[floor.getNumber()] : HEALTH[floor.getNumber()];
        // 12/14 to 20/14 of the wiki's number, which on the Entrance is exactly 12k-20k.
        double health = Math.round(base * (12 + ThreadLocalRandom.current().nextInt(9)) / 14 / 1_000) * 1_000.0;
        if (perk == Perk.MORE_HEALTH && perksOn(floor)) health *= 1.25;
        return health;
    }

    double damage(DungeonFloor floor) {
        double damage = floor.isMasterMode() ? MASTER_DAMAGE[floor.getNumber()] : DAMAGE[floor.getNumber()];
        if (perk == Perk.MORE_DAMAGE && perksOn(floor)) damage *= 1.2;
        return damage;
    }

    /** The Parasites a Parasite leaves: health and damage (wiki). */
    static double parasiteHealth(DungeonFloor floor) {
        return floor.isMasterMode() ? 999_999 : 33_333;
    }

    static double parasiteDamage(DungeonFloor floor) {
        return floor.isMasterMode() ? 30_000 : 800;
    }

    /** The Watcher's zap when someone hits him: as hard as his undeads hit, for now. */
    static double zapDamage(DungeonFloor floor) {
        return floor.isMasterMode() ? MASTER_DAMAGE[floor.getNumber()] : DAMAGE[floor.getNumber()];
    }
}
