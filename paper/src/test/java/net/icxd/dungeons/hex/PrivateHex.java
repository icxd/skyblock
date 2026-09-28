package net.icxd.dungeons.hex;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The Hex's private tables for tests (see HexData): {@code -Dhex.dir}, else the data checkout's hex/ next to this
 * repository. A test that needs them is skipped without them ({@link #folder()}).
 */
public final class PrivateHex {
    private PrivateHex() {
    }

    /** Where they'd be, there or not. */
    public static Path where() {
        String property = System.getProperty("hex.dir");
        if (property != null) return Path.of(property);
        Path repository = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent();
        return repository.resolveSibling("skyblock-dungeon-data").resolve(HexData.FOLDER);
    }

    /** The folder; the test is skipped if it isn't there. */
    public static Path folder() {
        Path folder = where();
        assumeTrue(Files.isDirectory(folder), "no " + folder);
        return folder;
    }
}
