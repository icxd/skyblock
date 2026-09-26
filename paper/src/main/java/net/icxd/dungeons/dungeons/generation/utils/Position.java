package net.icxd.dungeons.dungeons.generation.utils;

/** A grid cell. {@code y} is the world Z axis. */
public record Position(int x, int y) {

  public Position offset(Direction direction) {
    return new Position(x + direction.dx, y + direction.dy);
  }

  public Position plus(Position other) {
    return new Position(x + other.x, y + other.y);
  }

  public Position minus(Position other) {
    return new Position(x - other.x, y - other.y);
  }

  /** Rotates around the origin by 90 degrees clockwise {@code quarterTurns} times. */
  public Position rotateClockwise(int quarterTurns) {
    int px = x;
    int py = y;
    for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
      int t = px;
      px = -py;
      py = t;
    }
    return new Position(px, py);
  }

  public int manhattan(Position other) {
    return Math.abs(x - other.x) + Math.abs(y - other.y);
  }
}
