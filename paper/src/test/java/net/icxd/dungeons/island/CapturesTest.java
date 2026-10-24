package net.icxd.dungeons.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CapturesTest {
    private static Path capture(Path worlds, String folder, String areasJson, String to) throws IOException {
        Path dir = Files.createDirectories(worlds.resolve(folder).resolve("region")).getParent();
        Files.writeString(dir.resolve("capture.json"), "{\"areas\": " + areasJson + ", \"to\": \"" + to + "\"}");
        return dir;
    }

    /** As the user's first captures have them: Hypixel's made-up codes still in the names. */
    @Test
    void foundByTheAreaItsSidebarNamed(@TempDir Path worlds) throws IOException {
        Path island = capture(worlds, "1_your_isla_tnd", "[\"Your Isla§tnd\"]", "2026-10-24T10:33:26Z");
        Path hub = capture(worlds, "1_village_u", "[\"Village§u\", \"Mining Di§ustrict\", \"Farm§u\"]", "2026-10-24T10:54:06Z");
        assertEquals(island, Captures.find(worlds, "Your Island"));
        assertEquals(hub, Captures.find(worlds, "Farm"));
        assertNull(Captures.find(worlds, "Dungeon Hub"));
    }

    @Test
    void theNewestOfTwo(@TempDir Path worlds) throws IOException {
        capture(worlds, "a", "[\"Your Island\"]", "2026-10-24T10:33:26Z");
        Path newer = capture(worlds, "b", "[\"Your Island\"]", "2026-11-01T09:00:00Z");
        assertEquals(newer, Captures.find(worlds, "Your Island"));
    }

    @Test
    void nothingWithoutTheFolderOrItsFiles(@TempDir Path worlds) throws IOException {
        assertNull(Captures.find(worlds.resolve("missing"), "Your Island"));
        Path broken = Files.createDirectories(worlds.resolve("broken").resolve("region")).getParent();
        Files.writeString(broken.resolve("capture.json"), "not json");
        assertNull(Captures.find(worlds, "Your Island"));
        assertEquals("Your Island", Captures.clean("§7Your Isla§tnd"));
    }
}
