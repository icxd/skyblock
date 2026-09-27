package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParser;

import net.icxd.dungeons.dungeons.paste.PastePlan;
import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.MobKinds;
import net.icxd.dungeons.mob.Modifier;

/** The rooms' mob files: a made-up one, and the private data's (rooms/_mobs) when it's here. */
class RoomSpawnDataTest {
    /** The private data repository's rooms/_mobs: -Dmobs.dir, else the checkout next to this repository's. */
    static Path mobsFolder() {
        String property = System.getProperty("mobs.dir");
        if (property != null) return Path.of(property);
        Path repository = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent();
        return repository.resolveSibling("skyblock-dungeon-data/rooms/" + RoomSpawnData.FOLDER);
    }

    /** The captures (rooms/<room>/*.schem): -Drooms.dir, else the private checkout's rooms next to this repository's. */
    static Path capturesFolder() {
        String property = System.getProperty("rooms.dir");
        if (property != null) return Path.of(property);
        Path repository = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent();
        return repository.resolveSibling("skyblock-dungeon-data/rooms");
    }

    /** Whether a room has a capture there: a run can only have the rooms it has captures of. */
    private static boolean captured(Path captures, String room) {
        try (var files = Files.list(captures.resolve(room))) {
            return files.anyMatch(f -> f.getFileName().toString().endsWith(".schem"));
        } catch (IOException e) {
            return false;
        }
    }

    private static final String ROOM = """
            {"_about": "made up", "room": "hall", "recorded": true,
             "mobs": [{"kind": "ZOMBIE_GRUNT", "starred": true, "x": 12.5, "y": 69, "z": 3.5},
                      {"kind": "LOST_ADVENTURER", "starred": true, "level": 90, "x": 15.5, "y": 63, "z": 15.5},
                      {"kind": "TANK_ZOMBIE", "starred": false, "modifier": "SPEEDY", "inferred": true, "x": 1.5, "y": 80, "z": 2.5}],
             "skulls": [{"x": 4.5, "y": 69, "z": 4.5}],
             "crypts": [{"x": 7.5, "y": 69, "z": 25.5, "source": "recorded", "blocks": [[6, 69, 25], [6, 70, 25]]}],
             "weakWalls": [{"source": "recorded", "blocks": [[14, 69, 4], [15, 69, 3]],
                            "after": {"15,69,3": "minecraft:cobblestone_stairs[facing=west]"}}]}""";

    @Test
    void readsARoom() {
        RoomSpawnData.Room room = RoomSpawnData.parse("hall", JsonParser.parseString(ROOM).getAsJsonObject());
        assertTrue(room.recorded());
        assertEquals(3, room.mobs().size());
        RoomSpawnData.Mob grunt = room.mobs().get(0);
        assertEquals(new RoomSpawnData.Mob("ZOMBIE_GRUNT", true, null, null, 12.5, 69, 3.5, false), grunt);
        assertEquals(90, room.mobs().get(1).level());
        assertEquals("SPEEDY", room.mobs().get(2).modifier());
        assertTrue(room.mobs().get(2).inferred());
        assertEquals(new RoomSpawnData.Point(4.5, 69, 4.5), room.skulls().get(0));
        RoomSpawnData.Crypt crypt = room.crypts().get(0);
        assertEquals(new RoomSpawnData.Point(7.5, 69, 25.5), crypt.undead());
        assertEquals(2, crypt.blocks().size());
        RoomSpawnData.Wall wall = room.walls().get(0);
        assertEquals("minecraft:cobblestone_stairs[facing=west]", wall.after().get(new PastePlan.Block(15, 69, 3)));
        assertNull(wall.after().get(new PastePlan.Block(14, 69, 4)));
    }

    @Test
    void loadsAFolder(@TempDir Path folder) throws IOException {
        Files.writeString(folder.resolve("hall.json"), ROOM);
        Files.writeString(folder.resolve("broken.json"), "{\"mobs\": [{\"kind\": 5}]}");
        Files.writeString(folder.resolve(RoomSpawnData.HEADS), "{\"_about\": \"x\", \"Blessing\": \"abc\"}");
        RoomSpawnData.Loaded loaded = RoomSpawnData.load(folder);
        assertEquals(Set.of("hall"), loaded.rooms().keySet());
        assertEquals("abc", loaded.heads().get("Blessing"));
        assertFalse(loaded.heads().containsKey("_about"));
        assertEquals(1, loaded.problems().size(), loaded.problems().toString());
        assertTrue(loaded.problems().get(0).startsWith("broken.json"));
        assertNull(loaded.room("nothing"));
        assertNull(loaded.room(null));
        // No folder, no rooms.
        assertTrue(RoomSpawnData.load(folder.resolve("none")).rooms().isEmpty());
    }

    /** The private data, when it's here: every room reads, with kinds and modifiers the plugin has, in the capture's own area. */
    @Test
    void privateDataReads() {
        Path folder = mobsFolder();
        assumeTrue(Files.isDirectory(folder), "no " + folder);
        RoomSpawnData.Loaded loaded = RoomSpawnData.load(folder);
        assertTrue(loaded.problems().isEmpty(), loaded.problems().toString());
        assertNotNull(loaded.heads().get("Blessing"));
        Set<String> recorded = new HashSet<>();
        int crypts = 0;
        for (RoomSpawnData.Room room : loaded.rooms().values()) {
            if (room.recorded()) recorded.add(room.id());
            crypts += room.crypts().size();
            for (RoomSpawnData.Mob mob : room.mobs()) {
                MobKind kind = MobKinds.get(mob.kind());
                assertNotNull(kind, room.id() + ": " + mob.kind());
                assertNotNull(kind.variant(net.icxd.dungeons.common.DungeonFloor.ENTRANCE, mob.level()), room.id() + ": " + mob);
                if (mob.modifier() != null) assertNotNull(Modifier.parse(mob.modifier()), mob.modifier());
                // Inside a room of up to 4 cells (127 blocks).
                assertTrue(mob.x() >= 0 && mob.x() < 127 && mob.z() >= 0 && mob.z() < 127, room.id() + ": " + mob);
            }
            for (RoomSpawnData.Crypt crypt : room.crypts()) assertFalse(crypt.blocks().isEmpty(), room.id());
            for (RoomSpawnData.Wall wall : room.walls()) assertFalse(wall.blocks().isEmpty(), room.id());
        }
        // The twelve recorded rooms (research mobs.md 2), and the recorded Crypt: 43 mobs, 32 starred, its tomb 24 blocks.
        assertEquals(Set.of("altar", "catwalk", "crypt", "criss_cross", "default", "diagonal", "dragon", "long_hall", "pirate",
                "red_green", "redstone_warrior", "tomioka"), recorded);
        RoomSpawnData.Room crypt = loaded.room("crypt");
        assertEquals(43, crypt.mobs().size());
        assertEquals(32, crypt.mobs().stream().filter(RoomSpawnData.Mob::starred).count());
        assertEquals(24, crypt.crypts().get(0).blocks().size());
        assertTrue(crypts >= 4, "crypts: " + crypts);
        // Data for a room with no capture is never used (runs are built from the captures): say which.
        Path captures = capturesFolder();
        if (Files.isDirectory(captures)) {
            List<String> unused = new ArrayList<>();
            for (String room : new TreeSet<>(loaded.rooms().keySet())) {
                if (!captured(captures, room)) unused.add(room + (recorded.contains(room) ? " (recorded)" : ""));
            }
            System.out.println("rooms/_mobs data with no capture in " + captures + ", so not in any run: "
                    + (unused.isEmpty() ? "none" : String.join(", ", unused)));
        }
    }
}
