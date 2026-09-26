package net.icxd.dungeons;

import java.util.List;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * This server: its name on the network and which part of SkyBlock it hosts ({@code server.type}
 * in config.yml). Each server hosts one area in its main world; listeners marked {@link OnlyOn}
 * only run where they belong.
 */
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
        LOBBY("Hub"),
        /** Dungeon runs. */
        DUNGEONS("The Catacombs"),
        CRIMSON_ISLE("Crimson Isle"),
        DWARVEN_MINES("Dwarven Mines"),
        /** For development: runs everything. */
        NONE("Dev");

        @Getter
        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }
    }

    /** The world the area is in (level-name in server.properties). Dungeon runs get worlds of their own. */
    public World getMainWorld() {
        return Bukkit.getWorlds().get(0);
    }

    /** Whether something marked {@link OnlyOn} belongs on this server. */
    public boolean runs(Class<?> type) {
        OnlyOn only = type.getAnnotation(OnlyOn.class);
        return only == null || serverType == Type.NONE || List.of(only.value()).contains(serverType);
    }

    /** Areas with regions (the hub, the mines, ...); dungeons have rooms instead. */
    public boolean hasRegions() {
        return serverType != Type.DUNGEONS;
    }
}
