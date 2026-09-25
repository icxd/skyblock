package net.icxd.dungeons.dungeons.generation;

/**
 * Door colours. Only the critical path (entrance -> blood) has special doors: its first door is the
 * entrance door, its last the blood door, and every door in between a wither door. That's how
 * IllegalMap reconstructs door types from the map ({@code DungeonMap.setupTree}); in the world they
 * are infested stone bricks, coal blocks and stained clay respectively.
 */
public enum DoorType {
  /** Brown on the map, just an opening. */
  NORMAL,
  /** The door out of the entrance room. */
  ENTRANCE,
  /** Black, needs a Wither Key from the room before it. */
  WITHER,
  /** Red, needs the Blood Key from the room before it. */
  BLOOD,
}
