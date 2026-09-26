package net.icxd.dungeons.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** {@code config.properties} in the plugin's folder; written with defaults if it's missing. */
record ProxyConfig(String mongoUri, String mongoDatabase, int runsPerDungeonServer, int maxPartySize) {
    static final String FILE = "config.properties";

    private static final String DEFAULTS = """
            # SkyBlock proxy plugin settings.
            # The same MongoDB as the Paper servers: server types and load, ranks, and dungeon runs come from it.
            mongodb.uri=mongodb://localhost:27017
            mongodb.database=dungeons
            # How many dungeon runs share one dungeon server before parties go to another (or wait in line).
            dungeons.runs-per-server=4
            party.max-size=10
            """;

    static ProxyConfig load(Path dataDirectory) throws IOException {
        Path file = dataDirectory.resolve(FILE);
        if (!Files.exists(file)) {
            Files.createDirectories(dataDirectory);
            Files.writeString(file, DEFAULTS);
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        }
        return new ProxyConfig(
                properties.getProperty("mongodb.uri", "mongodb://localhost:27017").trim(),
                properties.getProperty("mongodb.database", "dungeons").trim(),
                Math.max(1, Integer.parseInt(properties.getProperty("dungeons.runs-per-server", "4").trim())),
                Math.max(2, Integer.parseInt(properties.getProperty("party.max-size", "10").trim())));
    }
}
