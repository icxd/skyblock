package net.icxd.dungeons.dungeons.paste;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.registry.WorldData;

import net.icxd.dungeons.dungeons.paste.PastePlan.Block;
import net.icxd.dungeons.dungeons.paste.PastePlan.Box;
import net.icxd.dungeons.dungeons.paste.PastePlan.Carve;
import net.icxd.dungeons.dungeons.paste.PastePlan.Copy;
import net.icxd.dungeons.dungeons.paste.PastePlan.DoorPaste;
import net.icxd.dungeons.dungeons.paste.PastePlan.RoomPaste;

/**
 * Builds a {@link PastePlan} in a world with WorldEdit 6. Only load this class when WorldEdit is
 * installed. Runs on the main thread and takes a few seconds for a full-size dungeon.
 */
public final class WorldEditPaster {
  /** The part of a door schematic that is pasted: all of it up to the top of the frame. */
  private static final Box DOOR_PART = new Box(new Block(0, 0, 0), new Block(4, PastePlan.DOOR_HEIGHT - 1, 2));

  private final World world;
  private final Map<Path, Clipboard> clipboards = new HashMap<>();

  public WorldEditPaster(World world) {
    this.world = world;
  }

  /** @return blocks changed by WorldEdit */
  public int paste(PastePlan plan) throws IOException, WorldEditException {
    BukkitWorld weWorld = new BukkitWorld(world);
    WorldData data = weWorld.getWorldData();
    EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession((com.sk89q.worldedit.world.World) weWorld, -1);
    session.setFastMode(true);
    try {
      for (Box box : plan.clears()) session.setBlocks(new CuboidRegion(vector(box.min()), vector(box.max())), new BaseBlock(0));
      for (RoomPaste room : plan.rooms()) {
        paste(session, data, load(room.capture().schematic(), data), room.turns(), room.center(), room.parts());
      }
      for (DoorPaste door : plan.doors()) {
        paste(session, data, load(door.schematic(), data), door.turns(), door.center(), List.of(DOOR_PART));
      }
    } finally {
      session.flushQueue();
    }

    // Plain block edits on top of the pasted blocks.
    for (Copy copy : plan.closings()) {
      org.bukkit.block.Block from = block(copy.from());
      block(copy.to()).setTypeIdAndData(from.getTypeId(), from.getData(), false);
    }
    for (Carve carve : plan.carves()) {
      for (List<Block> layer : carve.layers()) {
        boolean solid = false;
        for (Block b : layer) {
          org.bukkit.block.Block block = block(b);
          if (block.getType().isOccluding()) {
            block.setType(Material.AIR, false);
            solid = true;
          }
        }
        if (!solid) break;
      }
    }
    return session.getBlockChangeCount();
  }

  /**
   * Pastes {@code parts} (schematic coordinates) of a schematic turned {@code turns} times
   * clockwise around its centre column, with that column at {@code center}.
   */
  private static void paste(EditSession session, WorldData data, Clipboard clipboard, int turns, Block center, List<Box> parts)
      throws WorldEditException {
    Vector min = clipboard.getRegion().getMinimumPoint();
    Vector size = clipboard.getDimensions();
    Vector origin = min.add((size.getBlockX() - 1) / 2, 0, (size.getBlockZ() - 1) / 2);
    // WorldEdit turns counter-clockwise for positive angles (seen from above).
    AffineTransform transform = new AffineTransform().rotateY(-90 * turns);
    Extent source = new BlockTransformExtent(clipboard, transform, data.getBlockRegistry());
    for (Box part : parts) {
      CuboidRegion region = new CuboidRegion(min.add(vector(part.min())), min.add(vector(part.max())));
      ForwardExtentCopy copy = new ForwardExtentCopy(source, region, origin, session, vector(center));
      copy.setTransform(transform);
      Operations.complete(copy);
    }
  }

  private Clipboard load(Path file, WorldData data) throws IOException {
    Clipboard cached = clipboards.get(file);
    if (cached != null) return cached;
    try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
      Clipboard clipboard = ClipboardFormat.SCHEMATIC.getReader(in).read(data);
      clipboards.put(file, clipboard);
      return clipboard;
    }
  }

  private org.bukkit.block.Block block(Block b) {
    return world.getBlockAt(b.x(), b.y(), b.z());
  }

  private static Vector vector(Block b) {
    return new Vector(b.x(), b.y(), b.z());
  }
}
