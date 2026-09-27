package net.icxd.dungeons.mob;

/**
 * How a mob of a kind is spawned: starred (a room's required mobs), with a modifier (null for none),
 * times the room's health and damage multiplier (1 outside dungeons, or for a room that has none), and
 * at which of its floor's levels (null for the floor's first, see {@link MobKind#variant}).
 */
public record SpawnOptions(boolean starred, Modifier modifier, double roomMultiplier, Integer level) {
    public static final SpawnOptions NONE = new SpawnOptions(false, null, 1, null);

    public SpawnOptions {
        if (!(roomMultiplier > 0)) throw new IllegalArgumentException("room multiplier " + roomMultiplier);
    }

    public SpawnOptions starred(boolean starred) {
        return new SpawnOptions(starred, modifier, roomMultiplier, level);
    }

    public SpawnOptions modifier(Modifier modifier) {
        return new SpawnOptions(starred, modifier, roomMultiplier, level);
    }

    /**
     * Hypixel scales the health and damage of a room's mobs when it's opened: the Phase 1 room code
     * works out by how much (the recordings fit 1 + 0.05 x (squares opened before, Entrance not
     * counted, - 1), and 1.05 for the first room).
     */
    public SpawnOptions roomMultiplier(double roomMultiplier) {
        return new SpawnOptions(starred, modifier, roomMultiplier, level);
    }

    public SpawnOptions level(Integer level) {
        return new SpawnOptions(starred, modifier, roomMultiplier, level);
    }
}
