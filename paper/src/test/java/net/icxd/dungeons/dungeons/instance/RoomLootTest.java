package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.generation.DoorType;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;

/** Checked against the nine recorded room clears (research mobs.md 1.4). */
class RoomLootTest {
    private static final Door WITHER = new Door(new Edge(new Position(0, 0), new Position(1, 0)), 0, 1, DoorType.WITHER);
    private static final Door BLOOD = new Door(new Edge(new Position(1, 0), new Position(2, 0)), 1, 2, DoorType.BLOOD);

    /**
     * Pirate (L), Crypt (1x2), Catwalk (1x3), Altar (L) and the Dragon's champion room had a blessing; Tomioka,
     * Long Hall, Red Green and Criss-Cross (all 1x1) didn't.
     */
    @Test
    void blessingsFromBigRoomsAndMinibosses() {
        assertTrue(RoomLoot.blessed(3, false));
        assertTrue(RoomLoot.blessed(2, false));
        assertTrue(RoomLoot.blessed(1, true));
        assertFalse(RoomLoot.blessed(1, false));
    }

    /** Blessing, key, then the Superboom TNT or Revive Stone, as Pirate's "Blessing of Wisdom, Wither Key, Superboom TNT". */
    @Test
    void laidOutInOrder() {
        Random random = new Random(5);
        for (int i = 0; i < 100; i++) {
            List<RoomLoot.Drop> drops = RoomLoot.roll(true, List.of(WITHER), random);
            assertEquals(3, drops.size());
            assertEquals(RoomLoot.Kind.BLESSING, drops.get(0).kind());
            assertTrue(RoomLoot.BLESSINGS.contains(drops.get(0).blessing()));
            assertEquals(RoomLoot.Kind.WITHER_KEY, drops.get(1).kind());
            assertEquals(WITHER, drops.get(1).door());
            RoomLoot.Kind last = drops.get(2).kind();
            assertTrue(last == RoomLoot.Kind.SUPERBOOM_TNT || last == RoomLoot.Kind.REVIVE_STONE);
        }
        // Criss-Cross: "Blood Key, Superboom TNT"; Tomioka: only the Superboom (or Revive Stone).
        List<RoomLoot.Drop> blood = RoomLoot.roll(false, List.of(BLOOD), random);
        assertEquals(RoomLoot.Kind.BLOOD_KEY, blood.get(0).kind());
        assertEquals(2, blood.size());
        assertEquals(1, RoomLoot.roll(false, List.of(), random).size());
    }

    @Test
    void mostlySuperboom() {
        Random random = new Random(9);
        int superbooms = 0;
        for (int i = 0; i < 9_000; i++) {
            if (RoomLoot.roll(false, List.of(), random).get(0).kind() == RoomLoot.Kind.SUPERBOOM_TNT) superbooms++;
        }
        assertEquals(6_000, superbooms, 200);
    }

    /** The names on the stands, as recorded, and half a block apart diagonally. */
    @Test
    void namesAndPlaces() {
        assertEquals("&dBlessing of Wisdom", new RoomLoot.Drop(RoomLoot.Kind.BLESSING, "Wisdom", null).name());
        assertEquals("&6&8Wither Key", new RoomLoot.Drop(RoomLoot.Kind.WITHER_KEY, null, WITHER).name());
        assertEquals("&c&cBlood Key", new RoomLoot.Drop(RoomLoot.Kind.BLOOD_KEY, null, BLOOD).name());
        assertEquals("&9Superboom TNT", new RoomLoot.Drop(RoomLoot.Kind.SUPERBOOM_TNT, null, null).name());
        assertEquals("&6Revive Stone", new RoomLoot.Drop(RoomLoot.Kind.REVIVE_STONE, null, null).name());
        assertArrayEquals(new double[]{0, 0}, RoomLoot.offset(0));
        assertArrayEquals(new double[]{1, 1}, RoomLoot.offset(2));
    }
}
