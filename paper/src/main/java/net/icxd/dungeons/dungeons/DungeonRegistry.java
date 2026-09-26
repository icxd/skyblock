package net.icxd.dungeons.dungeons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The dungeon runs on this server, by run id. */
public class DungeonRegistry {
    private static final Map<UUID, Dungeon> dungeons = new HashMap<>();

    /** The run a player is in, alive or dead; null if none. */
    public static Dungeon getDungeonByPlayer(UUID player) {
        for (Dungeon dungeon : dungeons.values()) {
            if (dungeon.isMember(player)) return dungeon;
        }
        return null;
    }

    public static void registerDungeon(Dungeon dungeon) {
        dungeons.put(dungeon.getId(), dungeon);
    }

    public static void unregisterDungeon(Dungeon dungeon) {
        dungeons.remove(dungeon.getId());
    }
}
