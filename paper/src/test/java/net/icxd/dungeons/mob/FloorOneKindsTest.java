package net.icxd.dungeons.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;

/** Floor I's variants: the base health all three recorded Floor I runs show, and the wiki's Floor I rows for the rest. */
class FloorOneKindsTest {
    private static MobKind.Variant f1(MobKind kind, Integer level) {
        return kind.variant(DungeonFloor.FLOOR_1, level);
    }

    @Test
    void recordedHealth() {
        Map<MobKind, Integer> recorded = Map.ofEntries(Map.entry(MobKinds.ZOMBIE_GRUNT, 24_000), Map.entry(MobKinds.SKELETON_GRUNT, 16_000),
                Map.entry(MobKinds.TANK_ZOMBIE, 1_440), Map.entry(MobKinds.CRYPT_LURKER, 12_800), Map.entry(MobKinds.SCARED_SKELETON, 17_600),
                Map.entry(MobKinds.CRYPT_SOULEATER, 20_000), Map.entry(MobKinds.CRYPT_DREADLORD, 25_600),
                Map.entry(MobKinds.UNDEAD_SKELETON, 50_000), Map.entry(MobKinds.CRYPT_UNDEAD, 45_000),
                Map.entry(MobKinds.SKELETON_SOLDIER, 22_400), Map.entry(MobKinds.SNIPER, 12_000));
        for (Map.Entry<MobKind, Integer> e : recorded.entrySet()) assertEquals(e.getValue().doubleValue(), f1(e.getKey(), null).health(), e.getKey().id());
        // Minibosses at both their Floor I levels; the Entrance's Lv80s aren't on Floor I.
        assertEquals(52_000, f1(MobKinds.LOST_ADVENTURER, 85).health());
        assertEquals(130_000, f1(MobKinds.LOST_ADVENTURER, 90).health());
        assertEquals(12_000, f1(MobKinds.ANGRY_ARCHAEOLOGIST, 90).health());
        assertEquals(21_000, f1(MobKinds.ANGRY_ARCHAEOLOGIST, 100).health());
        assertNull(f1(MobKinds.ANGRY_ARCHAEOLOGIST, 80));
    }

    /** The recorded kills' Combat XP over one Combat Wisdom (107.9, 101.7, 96.7, 174.9 at x1.2402). */
    @Test
    void recordedCombatXp() {
        assertEquals(87, f1(MobKinds.ZOMBIE_GRUNT, null).combatXp());
        assertEquals(82, f1(MobKinds.CRYPT_LURKER, null).combatXp());
        assertEquals(78, f1(MobKinds.SKELETON_GRUNT, null).combatXp());
        assertEquals(141, f1(MobKinds.UNDEAD_SKELETON, null).combatXp());
        assertEquals(94, f1(MobKinds.UNDEAD_SKELETON, null).level());
    }

    @Test
    void everyEntranceKindIsOnFloorOne() {
        for (MobKind kind : MobKinds.ENTRANCE_KINDS) assertNotNull(f1(kind, null), kind.id());
        for (MobKind kind : List.of(MobKinds.SKELETON_SOLDIER, MobKinds.SNIPER)) {
            assertNull(kind.variant(DungeonFloor.ENTRANCE, null), kind.id());
            assertEquals(kind, MobKinds.get(kind.id()));
        }
        assertEquals(2_100, f1(MobKinds.TANK_ZOMBIE, null).defense());
        assertEquals(0, MobKinds.SNIPER.speed());
    }
}
