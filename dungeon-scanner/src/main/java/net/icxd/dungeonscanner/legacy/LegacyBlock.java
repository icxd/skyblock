package net.icxd.dungeonscanner.legacy;

/** A 1.8 block: numeric id + 4-bit data value. */
public record LegacyBlock(int id, int data) {
  public static final LegacyBlock AIR = new LegacyBlock(0, 0);

  public int packed() {
    return id << 4 | data;
  }

  @Override
  public String toString() {
    return id + ":" + data;
  }
}
