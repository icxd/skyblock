package net.icxd.dungeons.item.gemstone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.icxd.dungeons.hex.HexData;
import net.icxd.dungeons.hex.PrivateHex;

/** The private gemstone table for tests, read as the plugin reads it (see GemstoneTable). */
public final class PrivateGemstones {
    private PrivateGemstones() {
    }

    /**
     * The private data's table, with nothing wrong in it. The test is skipped without it, also when the folder is
     * there with only the other categories' tables.
     */
    public static GemstoneTable table() {
        Path folder = PrivateHex.folder();
        assumeTrue(Files.isRegularFile(folder.resolve(GemstoneTable.FILE)), "no " + folder.resolve(GemstoneTable.FILE));
        List<String> problems = new ArrayList<>();
        GemstoneTable table = GemstoneTable.read(HexData.json(folder, GemstoneTable.FILE, problems), problems);
        assertNotNull(table, problems.toString());
        assertEquals(List.of(), problems);
        return table;
    }
}
