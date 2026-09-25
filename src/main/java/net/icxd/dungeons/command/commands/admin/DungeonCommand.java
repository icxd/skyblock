package net.icxd.dungeons.command.commands.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.dungeons.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.DungeonConfig;
import net.icxd.dungeons.dungeons.generation.DungeonGenerator;
import net.icxd.dungeons.dungeons.generation.DungeonLayout;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.LayoutValidator;
import net.icxd.dungeons.dungeons.generation.room.HypixelRooms;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;
import net.icxd.dungeons.user.Rank;

/**
 * {@code /dungeon [floor] [seed]}: generates a layout, prints it to the console and builds a
 * block preview at y=100 around 0,0. Floor is e.g. {@code E}, {@code F7}, {@code M3}.
 */
@CommandParameters(aliases = "dungeon", permission = Rank.STAFF)
public class DungeonCommand extends SCommand {

  /** Blocks per grid cell in the preview. */
  private static final int CELL = 5;
  private static final int PREVIEW_Y = 100;

  @Override
  public void run(CommandSource source, String[] args) {
    DungeonFloor floor = args.length > 0 ? parseFloor(args[0]) : DungeonFloor.FLOOR_7;
    if (floor == null) {
      source.send(ChatColor.RED + "Unknown floor " + args[0] + ", use E, F1-F7 or M1-M7.");
      return;
    }
    long seed;
    try {
      seed = args.length > 1 ? Long.parseLong(args[1]) : new java.util.Random().nextLong();
    } catch (NumberFormatException e) {
      source.send(ChatColor.RED + "Seed must be a number.");
      return;
    }

    DungeonLayout layout = new DungeonGenerator(DungeonConfig.forFloor(floor), HypixelRooms.pool()).generate(seed);
    List<String> problems = LayoutValidator.validate(layout);

    Logger log = instance.getLogger();
    log.info("Dungeon " + floor.getName() + " seed " + seed + " (" + layout.getAttempts() + " attempts)");
    for (String line : layout.render().split("\n")) log.info(line);
    for (PlacedRoom r : layout.getRooms()) {
      log.info(String.format("  #%d %-8s %-12s %-16s rot=%d parent=%d doors=%d cells=%s",
          r.id(), r.type(), r.shape(), r.template().getId(), r.rotation(), r.parent(), r.doors().size(), r.cells()));
    }
    problems.forEach(p -> log.warning("INVALID: " + p));

    renderPreview(layout, source.getPlayer().getWorld());
    source.send(ChatColor.GREEN + floor.getName() + " seed " + seed + ": " + layout.getRooms().size() + " rooms"
        + (problems.isEmpty() ? "" : ChatColor.RED + " (" + problems.size() + " problems, see console)"));
    source.send(ChatColor.GRAY + "Preview at y=" + PREVIEW_Y + ": lime = critical path, gold = door, "
        + "infested stone = entrance door, coal = wither door, redstone = blood door. Map printed to the console.");
  }

  private static DungeonFloor parseFloor(String arg) {
    String a = arg.toUpperCase();
    if (a.equals("E") || a.equals("ENTRANCE")) return DungeonFloor.ENTRANCE;
    for (DungeonFloor f : DungeonFloor.values()) {
      if (f.name().equals(a)) return f;
      if (f.getNumber() > 0 && a.equals((f.isMasterMode() ? "M" : "F") + f.getNumber())) return f;
    }
    return null;
  }

  private static void renderPreview(DungeonLayout layout, World world) {
    Set<Integer> path = new HashSet<>(layout.getCriticalPath());
    int w = layout.getWidth();
    int h = layout.getHeight();
    int size = CELL + 1;

    for (int bx = 0; bx <= w * size; bx++) {
      for (int bz = 0; bz <= h * size; bz++) {
        world.getBlockAt(bx, PREVIEW_Y, bz).setType(Material.AIR);
        world.getBlockAt(bx, PREVIEW_Y + 1, bz).setType(Material.AIR);
      }
    }

    for (PlacedRoom room : layout.getRooms()) {
      byte color = path.contains(room.id()) ? 5 : (byte) Math.floorMod(room.id() * 7 + 3, 16);
      Set<Position> cells = new HashSet<>(room.cells());
      for (Position c : room.cells()) {
        int ox = c.x() * size;
        int oz = c.y() * size;
        for (int i = 0; i < CELL; i++) {
          for (int j = 0; j < CELL; j++) {
            wool(world.getBlockAt(ox + i, PREVIEW_Y, oz + j), color);
          }
        }
        // Fill the gap towards other cells of the same room so it reads as one room.
        if (cells.contains(new Position(c.x() + 1, c.y()))) {
          for (int j = 0; j < CELL; j++) wool(world.getBlockAt(ox + CELL, PREVIEW_Y, oz + j), color);
        }
        if (cells.contains(new Position(c.x(), c.y() + 1))) {
          for (int i = 0; i < CELL; i++) wool(world.getBlockAt(ox + i, PREVIEW_Y, oz + CELL), color);
        }
        if (cells.containsAll(List.of(new Position(c.x() + 1, c.y()), new Position(c.x(), c.y() + 1),
            new Position(c.x() + 1, c.y() + 1)))) {
          wool(world.getBlockAt(ox + CELL, PREVIEW_Y, oz + CELL), color);
        }
      }

      Position label = room.cells().get(0);
      Block signBlock = world.getBlockAt(label.x() * size + CELL / 2, PREVIEW_Y + 1, label.y() * size + CELL / 2);
      signBlock.setType(Material.SIGN_POST);
      Sign sign = (Sign) signBlock.getState();
      sign.setLine(0, "#" + room.id() + " " + room.type());
      sign.setLine(1, truncate(room.template().getId()));
      sign.setLine(2, room.shape() + " r" + room.rotation());
      sign.setLine(3, "doors: " + room.doors().size());
      sign.update();
    }

    for (Door door : layout.getDoors()) {
      Edge e = door.edge();
      boolean horizontal = e.a().y() == e.b().y(); // cells side by side -> door in the x gap
      int x = horizontal ? e.a().x() * size + CELL : e.a().x() * size + CELL / 2;
      int z = horizontal ? e.a().y() * size + CELL / 2 : e.a().y() * size + CELL;
      Material m = switch (door.type()) {
        case NORMAL -> Material.GOLD_BLOCK;
        case ENTRANCE -> Material.MONSTER_EGGS;
        case WITHER -> Material.COAL_BLOCK;
        case BLOOD -> Material.REDSTONE_BLOCK;
      };
      world.getBlockAt(x, PREVIEW_Y, z).setType(m);
    }
  }

  private static void wool(Block block, byte color) {
    block.setType(Material.WOOL);
    block.setData(color);
  }

  private static String truncate(String s) {
    return s.length() <= 15 ? s : s.substring(0, 15);
  }
}
