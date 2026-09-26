package net.icxd.dungeons.dungeons.generation.utils;

/**
 * The wall segment between two orthogonally adjacent grid cells. This is where a door can go
 * (Hypixel puts doors in the middle of a cell's wall, so every cell edge is exactly one door slot).
 * Always normalized so {@link #a()} is the north/west cell, which makes it usable as a map key.
 */
public record Edge(Position a, Position b) {

    public Edge {
        if (a.manhattan(b) != 1) {
            throw new IllegalArgumentException("Cells are not adjacent: " + a + " " + b);
        }
        if (b.x() < a.x() || b.y() < a.y()) {
            Position t = a;
            a = b;
            b = t;
        }
    }

    public static Edge of(Position cell, Direction side) {
        return new Edge(cell, cell.offset(side));
    }

    public boolean touches(Position cell) {
        return a.equals(cell) || b.equals(cell);
    }

    public Position other(Position cell) {
        if (a.equals(cell)) return b;
        if (b.equals(cell)) return a;
        throw new IllegalArgumentException(cell + " is not on " + this);
    }

    /** Side of {@code cell} this edge is on. */
    public Direction sideOf(Position cell) {
        Position o = other(cell);
        for (Direction d : Direction.values()) {
            if (cell.offset(d).equals(o)) return d;
        }
        throw new IllegalStateException();
    }
}
