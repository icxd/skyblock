package net.icxd.dungeons.command.commands.admin;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.dungeons.generator.Grid;
import net.icxd.dungeons.dungeons.generator.Pathfinder;
import net.icxd.dungeons.dungeons.generator.rooms.Connection;
import net.icxd.dungeons.dungeons.generator.rooms.Room;
import net.icxd.dungeons.dungeons.generator.rooms.RoomSize;
import net.icxd.dungeons.dungeons.generator.utils.Coordinate;
import net.icxd.dungeons.user.Rank;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandParameters(aliases = "dungeon", permission = Rank.ADMIN)
public class DungeonCommand extends SCommand {
  @Override
  public void run(CommandSource source, String[] args) {
    Grid grid = new Grid(10, 10);
    grid.generate();
//    grid.print();

    Pathfinder pathfinder = new Pathfinder();
    var nodes = pathfinder.chooseFarApartNodes(grid, 14);
    Coordinate start = nodes[0], goal = nodes[1];
    var path = pathfinder.findPath(grid, start, goal);
    if (path.isEmpty())
      throw new RuntimeException("Failed to generate path from start node to goal node");

    int[][] offsets = {{1, 0}, {1, 1}, {0, 1}, {-1, 1},
        {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
    List<Coordinate> roomLocations = new ArrayList<>();
    for (Coordinate c : path) {
      for (int[] offset : offsets) {
        var neighbor = new Coordinate(c.x() + offset[0], c.y() + offset[1]);
        if (!grid.isValidCoordinate(neighbor)) continue;
        if (roomLocations.contains(neighbor)) continue;

        roomLocations.add(neighbor);
      }
    }

    int roomIndex = 0;
    final Map<Coordinate, Integer> roomIndices = new HashMap<>();
    for (int i = 0; i < grid.getWidth() * grid.getHeight(); i++) {
      final int x = i / grid.getWidth(), y = i - (x * grid.getWidth());
      final Coordinate c = new Coordinate(x, y);
      if (!roomLocations.contains(c)) continue;

      final RoomSize size = RoomSize.getRandomRoomSize();
      final Coordinate offset = size.getOffsetToCorner();

      for (int x2 = 0; x2 < offset.x(); x2++) {
        for (int y2 = 0; y2 < offset.y(); y2++) {
          final Coordinate point = new Coordinate(x + x2, y + y2);
          if (!grid.isValidCoordinate(point)) continue;
          if (roomIndices.containsKey(point)) continue;

          roomIndices.put(point, roomIndex);
        }
      }

      roomIndex++;
    }

    final List<Connection> connections = new ArrayList<>();
    roomIndices.forEach((coordinate, index) -> {

    });

    World world = source.getPlayer().getWorld();
    for (int i = 0; i < grid.getWidth() * grid.getHeight(); i++) {
      final int x = i / grid.getWidth(), y = i - (x * grid.getWidth());
      final Coordinate coordinate = new Coordinate(x, y);
      final Room room = grid.getRoom(coordinate);
      Location location = new Location(world, x * 3, 100, y * 3);

      for (int x1 = 0; x1 < 3; x1++) {
        for (int y1 = 0; y1 < 3; y1++) {
          Location l = location.clone().add(x1, 0, y1);
          world.getBlockAt(l).setType(Material.WOOL);
          world.getBlockAt(l).setData(
              coordinate.equals(start) ? (byte) 4 :
                  coordinate.equals(goal) ? (byte) 4 :
                      path.contains(coordinate) ? (byte) 2 :
                          roomLocations.contains(coordinate) ? (byte) 10 :
                              switch (room.type()) {
                                case NONE -> (byte) 15;
                                case RED -> (byte) 14;
                                case GREEN -> (byte) 5;
                                case BLUE -> (byte) 11;
                              });

          Location l1 = location.clone().add(x1, 1, y1);
          if (roomIndices.containsKey(coordinate)) {
            final int index = roomIndices.get(coordinate);
            final byte color = (byte) (index % 15);
            world.getBlockAt(l1).setType(Material.STAINED_GLASS);
            world.getBlockAt(l1).setData(color);
          } else world.getBlockAt(l1).setType(Material.AIR);
        }
      }

      for (var connection : connections) {
        boolean down = connection.down();
        Location loc = location.clone().add(1, 2, 1);

        loc = loc.add(down ? 0 : 1, 0, down ? 1 : 0);
        world.getBlockAt(loc).setType(Material.WOOL);
        world.getBlockAt(loc).setData((byte) 7);
      }

      Location loc = location.clone().add(1, 2, 1);
      if (roomIndices.containsKey(coordinate)) {
        world.getBlockAt(loc).setType(Material.SIGN_POST);
        Sign sign = (Sign) world.getBlockAt(loc).getState();
        sign.setLine(1, "" + roomIndices.get(coordinate));
        sign.update(true, false);
      } else world.getBlockAt(loc).setType(Material.AIR);
    }

//    for (int x = 0; x < grid.getWidth(); x++) {
//      StringBuilder builder = new StringBuilder();
//
//      for (int y = 0; y < grid.getHeight(); y++) {
//        Coordinate current = new Coordinate(x, y);
//        if (current.equals(start))
//          builder.append(ChatColor.YELLOW).append("s ").append(ChatColor.RESET);
//        else if (current.equals(goal))
//          builder.append(ChatColor.YELLOW).append("g ").append(ChatColor.RESET);
//        else if (roomIndices.containsKey(current))
//          builder.append(roomIndices.get(current)).append(" ");
//        else if (roomLocations.contains(current))
//          builder.append(ChatColor.DARK_PURPLE).append("# ").append(ChatColor.RESET);
//        else if (path.contains(current))
//          builder.append(ChatColor.LIGHT_PURPLE).append("# ").append(ChatColor.RESET);
//        else {
//          Room room = grid.getRoom(current);
//          builder.append(room.type().color()).append("x ").append(ChatColor.RESET);
//        }
//      }
//
//      source.send(builder.toString());
//    }
  }
}
