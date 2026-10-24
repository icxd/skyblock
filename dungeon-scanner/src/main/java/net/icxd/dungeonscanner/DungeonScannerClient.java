package net.icxd.dungeonscanner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.icxd.dungeonscanner.export.Exporter;
import net.icxd.dungeonscanner.legacy.LegacyMapper;
import net.icxd.dungeonscanner.scan.DungeonScan;
import net.icxd.dungeonscanner.scan.RoomDatabase;
import net.icxd.dungeonscanner.scan.ScannedDungeon;
import net.icxd.dungeonscanner.world.WorldCapture;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Client entrypoint. {@code /dscan} starts scanning the dungeon you're in; with auto mode on
 * ({@code /dscan auto true}, the default) it starts by itself when Mort hands out the map. While a
 * scan is running, every second it re-reads the loaded part of the dungeon and saves every room
 * whose chunks are all loaded, so rooms are captured before anyone walks in and changes them.
 * {@code /dscan status} shows how many of Hypixel's rooms you have. {@code /dscan world start}
 * saves whatever world you walk through as region files ({@link WorldCapture}).
 */
public final class DungeonScannerClient implements ClientModInitializer {
  static final Logger LOG = LoggerFactory.getLogger("dungeonscanner");
  private static final Pattern MORT = Pattern.compile(
      "^\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.$");
  private static final Pattern FLOOR = Pattern.compile("The Catacombs \\((E|F[1-7]|M[1-7])\\)");
  /** Stop scanning a run after this long, rooms would have been touched by then. */
  private static final int MAX_SCAN_TICKS = 20 * 60 * 3;

  private final Path root = FabricLoader.getInstance().getGameDir().resolve("dungeon-scanner");
  private RoomDatabase db;
  private LegacyMapper mapper;
  private boolean auto = true;

  private Exporter exporter;
  private ClientLevel scanning;
  private int ticks;
  private WorldCapture world;

  @Override
  public void onInitializeClient() {
    db = RoomDatabase.load();

    ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
        ClientCommands.literal("dscan")
            .executes(c -> {
              start("manual");
              return 1;
            })
            .then(ClientCommands.literal("stop").executes(c -> {
              stop("stopped");
              return 1;
            }))
            .then(ClientCommands.literal("status").executes(c -> {
              status();
              return 1;
            }))
            .then(ClientCommands.literal("reconvert").executes(c -> {
              reconvert();
              return 1;
            }))
            .then(ClientCommands.literal("auto")
                .then(ClientCommands.argument("enabled", BoolArgumentType.bool()).executes(c -> {
                  auto = BoolArgumentType.getBool(c, "enabled");
                  say("Auto scan " + (auto ? "on" : "off") + ".", ChatFormatting.GRAY);
                  return 1;
                })))
            .then(ClientCommands.literal("world")
                .then(ClientCommands.literal("start")
                    .executes(c -> {
                      startWorld(null);
                      return 1;
                    })
                    .then(ClientCommands.argument("name", StringArgumentType.word()).executes(c -> {
                      startWorld(StringArgumentType.getString(c, "name"));
                      return 1;
                    })))
                .then(ClientCommands.literal("stop").executes(c -> {
                  stopWorld();
                  return 1;
                }))
                .then(ClientCommands.literal("status").executes(c -> {
                  say(world == null ? "Not saving the world." : world.status(), ChatFormatting.GRAY);
                  return 1;
                })))));

    ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
      if (!overlay && auto && MORT.matcher(ChatFormatting.stripFormatting(message.getString())).matches()) {
        start("auto");
      }
    });

    ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
      if (world != null) world.unloading(level, chunk);
    });
    ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> stopWorld());
  }

  private void startWorld(String name) {
    if (world != null) {
      say("Already saving the world to " + world.folder() + ".", ChatFormatting.YELLOW);
      return;
    }
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
    Path folder = root.resolve("worlds").resolve(name == null ? time : time + "_" + name);
    for (int i = 2; Files.exists(folder); i++) folder = folder.resolveSibling(time + "_" + (name == null ? "" : name + "_") + i);
    world = new WorldCapture(folder, text -> say(text, ChatFormatting.GREEN), text -> say(text, ChatFormatting.RED));
    say("Saving the world as it loads; nothing is asked of the server, so walk wherever you want it. "
        + "/dscan world stop to finish.", ChatFormatting.GRAY);
  }

  private void stopWorld() {
    if (world == null) return;
    WorldCapture stopping = world;
    world = null;
    stopping.stop();
    say("Stopped saving the world (" + stopping.folder() + ").", ChatFormatting.GRAY);
  }

  private void start(String why) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;
    if (mapper == null) {
      // Builds the 1.8 id table from Minecraft's data fixers; takes a moment the first time.
      mapper = new LegacyMapper();
    }
    exporter = new Exporter(root, mapper, floor(mc.level));
    scanning = mc.level;
    ticks = 0;
    say("Scanning dungeon (" + why + ")...", ChatFormatting.GRAY);
  }

  private void stop(String why) {
    if (exporter == null) return;
    exporter = null;
    scanning = null;
    say("Scan " + why + ".", ChatFormatting.GRAY);
  }

  private void tick(Minecraft mc) {
    if (world != null) world.tick(mc);
    if (exporter == null) return;
    if (mc.level != scanning) {
      stop("ended (left the dungeon)");
      return;
    }
    if (++ticks % 20 != 0) return;
    if (ticks > MAX_SCAN_TICKS) {
      stop("finished");
      return;
    }
    try {
      LevelView view = new LevelView(mc.level);
      ScannedDungeon dungeon = DungeonScan.scan(view, db);
      Exporter.Result result = exporter.export(view, dungeon);
      for (String id : result.newRooms()) say("Saved new room: " + id, ChatFormatting.GREEN);
      for (String p : result.problems()) say(p, ChatFormatting.YELLOW);
      boolean done = dungeon.complete() && dungeon.rooms().stream().allMatch(ScannedDungeon.Room::complete);
      if (done) stop("finished, every room captured");
    } catch (Exception e) {
      LOG.error("Dungeon scan failed", e);
      say("Scan failed: " + e + " (see log)", ChatFormatting.RED);
      stop("aborted");
    }
  }

  /** Rebuilds all 1.8 schematics from the modern ones (after the 1.8 conversion got better). */
  private void reconvert() {
    try {
      if (mapper == null) mapper = new LegacyMapper();
      var result = net.icxd.dungeonscanner.export.Reconverter.reconvertAll(root, mapper);
      say("Rebuilt " + result.rebuilt() + " .schematic files" + (result.trimmed() > 0
          ? ", cut " + result.trimmed() + " L rooms down to their own blocks (" + result.merged() + " were duplicates)." : "."),
          ChatFormatting.GREEN);
    } catch (Exception e) {
      LOG.error("Reconvert failed", e);
      say("Reconvert failed: " + e, ChatFormatting.RED);
    }
  }

  private void status() {
    try {
      Set<String> saved = Exporter.savedRooms(root);
      long known = db.all().stream().filter(r -> saved.contains(r.id())).count();
      say("Saved " + known + "/" + db.all().size() + " rooms (" + saved.size() + " folders) in " + root, ChatFormatting.GRAY);
      StringBuilder missing = new StringBuilder();
      db.all().stream().filter(r -> !saved.contains(r.id())).limit(25).forEach(r -> missing.append(r.name()).append(", "));
      if (!missing.isEmpty()) say("Missing: " + missing.substring(0, missing.length() - 2), ChatFormatting.DARK_GRAY);
    } catch (Exception e) {
      say("Couldn't read " + root + ": " + e, ChatFormatting.RED);
    }
  }

  /** Floor from the sidebar ("The Catacombs (F7)"), or "unknown". */
  private static String floor(ClientLevel level) {
    for (PlayerTeam team : level.getScoreboard().getPlayerTeams()) {
      String line = ChatFormatting.stripFormatting(team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString());
      if (line == null) continue;
      Matcher m = FLOOR.matcher(line);
      if (m.find()) return m.group(1);
    }
    return "unknown";
  }

  private static void say(String text, ChatFormatting color) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player != null) {
      mc.player.sendSystemMessage(Component.literal("[Dungeon Scanner] ").withStyle(ChatFormatting.DARK_AQUA)
          .append(Component.literal(text).withStyle(color)));
    }
    LOG.info(text);
  }
}
