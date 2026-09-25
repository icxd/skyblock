package net.icxd.dungeonscanner.scan;

import net.minecraft.world.level.block.Rotation;

/**
 * How a room is turned in the world. Every Hypixel room has a blue terracotta block on its roof in
 * one corner; the rotation is named after where that corner is relative to the room (Odin's
 * convention). Saving a room "to north" moves that block to the room's north-west corner, which
 * makes the same room come out identical no matter how it was placed.
 *
 * <p>{@link #dx}/{@link #dz}: where the marker sits relative to a 1x1 room's centre.
 */
public enum RoomRotation {
  NORTH(15, 15),
  SOUTH(-15, -15),
  WEST(15, -15),
  EAST(-15, 15);

  public final int dx;
  public final int dz;

  RoomRotation(int dx, int dz) {
    this.dx = dx;
    this.dz = dz;
  }

  /** Clockwise quarter turns that take world space to the room's own frame. */
  public int quarterTurns() {
    return switch (this) {
      case SOUTH -> 0;
      case EAST -> 1;
      case NORTH -> 2;
      case WEST -> 3;
    };
  }

  /** The same turn as {@link #toLocalX}/{@link #toLocalZ}, for block states. */
  public Rotation blockRotation() {
    return switch (this) {
      case SOUTH -> Rotation.NONE;
      case EAST -> Rotation.CLOCKWISE_90;
      case NORTH -> Rotation.CLOCKWISE_180;
      case WEST -> Rotation.COUNTERCLOCKWISE_90;
    };
  }

  /** World offset from the marker block -> room-local offset (Odin's {@code rotateToNorth}). */
  public int toLocalX(int dx, int dz) {
    return switch (this) {
      case SOUTH -> dx;
      case EAST -> -dz;
      case NORTH -> -dx;
      case WEST -> dz;
    };
  }

  public int toLocalZ(int dx, int dz) {
    return switch (this) {
      case SOUTH -> dz;
      case EAST -> dx;
      case NORTH -> -dz;
      case WEST -> -dx;
    };
  }
}
