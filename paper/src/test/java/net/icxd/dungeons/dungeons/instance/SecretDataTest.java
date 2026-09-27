package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.paste.PastePlan;

class SecretDataTest {
    @TempDir
    Path root;

    @Test
    void readsTheRoomsFolder() throws IOException {
        Path folder = Files.createDirectories(root.resolve("rooms").resolve(SecretData.FOLDER));
        Files.writeString(folder.resolve("tic_tac_toe.json"), """
                {"id": "tic_tac_toe", "name": "Tic Tac Toe", "secrets": 1, "waypoints": [
                  {"name": "1 - Lever", "category": "lever", "x": 13, "y": 73, "z": 25, "wall": [[8, 70, 22], [9, 70, 22]]},
                  {"name": "1 - Stonk", "category": "stonk", "x": 9, "y": 71, "z": 22},
                  {"name": "1 - Chest", "category": "chest", "x": 9, "y": 69, "z": 25, "start": true, "facing": "NORTH"}]}
                """, StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("broken.json"), "{", StandardCharsets.UTF_8);
        SecretData data = SecretData.load(root);
        assertEquals(1, data.size());
        assertEquals(1, data.problems().size());
        SecretData.Room room = data.room("tic_tac_toe");
        assertEquals(1, room.secrets());
        SecretData.Waypoint lever = room.waypoints().get(0);
        assertEquals(SecretData.Kind.LEVER, lever.kind());
        assertEquals(List.of(9, 70, 22), List.of(lever.wall().get(1)[0], lever.wall().get(1)[1], lever.wall().get(1)[2]));
        assertEquals(SecretData.Kind.OTHER, room.waypoints().get(1).kind());
        SecretData.Waypoint chest = room.waypoints().get(2);
        assertTrue(chest.start());
        assertEquals(Direction.NORTH, chest.facing());
        assertTrue(chest.kind().secret());
        assertFalse(lever.kind().secret());
        assertNull(data.room("crypt"));
    }

    @Test
    void noFolderNoSecrets() {
        SecretData data = SecretData.load(root);
        assertEquals(0, data.size());
        assertEquals(1, data.problems().size());
    }

    /** A Redstone Key's head is only the way to the secret, which is putting it on the node (the wiki). */
    @Test
    void redstoneKeys() {
        assertEquals(SecretData.Kind.KEY, SecretData.Kind.of("key", "1 - Redstone Skull (right click)"));
        assertEquals(SecretData.Kind.NODE, SecretData.Kind.of("key", "1 - Place Skull"));
        assertFalse(SecretData.Kind.KEY.secret());
        assertTrue(SecretData.Kind.NODE.secret());
        for (String category : List.of("superboom", "stonk", "entrance", "aotv", "pearl", "prince", "fairysoul", "default")) {
            assertEquals(SecretData.Kind.OTHER, SecretData.Kind.of(category, "1 - Something"), category);
        }
    }

    /**
     * The capture frame to the world, checked against what Hypixel put out in the recorded runs (research
     * secrets_puzzles.md 1.4, 1.7): Crypt turned once from (-200, -168) with its chests at (-185, 60, -155)
     * and (-172, 89, -149); Tic Tac Toe turned twice from (-104, -200), its wall at x -84..-82, y 70..71, z -192.
     */
    @Test
    void toTheWorldAsRecorded() {
        RoomFrame crypt = new RoomFrame(-200, -168, 63, 31, 1);
        assertEquals(new PastePlan.Block(-185, 60, -155), crypt.block(13, 60, 15));
        assertEquals(new PastePlan.Block(-186, 81, -166), crypt.block(2, 81, 16));
        assertEquals(new PastePlan.Block(-172, 89, -149), crypt.block(19, 89, 2));
        assertEquals(Direction.SOUTH, Direction.EAST.rotateClockwise(1));
        assertEquals(Direction.WEST, Direction.SOUTH.rotateClockwise(1));
        RoomFrame ticTacToe = new RoomFrame(-104, -200, 31, 31, 2);
        assertEquals(new PastePlan.Block(-83, 69, -195), ticTacToe.block(9, 69, 25));
        assertEquals(new PastePlan.Block(-87, 73, -195), ticTacToe.block(13, 73, 25));
        assertEquals(new PastePlan.Block(-82, 70, -192), ticTacToe.block(8, 70, 22));
        assertEquals(new PastePlan.Block(-84, 71, -192), ticTacToe.block(10, 71, 22));
    }

    // The private data (rooms/_secrets), where it's there: -Dsecrets.dir, else the data checkout next to this repository.

    private static Path privateData() {
        String property = System.getProperty("secrets.dir");
        if (property != null) return Path.of(property);
        Path repository = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent();
        return repository.resolveSibling("skyblock-dungeon-data").resolve("rooms").resolve(SecretData.FOLDER);
    }

    private static SecretData.Room recorded(String id) {
        Path folder = privateData();
        assumeTrue(Files.isDirectory(folder), "no " + folder);
        SecretData data = SecretData.load(folder.getParent().getParent());
        assertEquals(List.of(), data.problems());
        SecretData.Room room = data.room(id);
        assertNotNull(room, id);
        return room;
    }

    /** Every waypoint of a kind, in the world. */
    private static List<PastePlan.Block> placed(SecretData.Room room, SecretData.Kind kind, RoomFrame frame) {
        List<PastePlan.Block> out = new ArrayList<>();
        for (SecretData.Waypoint w : room.waypoints()) {
            if (w.kind() == kind) out.add(frame.block(w.x(), w.y(), w.z()));
        }
        return out;
    }

    private static SecretData.Waypoint at(SecretData.Room room, SecretData.Kind kind, RoomFrame frame, int x, int y, int z) {
        for (SecretData.Waypoint w : room.waypoints()) {
            if (w.kind() == kind && frame.block(w.x(), w.y(), w.z()).equals(new PastePlan.Block(x, y, z))) return w;
        }
        throw new AssertionError(kind + " " + x + "," + y + "," + z + " not in " + placed(room, kind, frame));
    }

    /** A chest where the recording had one, facing (in the world) as it did, put out at the start or not. */
    private static void chest(SecretData.Room room, RoomFrame frame, int x, int y, int z, Direction facing, boolean start) {
        SecretData.Waypoint w = at(room, SecretData.Kind.CHEST, frame, x, y, z);
        assertEquals(facing, w.facing() == null ? null : w.facing().rotateClockwise(frame.turns()), w.name());
        assertEquals(start, w.start(), w.name());
    }

    /** R1: Crypt's three chests put out when the player walked in (00:50.8), with their facings. */
    @Test
    void cryptAsRecorded() {
        SecretData.Room crypt = recorded("crypt");
        assertEquals(5, crypt.secrets());
        RoomFrame frame = new RoomFrame(-200, -168, 63, 31, 1);
        chest(crypt, frame, -172, 89, -149, Direction.WEST, false);
        chest(crypt, frame, -186, 81, -166, Direction.SOUTH, false);
        chest(crypt, frame, -185, 60, -155, Direction.SOUTH, false);
    }

    /** R1: the Tic Tac Toe chest behind its lever's wall (put out when the player got near, 03:54.7), and the wall that moved. */
    @Test
    void ticTacToeAsRecorded() {
        SecretData.Room room = recorded("tic_tac_toe");
        RoomFrame frame = new RoomFrame(-104, -200, 31, 31, 2);
        chest(room, frame, -83, 69, -195, Direction.SOUTH, false);
        SecretData.Waypoint lever = at(room, SecretData.Kind.LEVER, frame, -87, 73, -195);
        List<PastePlan.Block> wall = new ArrayList<>();
        for (int[] b : lever.wall()) wall.add(frame.block(b[0], b[1], b[2]));
        assertEquals(6, wall.size());
        for (int x = -84; x <= -82; x++) {
            assertTrue(wall.contains(new PastePlan.Block(x, 70, -192)));
            assertTrue(wall.contains(new PastePlan.Block(x, 71, -192)));
        }
    }

    /**
     * R1 Pirate (BetterMap's data, turned twice from (-168, -136)): the Wither Essence head at (-125, 60, -78),
     * and a chest facing east put out at the start (00:32.6).
     */
    @Test
    void pirateAsRecorded() {
        SecretData.Room pirate = recorded("pirate");
        RoomFrame frame = new RoomFrame(-168, -136, 63, 63, 2);
        at(pirate, SecretData.Kind.WITHER, frame, -125, 60, -78);
        chest(pirate, frame, -123, 83, -134, Direction.EAST, true);
    }

    /** R1 Long Hall (not captured yet): its three chests, the lever to chest 2 and the wall it moved (x -127..-125, y 84..88, z -180). */
    @Test
    void longHallAsRecorded() {
        SecretData.Room hall = recorded("long_hall");
        RoomFrame frame = new RoomFrame(-136, -200, 31, 31, 0);
        chest(hall, frame, -118, 62, -177, Direction.SOUTH, false);
        chest(hall, frame, -108, 81, -175, Direction.SOUTH, true);
        chest(hall, frame, -108, 82, -185, Direction.WEST, false);
        SecretData.Waypoint lever = at(hall, SecretData.Kind.LEVER, frame, -126, 70, -197);
        List<PastePlan.Block> wall = new ArrayList<>();
        for (int[] b : lever.wall()) wall.add(frame.block(b[0], b[1], b[2]));
        assertEquals(15, wall.size());
        for (int x = -127; x <= -125; x++) {
            for (int y = 84; y <= 88; y++) assertTrue(wall.contains(new PastePlan.Block(x, y, -180)), x + "," + y);
        }
    }

    /**
     * R2: Criss-Cross's chest at the start (01:52.4), Redstone Warrior's behind its lever at the start, Altar's
     * item and two chests (01:53.3), Red Green's chest behind its lever when the player walked in (02:23.6), with
     * the facings recorded.
     */
    @Test
    void secondRunAsRecorded() {
        chest(recorded("criss_cross"), new RoomFrame(-168, -200, 31, 31, 0), -153, 66, -194, Direction.SOUTH, true);
        SecretData.Room warrior = recorded("redstone_warrior");
        RoomFrame warriorFrame = new RoomFrame(-200, -168, 63, 31, 0);
        chest(warrior, warriorFrame, -176, 58, -153, Direction.EAST, true);
        at(warrior, SecretData.Kind.LEVER, warriorFrame, -196, 62, -153);
        chest(recorded("red_green"), new RoomFrame(-136, -168, 31, 31, 0), -107, 75, -153, Direction.WEST, false);
        SecretData.Room altar = recorded("altar");
        RoomFrame altarFrame = new RoomFrame(-136, -136, 63, 63, 2);
        at(altar, SecretData.Kind.ITEM, altarFrame, -88, 71, -122);
        chest(altar, altarFrame, -93, 83, -117, Direction.SOUTH, false);
        chest(altar, altarFrame, -85, 44, -113, Direction.WEST, false);
    }

    /** R1 had 21 secrets: Crypt 5, Pirate 6, Long Hall 3, Tic Tac Toe 1 and Catwalk 6 (the tab list's 19% for 4). */
    @Test
    void recordedRoomTotals() {
        int total = 0;
        for (String id : List.of("crypt", "pirate", "long_hall", "tic_tac_toe", "catwalk")) total += recorded(id).secrets();
        assertEquals(21, total);
        // R2's action bar: Altar 0/6, Red Green 0/3, Criss-Cross 0/1.
        assertEquals(6, recorded("altar").secrets());
        assertEquals(3, recorded("red_green").secrets());
        assertEquals(1, recorded("criss_cross").secrets());
    }
}
