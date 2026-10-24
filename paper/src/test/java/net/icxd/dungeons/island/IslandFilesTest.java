package net.icxd.dungeons.island;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IslandFilesTest {
    @Test
    void anIslandsRegionFilesGoAndComeBack(@TempDir Path dir) throws IOException {
        Path world = dir.resolve("island-a");
        Files.createDirectories(world.resolve("region"));
        Files.createDirectories(world.resolve("entities"));
        Files.write(world.resolve("region/r.0.0.mca"), new byte[]{1, 2, 3});
        Files.write(world.resolve("region/r.-1.0.mca"), new byte[]{4});
        Files.write(world.resolve("entities/r.0.0.mca"), new byte[]{5, 6});
        // Not the island's: the world's own data, and stray files.
        Files.writeString(world.resolve("level.dat"), "x");
        Files.writeString(world.resolve("region/notes.txt"), "x");

        Map<String, byte[]> files = IslandFiles.read(world);
        assertEquals(java.util.List.of("entities/r.0.0.mca", "region/r.-1.0.mca", "region/r.0.0.mca"), java.util.List.copyOf(files.keySet()));
        Path back = dir.resolve("island-b");
        IslandFiles.unzip(IslandFiles.zip(files), back);
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(back.resolve("region/r.0.0.mca")));
        assertArrayEquals(new byte[]{5, 6}, Files.readAllBytes(back.resolve("entities/r.0.0.mca")));
        assertFalse(Files.exists(back.resolve("level.dat")));
    }

    /** A stored island holds region files and nothing else: anything else in one is refused, wherever it'd go. */
    @Test
    void onlyRegionFilesComeOut(@TempDir Path dir) throws IOException {
        for (String name : new String[]{"../evil.mca", "region/../../evil.mca", "level.dat", "/region/r.0.0.mca", "region/r.0.0.mca/x"}) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(1);
                zip.closeEntry();
            }
            assertThrows(IOException.class, () -> IslandFiles.unzip(bytes.toByteArray(), dir.resolve("island")), name);
        }
        assertFalse(Files.exists(dir.resolve("evil.mca")));
    }

    @Test
    void aCaptureMakesANewIsland(@TempDir Path dir) throws IOException {
        Path capture = dir.resolve("1_your_island");
        Files.createDirectories(capture.resolve("region"));
        Files.createDirectories(capture.resolve("entities"));
        Files.write(capture.resolve("region/r.0.0.mca"), new byte[]{7});
        Files.write(capture.resolve("entities/r.0.0.mca"), new byte[]{8});
        Files.writeString(capture.resolve("capture.json"), "{}");
        Path world = dir.resolve("island-c");
        IslandFiles.copyCapture(capture, world);
        assertTrue(Files.exists(world.resolve("region/r.0.0.mca")));
        assertTrue(Files.exists(world.resolve("entities/r.0.0.mca")));
        assertFalse(Files.exists(world.resolve("capture.json")));
    }
}
