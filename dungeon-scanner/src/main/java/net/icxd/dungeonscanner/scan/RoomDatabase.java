package net.icxd.dungeonscanner.scan;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Hypixel's rooms, keyed by "core": a hash of the block column in the middle of each of a room's
 * cells. The data is Odin's {@code rooms.json} (BSD 3-Clause, (c) odtheking,
 * https://github.com/odtheking/Odin); cores are computed exactly the way Odin does it, see
 * {@link RoomCore}.
 */
public final class RoomDatabase {
  /** One entry of rooms.json. */
  public record RoomInfo(String name, String type, String shape, List<Integer> cores, int crypts) {
    /** File-name friendly id, e.g. "Water Board" -> "water_board". */
    public String id() {
      return idOf(name);
    }

    public int cellCount() {
      return switch (shape) {
        case "1x1" -> 1;
        case "1x2" -> 2;
        case "1x3", "L" -> 3;
        default -> 4;
      };
    }
  }

  private final List<RoomInfo> rooms;
  private final Map<Integer, RoomInfo> byCore = new HashMap<>();

  public RoomDatabase(List<RoomInfo> rooms) {
    this.rooms = List.copyOf(rooms);
    for (RoomInfo r : rooms) for (int core : r.cores()) byCore.put(core, r);
  }

  public static RoomDatabase load() {
    try (InputStream in = RoomDatabase.class.getResourceAsStream("/assets/dungeonscanner/rooms.json")) {
      if (in == null) throw new IllegalStateException("rooms.json missing from the mod jar");
      List<RoomInfo> rooms = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
          new TypeToken<List<RoomInfo>>() { }.getType());
      return new RoomDatabase(rooms);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Couldn't read rooms.json", e);
    }
  }

  public RoomInfo byCore(int core) {
    return byCore.get(core);
  }

  public List<RoomInfo> all() {
    return rooms;
  }

  public static String idOf(String name) {
    return name.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
  }
}
