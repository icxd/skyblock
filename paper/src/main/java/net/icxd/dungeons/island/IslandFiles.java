package net.icxd.dungeons.island;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * An island's world as stored: its region files (blocks, entities and points of interest, the
 * folders a 26.2 world keeps per dimension), zipped. Nothing else of the world is kept: its spawn,
 * time and rules are the plugin's (see {@link Islands}).
 */
final class IslandFiles {
    static final List<String> FOLDERS = List.of("region", "entities", "poi");
    /** What a zip may hold: a region file in one of those folders, and nothing else (no paths elsewhere). */
    private static final Pattern ENTRY = Pattern.compile("(region|entities|poi)/r\\.-?\\d+\\.-?\\d+\\.mca");
    /** Past this, an island's zip is refused when it's read: not one a server wrote. */
    static final long MAX_BYTES = 256L * 1024 * 1024;

    private IslandFiles() {
    }

    /** A world folder's region files, by their path in it ("region/r.0.0.mca"). Read where the world is saved (the main thread). */
    static Map<String, byte[]> read(Path world) throws IOException {
        Map<String, byte[]> files = new TreeMap<>();
        for (String folder : FOLDERS) {
            Path dir = world.resolve(folder);
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> paths = Files.list(dir)) {
                for (Path path : paths.sorted().toList()) {
                    String name = folder + "/" + path.getFileName();
                    if (ENTRY.matcher(name).matches() && Files.isRegularFile(path)) files.put(name, Files.readAllBytes(path));
                }
            }
        }
        return files;
    }

    static byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(file.getKey()));
                zip.write(file.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    /** Writes a stored island's files into a world folder. Throws on anything that isn't a region file where one goes. */
    static void unzip(byte[] data, Path world) throws IOException {
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                if (entry.isDirectory()) continue;
                if (!ENTRY.matcher(entry.getName()).matches()) throw new IOException("not an island file: " + entry.getName());
                byte[] content = zip.readAllBytes();
                total += content.length;
                if (total > MAX_BYTES) throw new IOException("island larger than " + MAX_BYTES + " bytes");
                Path target = world.resolve(entry.getName());
                Files.createDirectories(target.getParent());
                Files.write(target, content);
            }
        }
    }

    /**
     * A new island: a capture's region and entity files (its blocks, and decorations like the starter
     * island's Cobblestone Minion), as the scanner saved them.
     */
    static void copyCapture(Path capture, Path world) throws IOException {
        for (String folder : List.of("region", "entities")) {
            Path from = capture.resolve(folder);
            if (!Files.isDirectory(from)) continue;
            Path to = world.resolve(folder);
            Files.createDirectories(to);
            try (Stream<Path> paths = Files.list(from)) {
                for (Path path : paths.toList()) {
                    if (ENTRY.matcher(folder + "/" + path.getFileName()).matches()) Files.copy(path, to.resolve(path.getFileName()));
                }
            }
        }
    }
}
