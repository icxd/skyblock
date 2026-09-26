package net.icxd.dungeons.dungeons.paste;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityTypes;

import org.enginehub.linbus.tree.LinCompoundTag;

import net.icxd.dungeons.dungeons.paste.PastePlan.Block;
import net.icxd.dungeons.dungeons.paste.PastePlan.Box;
import net.icxd.dungeons.dungeons.paste.PastePlan.Copy;
import net.icxd.dungeons.dungeons.paste.PastePlan.DoorPaste;
import net.icxd.dungeons.dungeons.paste.PastePlan.Piece;
import net.icxd.dungeons.dungeons.paste.PastePlan.RoomPaste;

/**
 * Builds a {@link PastePlan} in a world with WorldEdit 7. Only load this class when WorldEdit is
 * installed. The work is spread over ticks (a few seconds for a full-size dungeon) so the server
 * keeps running; the callback gets the result.
 */
public final class WorldEditPaster {
    /** How long each tick may spend pasting. */
    private static final long TICK_BUDGET_NANOS = 25_000_000;
    private static final long MIN_STEP_NANOS = 1_000_000;
    /** Layers per step, so no single step takes long even for the biggest rooms. */
    private static final int SLICE = 4;

    /** Lighting on; no block updates, so sand doesn't fall and water doesn't flow while pasting. */
    private static final SideEffectSet SIDE_EFFECTS = SideEffectSet.defaults()
            .with(SideEffect.NEIGHBORS, SideEffect.State.OFF)
            .with(SideEffect.UPDATE, SideEffect.State.OFF);

    private final World world;
    /** Emptied before pasting instead of the plan's own clears; null for the plan's. */
    private final List<Box> clearFirst;
    private final Map<Path, Clipboard> clipboards = new ConcurrentHashMap<>();

    public WorldEditPaster(World world) {
        this(world, null);
    }

    /**
     * @param clearFirst what to empty before pasting, instead of the spots the plan clears (which
     *     assume the same size of floor was there before): nothing for a new world, or the whole area
     *     any floor can take up when reusing one
     */
    public WorldEditPaster(World world, List<Box> clearFirst) {
        this.world = world;
        this.clearFirst = clearFirst;
    }

    /** @param error why it stopped early, or null */
    public record Result(long millis, Exception error) {
    }

    private interface Step {
        void run(EditSession session) throws IOException, WorldEditException;
    }

    /** Starts pasting; {@code done} runs on the main thread when it's finished or failed. */
    public void paste(Plugin plugin, PastePlan plan, Consumer<Result> done) {
        long start = System.currentTimeMillis();
        Set<Path> files = new LinkedHashSet<>();
        for (RoomPaste room : plan.rooms()) files.add(room.capture().schematic());
        for (DoorPaste door : plan.doors()) for (Piece piece : door.pieces()) files.add(piece.schematic());
        for (Piece piece : plan.fillers()) files.add(piece.schematic());
        // Reading schematics is slow and doesn't touch the world, so it happens off the main thread.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                for (Path file : files) load(file);
            } catch (IOException | RuntimeException e) {
                Bukkit.getScheduler().runTask(plugin, () -> done.accept(new Result(System.currentTimeMillis() - start, e)));
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> pasteLoaded(plugin, plan, done, start));
        });
    }

    private void pasteLoaded(Plugin plugin, PastePlan plan, Consumer<Result> done, long start) {
        // Armor stands and the like from an earlier paste would otherwise be doubled.
        List<Box> areas = new ArrayList<>(List.of(plan.area()));
        if (clearFirst != null) areas.addAll(clearFirst);
        for (Box area : areas) {
            BoundingBox box = new BoundingBox(area.min().x(), area.min().y(), area.min().z(), area.max().x() + 1, area.max().y() + 1, area.max().z() + 1);
            for (Entity entity : world.getNearbyEntities(box)) {
                if (!(entity instanceof Player)) entity.remove();
            }
        }

        Deque<Step> steps = new ArrayDeque<>();
        for (Box clear : clearFirst != null ? clearFirst : plan.clears()) {
            for (Box slice : slices(clear)) {
                // Only blocks that aren't air already; most of a fresh world is.
                CuboidRegion region = new CuboidRegion(vector(slice.min()), vector(slice.max()));
                steps.add(session -> session.replaceBlocks(region, new ExistingBlockMask(session), BlockTypes.AIR.getDefaultState()));
            }
        }
        for (RoomPaste room : plan.rooms()) {
            for (Box part : room.parts()) {
                for (Box slice : slices(part)) {
                    steps.add(session -> paste(session, load(room.capture().schematic()), room.turns(), room.center(), List.of(slice)));
                }
            }
        }
        for (DoorPaste door : plan.doors()) {
            for (Piece piece : door.pieces()) steps.add(session -> paste(session, load(piece.schematic()), piece));
        }
        for (Piece piece : plan.fillers()) steps.add(session -> paste(session, load(piece.schematic()), piece));

        new BukkitRunnable() {
            /**
             * Time for steps this tick. Closing the session (WorldEdit's lighting pass) often takes
             * longer than the steps themselves, so this is tuned to keep whole ticks near the budget.
             */
            long stepBudget = TICK_BUDGET_NANOS;

            @Override
            public void run() {
                long tickStart = System.nanoTime();
                try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(BukkitAdapter.adapt(world)).maxBlocks(-1).build()) {
                    session.setSideEffectApplier(SIDE_EFFECTS);
                    session.setTrackingHistory(false);
                    while (!steps.isEmpty() && System.nanoTime() - tickStart < stepBudget) steps.poll().run(session);
                } catch (IOException | WorldEditException | RuntimeException e) {
                    cancel();
                    done.accept(new Result(System.currentTimeMillis() - start, e));
                    return;
                }
                long spent = Math.max(1, System.nanoTime() - tickStart);
                stepBudget = Math.clamp(stepBudget * TICK_BUDGET_NANOS / spent, MIN_STEP_NANOS, TICK_BUDGET_NANOS);
                if (!steps.isEmpty()) return;
                cancel();
                applyEdits(plan);
                done.accept(new Result(System.currentTimeMillis() - start, null));
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    /** {@code box} cut into {@link #SLICE}-layer boxes, bottom first. */
    private static List<Box> slices(Box box) {
        List<Box> out = new java.util.ArrayList<>();
        for (int y = box.min().y(); y <= box.max().y(); y += SLICE) {
            int top = Math.min(y + SLICE - 1, box.max().y());
            out.add(new Box(new Block(box.min().x(), y, box.min().z()), new Block(box.max().x(), top, box.max().z())));
        }
        return out;
    }

    /** Plain block edits on top of the pasted blocks. */
    private void applyEdits(PastePlan plan) {
        for (Copy copy : plan.closings()) {
            block(copy.to()).setBlockData(block(copy.from()).getBlockData(), false);
        }
    }

    /**
     * Pastes {@code parts} (schematic coordinates) of a schematic turned {@code turns} times
     * clockwise around its centre column, with that column at {@code center}. Entities in those
     * parts come along.
     */
    private static void paste(EditSession session, Clipboard clipboard, int turns, Block center, List<Box> parts)
            throws WorldEditException {
        BlockVector3 min = clipboard.getRegion().getMinimumPoint();
        BlockVector3 size = clipboard.getDimensions();
        BlockVector3 origin = min.add((size.x() - 1) / 2, 0, (size.z() - 1) / 2);
        // WorldEdit turns counter-clockwise for positive angles (seen from above).
        AffineTransform transform = new AffineTransform().rotateY(-90 * turns);
        Extent source = new BlockTransformExtent(clipboard, transform);
        for (Box part : parts) {
            CuboidRegion region = new CuboidRegion(min.add(vector(part.min())), min.add(vector(part.max())));
            ForwardExtentCopy copy = new ForwardExtentCopy(source, region, origin, new KeepStill(session), vector(center));
            copy.setTransform(transform);
            copy.setCopyingBiomes(false);
            Operations.complete(copy);
        }
    }

    /** Pastes a piece of a schematic. Its entities stay behind; the room they're in brought its own. */
    private static void paste(EditSession session, Clipboard clipboard, Piece piece) throws WorldEditException {
        BlockVector3 min = clipboard.getRegion().getMinimumPoint();
        AffineTransform transform = new AffineTransform().rotateY(-90 * piece.turns());
        CuboidRegion region = new CuboidRegion(min.add(vector(piece.box().min())), min.add(vector(piece.box().max())));
        ForwardExtentCopy copy = new ForwardExtentCopy(new BlockTransformExtent(clipboard, transform), region,
                min.add(vector(piece.from())), new KeepStill(session), vector(piece.to()));
        copy.setTransform(transform);
        copy.setCopyingBiomes(false);
        copy.setCopyingEntities(false);
        Operations.complete(copy);
    }

    /**
     * Keeps what doesn't move on Hypixel from moving here. Half the captured leaves aren't
     * persistent (Hypixel doesn't random-tick dungeons; a normal server would make them decay), and
     * the decoration armor stands have gravity (on Hypixel they're only sent to clients, so they
     * hover; here they'd fall).
     */
    private static final class KeepStill extends AbstractDelegateExtent {
        KeepStill(Extent extent) {
            super(extent);
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) throws WorldEditException {
            @SuppressWarnings("unchecked")
            Property<Boolean> persistent = (Property<Boolean>) block.getBlockType().getPropertyMap().get("persistent");
            return super.setBlock(position, persistent == null ? block : block.with(persistent, true));
        }

        @Override
        public com.sk89q.worldedit.entity.Entity createEntity(Location location, BaseEntity entity) {
            if (entity.getType() == EntityTypes.ARMOR_STAND && entity.getNbtReference() != null) {
                LinCompoundTag nbt = entity.getNbtReference().getValue().toBuilder().putByte("NoGravity", (byte) 1).build();
                entity = new BaseEntity(entity.getType(), LazyReference.computed(nbt));
            }
            return super.createEntity(location, entity);
        }
    }

    private Clipboard load(Path file) throws IOException {
        Clipboard cached = clipboards.get(file);
        if (cached != null) return cached;
        ClipboardFormat format = ClipboardFormats.findByPath(file);
        if (format == null) throw new IOException("Not a schematic WorldEdit can read: " + file);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file)); ClipboardReader reader = format.getReader(in)) {
            Clipboard clipboard = reader.read();
            clipboards.put(file, clipboard);
            return clipboard;
        }
    }

    private org.bukkit.block.Block block(Block b) {
        return world.getBlockAt(b.x(), b.y(), b.z());
    }

    private static BlockVector3 vector(Block b) {
        return BlockVector3.at(b.x(), b.y(), b.z());
    }
}
