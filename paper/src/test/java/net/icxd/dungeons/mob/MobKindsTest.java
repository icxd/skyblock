package net.icxd.dungeons.mob;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.icxd.dungeons.common.DungeonFloor;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The kind table (research mobs.md 3): every Entrance kind is there with the numbers a kill needs. */
class MobKindsTest {
    @Test
    void entranceKinds() {
        List<String> names = MobKinds.ENTRANCE_KINDS.stream().map(MobKind::name).toList();
        assertEquals(List.of("Zombie Grunt", "Skeleton Grunt", "Tank Zombie", "Crypt Lurker", "Scared Skeleton", "Crypt Souleater",
                "Crypt Dreadlord", "Undead Skeleton", "Crypt Undead", "Lost Adventurer", "Angry Archaeologist"), names);
        for (MobKind kind : MobKinds.ENTRANCE_KINDS) {
            assertTrue(kind.dungeon(), kind.id());
            assertFalse(kind.types().isEmpty(), kind.id());
            assertFalse(kind.variants(DungeonFloor.ENTRANCE).isEmpty(), kind.id());
            assertFalse(Double.isNaN(kind.speed()), kind.id());
            assertTrue(kind.magicResistance() > 0 && kind.magicResistance() < 1, kind.id());
            for (MobKind.Variant variant : kind.variants(DungeonFloor.ENTRANCE)) {
                assertTrue(variant.level() > 0, kind.id());
                assertTrue(variant.health() > 0, kind.id());
                assertTrue(variant.damage() > 0, kind.id());
                assertTrue(variant.combatXp() > 0, kind.id());
                assertEquals(1, variant.coins(), kind.id());
                for (MobDrop drop : variant.drops()) assertTrue(drop.chance() > 0 && drop.chance() <= 100, kind.id());
            }
            // Nothing on the floors that aren't filled in yet.
            assertNull(kind.variant(DungeonFloor.FLOOR_1, null), kind.id());
        }
    }

    /** Numbers that are easy to get wrong: the critic's corrections and the recorded ones. */
    @Test
    void numbers() {
        assertEquals(2_000, MobKinds.TANK_ZOMBIE.firstVariant().defense());
        assertEquals(40, MobKinds.CRYPT_UNDEAD.firstVariant().combatXp());
        assertEquals(40, MobKinds.SKELETON_GRUNT.firstVariant().combatXp());
        assertEquals(61, MobKinds.CRYPT_DREADLORD.firstVariant().combatXp());
        assertEquals(22_500, MobKinds.CRYPT_UNDEAD.firstVariant().health());
        assertEquals(List.of(80, 90), MobKinds.LOST_ADVENTURER.variants(DungeonFloor.ENTRANCE).stream().map(MobKind.Variant::level).toList());
        assertEquals(130_000, MobKinds.LOST_ADVENTURER.variant(DungeonFloor.ENTRANCE, 90).health());
        assertEquals(8_500, MobKinds.ANGRY_ARCHAEOLOGIST.firstVariant().health());
        assertEquals(12_000, MobKinds.ANGRY_ARCHAEOLOGIST.variant(DungeonFloor.ENTRANCE, 90).health());
        assertEquals(900, MobKinds.ANGRY_ARCHAEOLOGIST.firstVariant().defense());
        assertNull(MobKinds.ANGRY_ARCHAEOLOGIST.variant(DungeonFloor.ENTRANCE, 85));
        // The Watcher's undeads: the wiki's Entrance 14k and 1,080, 75 Combat XP, no coins known.
        MobKind.Variant undead = MobKinds.WATCHER_UNDEAD.variant(DungeonFloor.ENTRANCE, null);
        assertEquals(75, undead.combatXp());
        assertEquals(1_080, undead.damage());
        assertEquals(0, undead.coins());
        assertFalse(MobKinds.WATCHER_UNDEAD.roomScaled());
        assertTrue(MobKinds.WATCHER_UNDEAD.dungeon());
    }

    /** Whatever order a kind lists them in, its variants are lowest level first, and its first is the Entrance's lowest. */
    @Test
    void variantOrder() {
        MobKind kind = MobKind.builder("TEST_ORDER", "Test", EntityType.ZOMBIE)
                .variant(DungeonFloor.FLOOR_1, 50, 1, 1, 0, 1, 1)
                .variant(DungeonFloor.ENTRANCE, 90, 1, 1, 0, 1, 1)
                .variant(DungeonFloor.ENTRANCE, 80, 1, 1, 0, 1, 1)
                .build();
        assertEquals(80, kind.firstVariant().level());
        assertEquals(DungeonFloor.ENTRANCE, kind.firstVariant().floor());
        assertEquals(80, kind.variant(DungeonFloor.ENTRANCE, null).level());
        assertEquals(List.of(80, 90), kind.variants(DungeonFloor.ENTRANCE).stream().map(MobKind.Variant::level).toList());
        assertEquals(50, kind.variant(DungeonFloor.FLOOR_1, null).level());
        assertTrue(kind.dungeon());
    }

    @Test
    void hubKinds() {
        assertFalse(MobKinds.MAGMA_CUBE.dungeon());
        assertEquals(20, MobKinds.MAGMA_CUBE.variant(null, null).coins());
        assertEquals(1_000, MobKinds.BLADESOUL.variant(null, null).coins());
        // Research has no Combat XP for either.
        assertEquals(0, MobKinds.MAGMA_CUBE.firstVariant().combatXp());
        assertEquals(0, MobKinds.BLADESOUL.firstVariant().combatXp());
        assertEquals(1_000_000, MobKinds.MAGMA_CUBE.firstVariant().health());
        assertEquals(50_000_000, MobKinds.BLADESOUL.firstVariant().health());
        assertNull(MobKinds.BLADESOUL.variant(DungeonFloor.ENTRANCE, null));
        assertEquals(MobKind.NameStyle.BOSS, MobKinds.BLADESOUL.style());
        assertTrue(MobsTest.mob("BLADESOUL").isBoss());
    }

    @Test
    void ids() {
        Set<String> ids = new HashSet<>();
        for (MobKind kind : MobKinds.all().values()) assertTrue(ids.add(kind.id()), kind.id());
        assertEquals(14, ids.size());
        assertEquals(MobKinds.CRYPT_LURKER, MobKinds.get("crypt_lurker"));
        assertNull(MobKinds.get(null));
    }

    /** Player-shaped mobs are Mannequins with a recorded skin, which dungeons/textures.json has; so are heads they wear. */
    @Test
    void skins() throws Exception {
        JsonObject textures;
        try (Reader in = new InputStreamReader(MobKindsTest.class.getResourceAsStream("/dungeons/textures.json"), StandardCharsets.UTF_8)) {
            textures = JsonParser.parseReader(in).getAsJsonObject().getAsJsonObject("textures");
        }
        for (MobKind kind : MobKinds.all().values()) {
            if (kind.entityType() == EntityType.MANNEQUIN) {
                assertNotNull(kind.skin(), kind.id());
                assertTrue(textures.has(kind.skin()), kind.id() + ": " + kind.skin());
                assertTrue(textures.getAsJsonObject(kind.skin()).has("signature"), kind.id());
            } else {
                assertNull(kind.skin(), kind.id());
            }
            MobKind.Piece head = kind.gear().helmet();
            if (head != null && head.skin() != null) assertTrue(textures.has(head.skin()), kind.id() + ": " + head.skin());
        }
    }
}
