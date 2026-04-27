package net.icxd.dungeons.dungeons;

import org.bukkit.entity.Player;

import java.util.HashMap;

public class DungeonRegistry {
    private static final HashMap<Player, Dungeon> dungeons = new HashMap<>();

    public static Dungeon getDungeonByPlayer(Player player) {
        for (Dungeon dungeon : dungeons.values()) {
            if (dungeon.getPlayers().contains(player)) {
                return dungeon;
            }
        }
        return null;
    }

    public static void registerDungeon(Dungeon dungeon) {
        dungeons.put(dungeon.getOwner(), dungeon);
    }

    public static void unregisterDungeon(Dungeon dungeon) {
        dungeons.remove(dungeon.getOwner());
    }
}
