package net.icxd.dungeons.hex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonElement;

/** The Hex's data folder. */
class HexDataTest {
    @Test
    void json(@TempDir Path folder) throws IOException {
        Files.writeString(folder.resolve("reforges.json"), "{\"a\": 1}");
        Files.writeString(folder.resolve("broken.json"), "{");
        List<String> problems = new ArrayList<>();
        JsonElement read = HexData.json(folder, "reforges.json", problems);
        assertEquals(1, read.getAsJsonObject().get("a").getAsInt());
        assertEquals(List.of(), problems);
        assertNull(HexData.json(folder, "missing.json", problems));
        assertNull(HexData.json(folder, "broken.json", problems));
        assertEquals(2, problems.size());
        assertTrue(problems.get(0).startsWith("no "));
        assertTrue(problems.get(1).startsWith("broken.json: "));
    }
}
