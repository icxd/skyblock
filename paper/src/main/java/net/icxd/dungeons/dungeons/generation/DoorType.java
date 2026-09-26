package net.icxd.dungeons.dungeons.generation;

/**
 * Door types. Only the critical path (entrance -> blood) has special doors: its first door is the
 * entrance door, the one into the fairy room is the fairy door, its last the blood door, and every
 * other door on it a wither door. IllegalMap reconstructs door types from the map the same way
 * ({@code DungeonMap.setupTree}), and Odin tells the fairy door apart by its colour.
 */
public enum DoorType {
    /** Brown on the map, just an opening. */
    NORMAL,
    /** The door out of the entrance room (infested stone bricks). */
    ENTRANCE,
    /** Black, needs a Wither Key (coal blocks). */
    WITHER,
    /** The door into the fairy room, pink on the map. Opens without a key. */
    FAIRY,
    /** Red, needs the Blood Key from the room before it (stained clay). */
    BLOOD,
}
