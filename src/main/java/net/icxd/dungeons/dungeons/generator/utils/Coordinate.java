package net.icxd.dungeons.dungeons.generator.utils;

public record Coordinate(int x, int y) {
  @Override public String toString() { return x + ", " + y; }
}