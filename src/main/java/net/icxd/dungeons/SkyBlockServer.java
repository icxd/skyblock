package net.icxd.dungeons;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public class SkyBlockServer {
    private final String name;
    private final int port;
    private final Type serverType;

    public SkyBlockServer(FileConfiguration config) {
        this.name = config.getString("server.name");
        this.port = config.getInt("server.port");
        this.serverType = Type.valueOf(config.getString("server.type"));
    }

    public enum Type {
        LOBBY("hub"),
        DUNGEONS(null),
        CRIMSON_ISLE("crimson_isle"),
        DWARVEN_MINES("dwarven_mines"),
        NONE(null);

        @Getter
        private final String worldName;
        Type(String worldName) {
            this.worldName = worldName;
        }
    }
}
