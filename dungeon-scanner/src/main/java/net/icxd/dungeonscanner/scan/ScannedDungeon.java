package net.icxd.dungeonscanner.scan;

import java.util.List;

import net.icxd.dungeonscanner.scan.RoomDatabase.RoomInfo;

/** Everything one scan pass found out about the dungeon layout. */
public record ScannedDungeon(List<Room> rooms, List<Door> doors, int width, int height, boolean complete) {

  /** A grid cell index (0..5, 0..5). */
  public record Cell(int x, int z) {
    public int centerX() {
      return DungeonScan.CENTER + x * DungeonScan.PITCH;
    }

    public int centerZ() {
      return DungeonScan.CENTER + z * DungeonScan.PITCH;
    }
  }

  public enum DoorType { NORMAL, ENTRANCE, WITHER, BLOOD }

  /**
   * @param info            null if the room isn't in the database
   * @param rotation        null if the roof marker wasn't found
   * @param clayX           world position of the roof marker (or where it should be)
   * @param complete        every cell and every surrounding gap was loaded, so the room's extent
   *                        and doors are final
   */
  public record Room(
      String id,
      RoomInfo info,
      List<Cell> cells,
      int core,
      int roof,
      RoomRotation rotation,
      int clayX,
      int clayY,
      int clayZ,
      boolean complete
  ) {
    public String type() {
      return info != null ? info.type() : "UNKNOWN";
    }

    public String shape() {
      return info != null ? info.shape() : DungeonScan.shapeOf(cells);
    }
  }

  /** A door in the 1-block gap between two cells of different rooms. */
  public record Door(Cell a, Cell b, DoorType type, int x, int z) {
  }
}
