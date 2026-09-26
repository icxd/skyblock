package net.icxd.dungeons.command.commands.admin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.icxd.dungeons.command.CommandParameters;
import net.icxd.dungeons.command.CommandSource;
import net.icxd.dungeons.command.SCommand;
import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.DungeonConfig;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.dungeons.generation.DungeonGenerator;
import net.icxd.dungeons.dungeons.generation.DungeonLayout;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.LayoutValidator;
import net.icxd.dungeons.dungeons.generation.room.HypixelRooms;
import net.icxd.dungeons.dungeons.generation.utils.Edge;
import net.icxd.dungeons.dungeons.generation.utils.Position;
import net.icxd.dungeons.dungeons.paste.PastePlan;
import net.icxd.dungeons.dungeons.paste.RoomLibrary;
import net.icxd.dungeons.dungeons.paste.WorldEditPaster;
import net.icxd.dungeons.common.Rank;

/**
 * {@code /dungeon [floor] [seed]}: generates a layout, prints it to the console and builds a
 * block preview at y=100 around 0,0. Floor is e.g. {@code E}, {@code F7}, {@code M3}.
 *
 * <p>{@code /dungeon paste [floor] [seed]}: generates a layout from the captured rooms in
 * {@code plugins/<plugin>/dungeon-rooms} (the scanner's {@code rooms/} folder) and pastes it
 * with WorldEdit where Hypixel has it, from -200,-200, in your world (the main world from the
 * console).
 */
@CommandParameters(aliases = "dungeon", permission = Rank.STAFF)
public class DungeonCommand extends SCommand {

  /** Blocks per grid cell in the preview. */
  private static final int CELL = 5;
  private static final int PREVIEW_Y = 100;
  private static final String ROOM_FOLDER = "dungeon-rooms";

  @Override
  public void run(CommandSource source, String[] args) {
    if (args.length > 0 && args[0].equalsIgnoreCase("paste")) {
      paste(source, Arrays.copyOfRange(args, 1, args.length));
      return;
    }
    DungeonFloor floor = floor(source, args);
    Long seed = seed(source, args);
    if (floor == null || seed == null) return;

    DungeonLayout layout = new DungeonGenerator(DungeonConfig.forFloor(floor), HypixelRooms.pool()).generate(seed);
    List<String> problems = LayoutValidator.validate(layout);
    log(floor, seed, layout, problems);

    renderPreview(layout, source.getPlayer().getWorld());
    source.send(ChatColor.GREEN + floor.getName() + " seed " + seed + ": " + layout.getRooms().size() + " rooms"
        + (problems.isEmpty() ? "" : ChatColor.RED + " (" + problems.size() + " problems, see console)"));
    source.send(ChatColor.GRAY + "Preview at y=" + PREVIEW_Y + ": lime = critical path, gold = door, "
        + "infested stone = entrance door, coal = wither door, pink wool = fairy door, redstone = blood door. "
        + "Map printed to the console.");
  }

  private void paste(CommandSource source, String[] args) {
    // From the console it goes into the main world.
    Player player = source.getPlayer();
    World world = player != null ? player.getWorld() : Bukkit.getWorlds().get(0);
    if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
      source.send(ChatColor.RED + "Pasting needs WorldEdit 7.");
      return;
    }
    DungeonFloor floor = floor(source, args);
    Long seed = seed(source, args);
    if (floor == null || seed == null) return;

    Logger log = instance.getLogger();
    File folder = new File(instance.getDataFolder(), ROOM_FOLDER);
    RoomLibrary library;
    try {
      library = RoomLibrary.load(folder.toPath());
    } catch (IOException e) {
      source.send(ChatColor.RED + "Couldn't read rooms from " + folder + ": " + e.getMessage());
      source.send(ChatColor.GRAY + "Copy the scanner's rooms/ folder there.");
      return;
    }
    library.problems().forEach(p -> log.warning("Room library: " + p));
    if (library.templates().isEmpty()) {
      source.send(ChatColor.RED + "No rooms in " + folder + ". Copy the scanner's rooms/ folder there.");
      return;
    }

    DungeonLayout layout;
    try {
      layout = new DungeonGenerator(DungeonConfig.forFloor(floor), library.pool()).generate(seed);
    } catch (IllegalStateException e) {
      source.send(ChatColor.RED + "Not enough captured rooms for " + floor.getName() + ": " + e.getMessage());
      return;
    }
    List<String> problems = LayoutValidator.validate(layout);
    log(floor, seed, layout, problems);

    PastePlan plan = PastePlan.create(layout, library, PastePlan.HYPIXEL_BASE, PastePlan.HYPIXEL_BASE, seed);
    plan.problems().forEach(p -> log.warning("Paste: " + p));
    int issues = problems.size() + plan.problems().size() + library.problems().size();
    source.send(ChatColor.GRAY + "Pasting " + floor.getName() + " seed " + seed + " (" + plan.rooms().size() + " rooms)...");
    Integer randomTicks = world.getGameRuleValue(GameRules.RANDOM_TICK_SPEED);
    if (randomTicks != null && randomTicks > 0) {
      // Hypixel's dungeons don't random-tick; here the ice in rooms like Ice Path melts and floods them.
      source.send(ChatColor.YELLOW + "This world random-ticks blocks, so ice in the rooms will melt. "
          + "Hypixel's dungeons don't: use a world with /gamerule random_tick_speed 0.");
    }
    new WorldEditPaster(world).paste(instance, plan, result -> {
      if (result.error() != null) {
        log.log(Level.SEVERE, "Paste failed", result.error());
        source.send(ChatColor.RED + "Paste failed: " + result.error().getMessage());
        return;
      }
      String summary = floor.getName() + " seed " + seed + ": pasted " + plan.rooms().size() + " rooms and " + plan.doors().size()
          + " doors in " + result.millis() + " ms, from " + library.summary() + ".";
      log.info(summary);
      if (player != null && player.isOnline()) {
        PastePlan.Block entrance = plan.entrance();
        player.teleport(RunManager.standingSpot(world, entrance.x(), entrance.y(), entrance.z()));
      }
      source.send(ChatColor.GREEN + summary);
      if (issues > 0) source.send(ChatColor.RED + "" + issues + " problems, see console.");
    });
  }

  private static DungeonFloor floor(CommandSource source, String[] args) {
    DungeonFloor floor = args.length > 0 ? DungeonFloor.parse(args[0]) : DungeonFloor.FLOOR_7;
    if (floor == null) source.send(ChatColor.RED + "Unknown floor " + args[0] + ", use E, F1-F7 or M1-M7.");
    return floor;
  }

  private static Long seed(CommandSource source, String[] args) {
    try {
      return args.length > 1 ? Long.parseLong(args[1]) : new java.util.Random().nextLong();
    } catch (NumberFormatException e) {
      source.send(ChatColor.RED + "Seed must be a number.");
      return null;
    }
  }

  private static void log(DungeonFloor floor, long seed, DungeonLayout layout, List<String> problems) {
    Logger log = instance.getLogger();
    log.info("Dungeon " + floor.getName() + " seed " + seed + " (" + layout.getAttempts() + " attempts)");
    for (String line : layout.render().split("\n")) log.info(line);
    for (PlacedRoom r : layout.getRooms()) {
      log.info(String.format("  #%d %-8s %-12s %-16s rot=%d parent=%d doors=%d cells=%s",
          r.id(), r.type(), r.shape(), r.template().getId(), r.rotation(), r.parent(), r.doors().size(), r.cells()));
    }
    problems.forEach(p -> log.warning("INVALID: " + p));
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
      signBlock.setType(Material.OAK_SIGN);
      Sign sign = (Sign) signBlock.getState();
      SignSide front = sign.getSide(Side.FRONT);
      front.line(0, Component.text("#" + room.id() + " " + room.type()));
      front.line(1, Component.text(truncate(room.template().getId())));
      front.line(2, Component.text(room.shape() + " r" + room.rotation()));
      front.line(3, Component.text("doors: " + room.doors().size()));
      sign.update();
    }

    for (Door door : layout.getDoors()) {
      Edge e = door.edge();
      boolean horizontal = e.a().y() == e.b().y(); // cells side by side -> door in the x gap
      int x = horizontal ? e.a().x() * size + CELL : e.a().x() * size + CELL / 2;
      int z = horizontal ? e.a().y() * size + CELL / 2 : e.a().y() * size + CELL;
      Block block = world.getBlockAt(x, PREVIEW_Y, z);
      switch (door.type()) {
        case NORMAL -> block.setType(Material.GOLD_BLOCK);
        case ENTRANCE -> block.setType(Material.INFESTED_STONE);
        case WITHER -> block.setType(Material.COAL_BLOCK);
        case FAIRY -> wool(block, (byte) 6);
        case BLOOD -> block.setType(Material.REDSTONE_BLOCK);
      }
    }
  }

  /** Wool in the 1.8 colour order (0 white, 5 lime, 6 pink, ...). */
  private static final Material[] WOOL = {
      Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL, Material.LIGHT_BLUE_WOOL,
      Material.YELLOW_WOOL, Material.LIME_WOOL, Material.PINK_WOOL, Material.GRAY_WOOL,
      Material.LIGHT_GRAY_WOOL, Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
      Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL, Material.BLACK_WOOL,
  };

  private static void wool(Block block, byte color) {
    block.setType(WOOL[color & 15]);
  }

  private static String truncate(String s) {
    return s.length() <= 15 ? s : s.substring(0, 15);
  }
}
