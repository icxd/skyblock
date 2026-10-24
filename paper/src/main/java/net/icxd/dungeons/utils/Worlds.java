package net.icxd.dungeons.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.generator.ChunkGenerator;

/** Where Paper keeps worlds, and the empty generator the plugin's own worlds use. */
public final class Worlds {
    private Worlds() {
    }

    /** Where Paper keeps the worlds: Paper 26 has them all in {@code world/dimensions/minecraft}, the main one as {@code overworld}. */
    public static Path dimensionsFolder() {
        Path main = Bukkit.getWorlds().get(0).getWorldPath();
        return main.getFileName().toString().equals("overworld") ? main.getParent() : Bukkit.getWorldContainer().toPath();
    }

    /** The folder a world of this name has (or would have). */
    public static Path folder(String name) {
        return dimensionsFolder().resolve(name);
    }

    /** Deletes a folder and everything in it; true if it's gone. */
    public static boolean delete(Path folder) throws IOException {
        if (!Files.exists(folder)) return true;
        try (Stream<Path> paths = Files.walk(folder)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
        return !Files.exists(folder);
    }

    /** Nothing at all: whatever the world has is pasted or copied in. */
    public static final class Void extends ChunkGenerator {
    }
}
