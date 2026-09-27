package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.dungeons.paste.PastePlan;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.mob.DataMob;
import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.MobKinds;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.Modifier;
import net.icxd.dungeons.mob.SpawnOptions;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * A run's room mobs, and what comes of them (research mobs.md 1-2, 6-7; critic.md 3.3):
 * <ul>
 *   <li>Every normal and champion room has its mobs from the start (the recorded ones where the room was
 *       recorded, {@link FallbackSpawns} otherwise), waiting ({@link DataMob#setDormant}) at their base
 *       health until the room opens: its door is opened, or someone walks in (or hits one of them). Then
 *       they wake up with the room's health and damage multiplier ({@link #multiplier}).</li>
 *   <li>The room is cleared when its starred mobs are dead: its tick on the map, and where the last one
 *       died its loot ({@link RoomLoot}): a blessing, the next door's key, a Superboom TNT or a Revive
 *       Stone, picked up by walking into them.</li>
 *   <li>Skeleton skulls (the captures', else the recorded ones, else planned) rise and become Undead
 *       Skeletons some seconds after their room opens, and every skeleton killed leaves one that does the
 *       same (mobs.md 1.8).</li>
 *   <li>Crypts and weak walls where their blocks are known, blown up with Superboom TNT: the tomb's
 *       Crypt Undead comes out, and the crypt counts for the score ({@link #cryptsBlown}).</li>
 * </ul>
 * Main thread; {@link DungeonRun} ticks it.
 */
final class RoomMobs {
    /** The first room opened gets x1.05 (both recordings), every later one 1 + 0.05 x (squares opened before - 1). */
    static final double FIRST_ROOM = 1.05;
    static final double PER_SQUARE = 0.05;
    /** Mobs spawned a tick, so a floor's hundreds don't hold up one tick. */
    private static final int SPAWNS_PER_TICK = 40;

    /**
     * When a skull rises after its room opens: 6.3 to 28.4 s after, one by one, in the recordings (what sets
     * it off is UNKNOWN: players were 1.5 to 56 blocks away), so at random in that span.
     */
    static final double SKULL_RISE_MIN = 6.3;
    static final double SKULL_RISE_MAX = 28.4;
    /** A killed skeleton's skull rises 14.6 to 23 s later (recorded). */
    static final double SKULL_AGAIN_MIN = 14.6;
    static final double SKULL_AGAIN_MAX = 23;
    /** It rises 0.21 blocks and turns 43 degrees every 3 ticks, 12 times: 2.5 blocks in 1.8 s (recorded 1.7 s, 2.5 blocks). */
    static final int RISE_STEPS = 12;
    static final int RISE_EVERY = 3;
    static final double RISE_STEP = 0.21;
    static final float RISE_TURN = 43;
    /** The Undead Skeleton appears this far above where the skull started, and drops to the floor. */
    static final double UNDEAD_ABOVE = 2.72;
    /** A killed skeleton's skull lies this far below where it died (its stand; the skull itself on the floor). */
    static final double DEAD_SKULL_BELOW = 1.44;
    /** A lightning bolt came down on the skull 0.3 to 1.4 s before 7 of the 44 recorded rises. */
    static final double LIGHTNING_CHANCE = 7 / 44.0;
    static final int LIGHTNING_BEFORE = 16;

    /** The loot's heads lie on stands this far below the drop spot, its names this far, the Superboom's paper this far. */
    private static final double HEAD_BELOW = 0.72;
    private static final double NAME_BELOW = 0.47;
    private static final double HAND_BELOW = 0.5;
    private static final double PICKUP_REACH = 1.5;

    /**
     * How close to a tomb or weak wall a Superboom TNT has to go off: UNKNOWN (every recorded one went off right
     * against it, where the block clicked was part of it), so within 2 blocks.
     */
    static final int BLAST_REACH = 2;
    /** About half the blown blocks puffed squid ink (58 of 127 recorded). */
    private static final double INK_CHANCE = 0.45;

    private static final Map<DataMob, Tracked> TRACKED = new IdentityHashMap<>();
    private static final Set<UUID> STANDS = new HashSet<>();
    private static volatile RoomSpawnData.Loaded data = RoomSpawnData.Loaded.NONE;

    /** The rooms' data, loaded when the server starts (see {@link RunManager}). */
    static void setData(RoomSpawnData.Loaded loaded) {
        data = loaded == null ? RoomSpawnData.Loaded.NONE : loaded;
    }

    /** A spawned mob of one of the rooms. */
    static final class Tracked {
        final RoomMobs owner;
        final RoomState room;
        final Mobs.Live live;
        final boolean starred;
        boolean dead;

        Tracked(RoomMobs owner, RoomState room, Mobs.Live live, boolean starred) {
            this.owner = owner;
            this.room = room;
            this.live = live;
            this.starred = starred;
        }

        DataMob mob() {
            return (DataMob) live.type();
        }
    }

    /** A mob waiting to be spawned. */
    private record Planned(RoomState room, MobKind kind, SpawnOptions options, Location at) {
    }

    /** One room of the run. */
    static final class RoomState {
        final PlacedRoom room;
        final RoomFrame frame;
        /** A normal or champion room: one with mobs to clear. */
        final boolean hasMobs;
        final List<Tracked> mobs = new ArrayList<>();
        /** The doors whose keys this room drops. */
        final List<Door> keys = new ArrayList<>();
        int starredPlanned;
        int starredDead;
        boolean opened;
        boolean cleared;
        double multiplier = 1;

        RoomState(PlacedRoom room, RoomFrame frame) {
            this.room = room;
            this.frame = frame;
            this.hasMobs = hasMobs(room.type());
        }
    }

    /** A skeleton skull that becomes an Undead Skeleton. */
    private static final class Skull {
        final RoomState room;
        final ArmorStand stand;
        final Location start;
        /** The tick it starts rising at; -1 until its room opens. */
        long riseAt = -1;
        int step;
        boolean lightning;

        Skull(RoomState room, ArmorStand stand) {
            this.room = room;
            this.stand = stand;
            this.start = stand.getLocation();
        }
    }

    /** A piece of a cleared room's loot, waiting to be picked up (keys are {@link RunDoors}'). */
    private static final class Loot {
        final RoomLoot.Drop drop;
        final Location at;
        final List<ArmorStand> stands;
        /** Told their inventory is full, while they stand on it. */
        final Set<UUID> told = new HashSet<>();

        Loot(RoomLoot.Drop drop, Location at, List<ArmorStand> stands) {
            this.drop = drop;
            this.at = at;
            this.stands = stands;
        }
    }

    /** A tomb or a weak wall. */
    private static final class Blast {
        final RoomState room;
        final List<PastePlan.Block> blocks;
        final Map<PastePlan.Block, BlockData> after;
        /** A tomb's Crypt Undead comes out here; null for a wall. */
        final Location undead;
        boolean blown;

        Blast(RoomState room, List<PastePlan.Block> blocks, Map<PastePlan.Block, BlockData> after, Location undead) {
            this.room = room;
            this.blocks = blocks;
            this.after = after;
            this.undead = undead;
        }

        boolean near(Block at) {
            for (PastePlan.Block b : blocks) {
                if (Math.max(Math.abs(b.x() - at.getX()), Math.max(Math.abs(b.y() - at.getY()), Math.abs(b.z() - at.getZ()))) <= BLAST_REACH) {
                    return true;
                }
            }
            return false;
        }
    }

    private final DungeonRun run;
    private final World world;
    private final RunLayout layout;
    private final RunDoors doors;
    private final DungeonFloor floor;
    private final Logger log;
    private final RandomGenerator random = ThreadLocalRandom.current();
    private final Map<Integer, RoomState> rooms = new LinkedHashMap<>();
    private final Deque<Planned> toSpawn = new ArrayDeque<>();
    private final List<Skull> skulls = new ArrayList<>();
    private final List<Loot> loot = new ArrayList<>();
    private final List<Blast> crypts = new ArrayList<>();
    private final List<Blast> walls = new ArrayList<>();
    private int openedSquares;
    private int cryptsBlown;
    private long ticks;
    private boolean disposed;

    RoomMobs(DungeonRun run, World world, RunLayout layout, RunDoors doors, DungeonFloor floor, Logger log) {
        this.run = run;
        this.world = world;
        this.layout = layout;
        this.doors = doors;
        this.floor = floor;
        this.log = log;
        RoomSpawnData.Loaded loaded = data;
        List<Planned> planned = new ArrayList<>();
        for (PlacedRoom room : layout.rooms()) {
            RoomState state = new RoomState(room, layout.frame(room));
            rooms.put(room.id(), state);
            if (state.frame == null) continue;
            var capture = layout.capture(room);
            RoomSpawnData.Room roomData = loaded.room(capture == null ? null : capture.id());
            if (state.hasMobs) plan(state, roomData, planned);
            if (roomData != null) blasts(state, roomData);
        }
        planned.sort(Comparator.comparingInt(p -> p.room().room.depth()));
        toSpawn.addAll(planned);
        // A key drops with its room's loot; a key room with nothing starred has it waiting from the start.
        for (Door door : doors.keyDoors()) {
            RoomState keyRoom = rooms.get(layout.keyRoom(door).id());
            if (keyRoom != null && keyRoom.hasMobs && keyRoom.starredPlanned > 0) keyRoom.keys.add(door);
            else doors.placeKey(door);
        }
    }

    /** Normal (and rare, a normal room's dead-end kind) and champion rooms have mobs; puzzles, traps and the rest none. */
    static boolean hasMobs(RoomType type) {
        return type == RoomType.REGULAR || type == RoomType.RARE || type == RoomType.MINIBOSS;
    }

    /** A newly opened room's health and damage multiplier, given how many squares were opened before it (the Entrance's not counted). */
    static double multiplier(int squaresBefore) {
        return squaresBefore <= 0 ? FIRST_ROOM : 1 + PER_SQUARE * (squaresBefore - 1);
    }

    // Planning

    private void plan(RoomState state, RoomSpawnData.Room roomData, List<Planned> out) {
        List<Planned> mine = new ArrayList<>();
        if (roomData != null && roomData.recorded()) {
            for (RoomSpawnData.Mob m : roomData.mobs()) {
                MobKind kind = MobKinds.get(m.kind());
                if (kind == null) continue;
                double[] p = state.frame.point(m.x(), m.y(), m.z());
                mine.add(new Planned(state, kind, options(kind, m.starred(), m.level()), at(p)));
            }
            skulls(state, roomData.skulls());
        } else {
            fallback(state, mine);
        }
        for (Planned p : mine) {
            if (p.options().starred()) state.starredPlanned++;
        }
        out.addAll(mine);
    }

    /**
     * Starred or not as recorded; a modifier of its own each run (the recorded ones were one sample of
     * Hypixel's chance, see {@link Modifier#roll}), never on a miniboss.
     */
    private SpawnOptions options(MobKind kind, boolean starred, Integer level) {
        boolean miniboss = kind.style() == MobKind.NameStyle.MINIBOSS;
        return new SpawnOptions(starred, miniboss ? null : Modifier.roll(random), 1, level);
    }

    private Location at(double[] p) {
        return new Location(world, p[0], p[1], p[2], random.nextFloat() * 360, 0);
    }

    /** The room's skulls: the capture's own stands if it has any, else the recorded spots. */
    private void skulls(RoomState state, List<RoomSpawnData.Point> recorded) {
        if (!captureSkulls(state).isEmpty()) return;
        for (RoomSpawnData.Point p : recorded) {
            double[] w = state.frame.point(p.x(), p.y(), p.z());
            skulls.add(new Skull(state, skullStand(new Location(world, w[0], w[1], w[2], random.nextFloat() * 360, 0))));
        }
    }

    /** The skeleton skull stands the room's capture came with, now skulls that rise. */
    private List<Skull> captureSkulls(RoomState state) {
        List<Skull> out = new ArrayList<>();
        for (int[] min : layout.cellMins(state.room)) {
            BoundingBox box = new BoundingBox(min[0], world.getMinHeight(), min[1], min[0] + PastePlan.CELL, world.getMaxHeight(),
                    min[1] + PastePlan.CELL);
            for (Entity e : world.getNearbyEntities(box, e -> e instanceof ArmorStand)) {
                ArmorStand stand = (ArmorStand) e;
                ItemStack helmet = stand.getEquipment().getHelmet();
                if (helmet == null || helmet.getType() != Material.SKELETON_SKULL) continue;
                STANDS.add(stand.getUniqueId());
                Skull skull = new Skull(state, stand);
                out.add(skull);
                skulls.add(skull);
            }
        }
        return out;
    }

    /** A room nobody recorded: {@link FallbackSpawns}' mobs and skulls (unless the capture has skulls). */
    private void fallback(RoomState state, List<Planned> out) {
        FallbackSpawns.Blocks blocks = new FallbackSpawns.Blocks() {
            @Override
            public boolean solid(int x, int y, int z) {
                return world.getBlockAt(x, y, z).getType().isSolid();
            }

            @Override
            public boolean passable(int x, int y, int z) {
                return world.getBlockAt(x, y, z).isPassable();
            }
        };
        List<int[]> doorBlocks = new ArrayList<>();
        for (Door door : state.room.doors()) doorBlocks.addAll(layout.doorBlocks(door));
        List<int[]> columns = FallbackSpawns.columns(layout.cellMins(state.room), PastePlan.CELL, doorBlocks);
        List<FallbackSpawns.Spot> floorSpots = FallbackSpawns.spots(blocks, columns, FallbackSpawns.FLOOR_MIN_Y, FallbackSpawns.FLOOR_MAX_Y);
        int squares = state.room.cells().size();
        if (state.room.type() == RoomType.MINIBOSS) {
            var capture = layout.capture(state.room);
            int low = capture == null ? FallbackSpawns.FLOOR_MIN_Y : capture.originY() + 1;
            int high = capture == null ? FallbackSpawns.FLOOR_MAX_Y : capture.topY() - 2;
            List<FallbackSpawns.Spot> anywhere = FallbackSpawns.spots(blocks, columns, low, high);
            Location middle = layout.center(world, RunLayout.firstCell(state.room));
            FallbackSpawns.Spot spot = FallbackSpawns.nearest(anywhere, middle.getX(), middle.getY(), middle.getZ());
            if (spot != null) {
                MobKind kind = FallbackSpawns.miniboss(random);
                out.add(new Planned(state, kind, new SpawnOptions(true, null, 1, FallbackSpawns.MINIBOSS_LEVEL), spotLocation(spot)));
            }
        } else {
            for (FallbackSpawns.Spot spot : FallbackSpawns.groups(floorSpots, FallbackSpawns.starredCount(squares, random), random)) {
                MobKind kind = FallbackSpawns.kind(random);
                out.add(new Planned(state, kind, options(kind, true, null), spotLocation(spot)));
            }
        }
        if (captureSkulls(state).isEmpty()) {
            int count = FallbackSpawns.skullCount(squares, random);
            for (int i = 0; i < count && !floorSpots.isEmpty(); i++) {
                FallbackSpawns.Spot spot = FallbackSpawns.any(floorSpots, random);
                skulls.add(new Skull(state, skullStand(spotLocation(spot))));
            }
        }
    }

    private Location spotLocation(FallbackSpawns.Spot spot) {
        return new Location(world, spot.x() + 0.5, spot.y(), spot.z() + 0.5, random.nextFloat() * 360, 0);
    }

    /** The room's crypts and weak walls, in the world. */
    private void blasts(RoomState state, RoomSpawnData.Room roomData) {
        for (RoomSpawnData.Crypt c : roomData.crypts()) {
            double[] p = state.frame.point(c.undead().x(), c.undead().y(), c.undead().z());
            crypts.add(new Blast(state, world(state.frame, c.blocks()), Map.of(), new Location(world, p[0], p[1], p[2])));
        }
        for (RoomSpawnData.Wall w : roomData.walls()) {
            Map<PastePlan.Block, BlockData> after = new LinkedHashMap<>();
            for (Map.Entry<PastePlan.Block, String> a : w.after().entrySet()) {
                try {
                    BlockData block = Bukkit.createBlockData(a.getValue());
                    block.rotate(state.frame.rotation());
                    PastePlan.Block b = a.getKey();
                    after.put(state.frame.block(b.x(), b.y(), b.z()), block);
                } catch (IllegalArgumentException e) {
                    log.warning("Room " + state.room.template().getId() + ": weak wall block " + a.getValue() + ": " + e.getMessage());
                }
            }
            walls.add(new Blast(state, world(state.frame, w.blocks()), after, null));
        }
    }

    private static List<PastePlan.Block> world(RoomFrame frame, List<PastePlan.Block> blocks) {
        List<PastePlan.Block> out = new ArrayList<>();
        for (PastePlan.Block b : blocks) out.add(frame.block(b.x(), b.y(), b.z()));
        return out;
    }

    // Stands

    private ArmorStand skullStand(Location at) {
        ArmorStand stand = stand(at);
        stand.getEquipment().setHelmet(new ItemStack(Material.SKELETON_SKULL));
        stand.setMarker(true);
        return stand;
    }

    private ArmorStand stand(Location at) {
        ArmorStand stand = world.spawn(at, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setInvulnerable(true);
            s.setPersistent(false);
            s.setBasePlate(false);
        });
        STANDS.add(stand.getUniqueId());
        return stand;
    }

    /** One of the stands the rooms put up (skulls, loot), which players can't take anything off. */
    static boolean isStand(Entity entity) {
        return STANDS.contains(entity.getUniqueId());
    }

    // Opening

    /** A room opened (its door, or someone walked in): its mobs wake up with its multiplier, and its skulls start to rise. */
    void open(PlacedRoom room) {
        RoomState state = rooms.get(room.id());
        if (state == null || state.opened) return;
        state.opened = true;
        state.multiplier = multiplier(openedSquares);
        if (room.type() != RoomType.START) openedSquares += room.cells().size();
        for (Tracked t : state.mobs) wake(t);
        for (Skull skull : skulls) {
            if (skull.room == state && skull.riseAt < 0) rise(skull, SKULL_RISE_MIN, SKULL_RISE_MAX);
        }
        // Nothing starred in it (none planned, or none could stand anywhere): done as soon as it's open.
        if (state.hasMobs && state.starredPlanned == 0 && !state.cleared) clear(state, null);
    }

    private void wake(Tracked t) {
        if (t.dead || !t.live.entity().isValid()) return;
        Mobs.setRoomMultiplier(t.live, t.room.multiplier);
        t.mob().setDormant(t.live.entity(), false);
    }

    private void rise(Skull skull, double minSeconds, double maxSeconds) {
        skull.riseAt = ticks + Math.round(random.nextDouble(minSeconds, maxSeconds) * 20);
        skull.lightning = random.nextDouble() < LIGHTNING_CHANCE;
    }

    /** Whether its starred mobs are all dead (a room with no mobs to clear never is, here). */
    boolean isCleared(PlacedRoom room) {
        RoomState state = rooms.get(room.id());
        return state != null && state.cleared;
    }

    int cryptsBlown() {
        return cryptsBlown;
    }

    // Every tick

    void tick(List<Player> players) {
        if (disposed) return;
        ticks++;
        spawnSome();
        tickSkulls();
        tickLoot(players);
        if (ticks % 20 == 0) sweep();
    }

    /** Only the spawning, while the run hasn't started. */
    void tickWaiting() {
        if (disposed) return;
        spawnSome();
    }

    private void spawnSome() {
        for (int i = 0; i < SPAWNS_PER_TICK && !toSpawn.isEmpty(); i++) spawn(toSpawn.poll());
    }

    private void spawn(Planned p) {
        Mobs.Live live;
        try {
            live = Mobs.spawn(p.kind(), floor, p.options(), p.at());
        } catch (IllegalArgumentException e) {
            log.warning("Room " + p.room().room.template().getId() + ": " + e.getMessage());
            if (p.options().starred()) p.room().starredPlanned--;
            return;
        }
        Tracked t = track(p.room(), live, p.options().starred());
        if (p.room().opened) wake(t);
        else t.mob().setDormant(live.entity(), true);
    }

    private Tracked track(RoomState room, Mobs.Live live, boolean starred) {
        Tracked t = new Tracked(this, room, live, starred);
        room.mobs.add(t);
        TRACKED.put((DataMob) live.type(), t);
        return t;
    }

    /** A mob that spawns awake in an open room (an Undead Skeleton, a Crypt Undead): never starred, never room-scaled. */
    private void spawnAwake(RoomState room, MobKind kind, Location at) {
        try {
            track(room, Mobs.spawn(kind, floor, SpawnOptions.NONE, at), false);
        } catch (IllegalArgumentException e) {
            log.warning(e.getMessage());
        }
    }

    private void tickSkulls() {
        for (Skull skull : List.copyOf(skulls)) {
            if (skull.riseAt < 0) continue;
            if (!skull.stand.isValid()) {
                skulls.remove(skull);
                continue;
            }
            if (skull.lightning && ticks == skull.riseAt - LIGHTNING_BEFORE) world.strikeLightningEffect(skull.start);
            if (ticks < skull.riseAt || (ticks - skull.riseAt) % RISE_EVERY != 0) continue;
            skull.step++;
            Location to = skull.start.clone().add(0, skull.step * RISE_STEP, 0);
            to.setYaw(skull.start.getYaw() + skull.step * RISE_TURN);
            skull.stand.teleport(to);
            if (skull.step < RISE_STEPS) continue;
            skulls.remove(skull);
            skull.stand.remove();
            STANDS.remove(skull.stand.getUniqueId());
            spawnAwake(skull.room, MobKinds.UNDEAD_SKELETON, skull.start.clone().add(0, UNDEAD_ABOVE, 0));
        }
    }

    /** Starred mobs that went without dying (fell out of the world, say) count as dead where they were last. */
    private void sweep() {
        for (RoomState state : rooms.values()) {
            if (state.cleared) continue;
            for (Tracked t : List.copyOf(state.mobs)) {
                if (!t.dead && !t.live.entity().isValid() && Mobs.of(t.live.entity()) == null) died(t, t.live.entity().getLocation());
            }
        }
    }

    // Deaths and clearing

    /** A mob of one of the rooms is dead. */
    static Tracked tracked(DataMob mob) {
        return mob == null ? null : TRACKED.get(mob);
    }

    void died(Tracked t, Location at) {
        if (t.dead) return;
        t.dead = true;
        TRACKED.remove(t.mob());
        RoomState state = t.room;
        if (skeletal(t.mob().kind()) && !disposed) {
            Skull skull = new Skull(state, skullStand(new Location(world, at.getX(), at.getY() - DEAD_SKULL_BELOW, at.getZ(),
                    random.nextFloat() * 360, 0)));
            skulls.add(skull);
            rise(skull, SKULL_AGAIN_MIN, SKULL_AGAIN_MAX);
        }
        if (!t.starred) return;
        state.starredDead++;
        if (!state.cleared && state.starredDead >= state.starredPlanned && toSpawnFor(state) == 0) clear(state, at);
    }

    /** Skeletons (and Undead Skeletons) leave a skull that becomes an Undead Skeleton. */
    static boolean skeletal(MobKind kind) {
        return kind == MobKinds.SKELETON_GRUNT || kind == MobKinds.SCARED_SKELETON || kind == MobKinds.UNDEAD_SKELETON;
    }

    private int toSpawnFor(RoomState state) {
        int n = 0;
        for (Planned p : toSpawn) {
            if (p.room() == state && p.options().starred()) n++;
        }
        return n;
    }

    /** The room's starred mobs are all dead: its loot where the last one died ({@code at}; none if null), and it's done. */
    private void clear(RoomState state, Location at) {
        state.cleared = true;
        if (at != null) dropLoot(state, at);
        else for (Door door : state.keys) doors.placeKey(door);
        run.clearedRoom(state.room);
    }

    private void dropLoot(RoomState state, Location at) {
        boolean champion = state.room.type() == RoomType.MINIBOSS;
        List<RoomLoot.Drop> drops = RoomLoot.roll(RoomLoot.blessed(state.room.cells().size(), champion), state.keys, random);
        for (int i = 0; i < drops.size(); i++) {
            RoomLoot.Drop drop = drops.get(i);
            double[] offset = RoomLoot.offset(i);
            Location spot = new Location(world, at.getX() + offset[0], at.getY(), at.getZ() + offset[1]);
            if (drop.door() != null) {
                doors.dropKey(drop.door(), spot);
                continue;
            }
            List<ArmorStand> stands = new ArrayList<>();
            if (drop.kind() == RoomLoot.Kind.SUPERBOOM_TNT) {
                ArmorStand hand = stand(spot.clone().subtract(0, HAND_BELOW, 0));
                ItemStack item = item("SUPERBOOM_TNT");
                if (item != null) hand.getEquipment().setItemInMainHand(item);
                stands.add(hand);
            } else {
                ArmorStand head = stand(spot.clone().subtract(0, HEAD_BELOW, 0));
                head.getEquipment().setHelmet(head(drop.kind() == RoomLoot.Kind.BLESSING ? "Blessing" : "Revive Stone"));
                stands.add(head);
            }
            ArmorStand label = stand(spot.clone().subtract(0, NAME_BELOW, 0));
            label.customName(Text.line(drop.name()));
            label.setCustomNameVisible(true);
            stands.add(label);
            loot.add(new Loot(drop, spot, stands));
        }
    }

    /** A head with the texture the data has for it (a plain head if it has none). */
    private static ItemStack head(String name) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        String hash = data.heads().get(name);
        if (hash != null) Utils.skull(head, Utils.texture(hash));
        return head;
    }

    private static ItemStack item(String id) {
        SkyBlockItem item = ItemRegistry.get(id);
        return item == null ? null : ItemBuilder.build(item);
    }

    private void tickLoot(List<Player> players) {
        for (Loot l : List.copyOf(loot)) {
            Set<UUID> here = new HashSet<>();
            for (Player player : players) {
                Location p = player.getLocation();
                double dx = p.getX() - l.at.getX();
                double dz = p.getZ() - l.at.getZ();
                if (dx * dx + dz * dz > PICKUP_REACH * PICKUP_REACH || Math.abs(p.getY() - l.at.getY()) > 2 || player.isDead()) continue;
                here.add(player.getUniqueId());
                if (pickUp(l, player)) break;
            }
            l.told.retainAll(here);
        }
    }

    /**
     * Picks it up: "Name has obtained Blessing of Wisdom!" to everyone, then the blessing for the team (see
     * {@link DungeonRun#blessingFound}), or the item into their inventory, if there's room ("You don't have
     * enough space in your inventory to pick up this item!" if not, once while they stand on it).
     */
    private boolean pickUp(Loot l, Player player) {
        ItemStack item = null;
        if (l.drop.kind() != RoomLoot.Kind.BLESSING) {
            item = item(l.drop.kind() == RoomLoot.Kind.SUPERBOOM_TNT ? "SUPERBOOM_TNT" : "REVIVE_STONE");
            if (item != null) {
                // "This item will vanish from your inventory at the end of the Dungeon!"
                if (l.drop.kind() == RoomLoot.Kind.REVIVE_STONE) StoredInventory.markNotSaved(item);
                if (!player.getInventory().addItem(item).isEmpty()) {
                    if (l.told.add(player.getUniqueId())) {
                        player.sendMessage(Utils.color("&cYou don't have enough space in your inventory to pick up this item!"));
                    }
                    return false;
                }
            }
        }
        loot.remove(l);
        for (ArmorStand stand : l.stands) {
            stand.remove();
            STANDS.remove(stand.getUniqueId());
        }
        DungeonRun.Member member = run.member(player.getUniqueId());
        run.tell((member != null ? member.display() : player.getName()) + "&f &ehas obtained " + l.drop.name() + "&e!");
        if (l.drop.kind() == RoomLoot.Kind.BLESSING) run.blessingFound(player, l.drop.blessing(), RoomLoot.BLESSING_LEVEL);
        return true;
    }

    // Superboom TNT

    /**
     * A Superboom TNT goes off at this block (where it was placed, against the block clicked): it blows up
     * every tomb and weak wall within {@link #BLAST_REACH}, as recorded (research critic.md 3.3): the
     * explosion sound (master, at the TNT), squid ink over the blocks as they go, and for a tomb a second
     * explosion (blocks, quieter) and angry villagers where its Crypt Undead comes out. Blown walls stay open.
     */
    void superboom(Block at) {
        Location middle = at.getLocation().add(0.5, 0.5, 0.5);
        world.playSound(middle, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 1, (float) random.nextDouble(0.87, 0.92));
        for (Blast crypt : crypts) {
            if (crypt.blown || !crypt.near(at)) continue;
            blow(crypt);
            world.playSound(middleOf(crypt.blocks), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.5f, 1);
            world.spawnParticle(Particle.ANGRY_VILLAGER, crypt.undead, 4, 0.4, 0.4, 0.4, 0);
            cryptsBlown++;
            // Its room is open by now: someone is standing in it.
            open(crypt.room.room);
            spawnAwake(crypt.room, MobKinds.CRYPT_UNDEAD, crypt.undead);
        }
        for (Blast wall : walls) {
            if (!wall.blown && wall.near(at)) blow(wall);
        }
    }

    private void blow(Blast blast) {
        blast.blown = true;
        for (PastePlan.Block b : blast.blocks) {
            Block block = world.getBlockAt(b.x(), b.y(), b.z());
            // A weak wall is its cracked bricks (what the capture had): anything else there now stays.
            if (blast.undead == null && !block.getType().name().contains("CRACKED_STONE_BRICKS") && !blast.after.containsKey(b)) continue;
            if (random.nextDouble() < INK_CHANCE) world.spawnParticle(Particle.SQUID_INK, b.x(), b.y(), b.z(), 1, 0.15, 0.25, 0.15, 0);
            BlockData after = blast.after.get(b);
            if (after != null) block.setBlockData(after, false);
            else block.setType(Material.AIR, false);
        }
    }

    private Location middleOf(List<PastePlan.Block> blocks) {
        double x = 0;
        double y = 0;
        double z = 0;
        for (PastePlan.Block b : blocks) {
            x += b.x() + 0.5;
            y += b.y() + 0.5;
            z += b.z() + 0.5;
        }
        int n = Math.max(1, blocks.size());
        return new Location(world, x / n, y / n, z / n);
    }

    // The end

    /** Everything the rooms put in the world goes. */
    void dispose() {
        disposed = true;
        toSpawn.clear();
        for (RoomState state : rooms.values()) {
            for (Tracked t : state.mobs) {
                TRACKED.remove(t.mob());
                if (!t.dead) Mobs.remove(t.live);
            }
            state.mobs.clear();
        }
        for (Skull skull : skulls) {
            skull.stand.remove();
            STANDS.remove(skull.stand.getUniqueId());
        }
        skulls.clear();
        for (Loot l : loot) {
            for (ArmorStand stand : l.stands) {
                stand.remove();
                STANDS.remove(stand.getUniqueId());
            }
        }
        loot.clear();
    }
}
