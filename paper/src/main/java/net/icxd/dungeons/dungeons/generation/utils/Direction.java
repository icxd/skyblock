package net.icxd.dungeons.dungeons.generation.utils;

/**
 * Grid directions. {@code y} grows towards {@link #SOUTH} (it maps to world Z).
 */
public enum Direction {
    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** Rotates 90 degrees clockwise {@code quarterTurns} times. */
    public Direction rotateClockwise(int quarterTurns) {
        return values()[Math.floorMod(ordinal() + quarterTurns, 4)];
    }

    public Direction opposite() {
        return rotateClockwise(2);
    }
}
