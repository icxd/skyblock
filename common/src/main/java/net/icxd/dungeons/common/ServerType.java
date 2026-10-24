package net.icxd.dungeons.common;

/**
 * Which part of SkyBlock a server hosts ({@code server.type} in the Paper plugin's config.yml).
 * The proxy uses it to send players to the right kind of server.
 */
public enum ServerType {
    LOBBY("Hub"),
    /** Where players go after a dungeon run; falls back to a hub when there's none. */
    DUNGEON_HUB("Dungeon Hub"),
    /** Dungeon runs. */
    DUNGEONS("The Catacombs"),
    CRIMSON_ISLE("Crimson Isle"),
    DWARVEN_MINES("Dwarven Mines"),
    /**
     * Private islands, as Hypixel's "mini" servers host them: each profile's island is a world of its
     * own, loaded on whichever of these servers its player is sent to.
     */
    ISLAND("Private Island"),
    /** For development: runs everything (the hub in its main world, islands in worlds of their own). */
    NONE("Dev");

    private final String displayName;

    ServerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
