package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lidded;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.profile.ProfileProperty;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.utils.Direction;
import net.icxd.dungeons.dungeons.paste.PastePlan;
import net.icxd.dungeons.dungeons.paste.RoomCapture;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.mob.DataMob;
import net.icxd.dungeons.mob.MobKinds;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.SpawnOptions;
import net.icxd.dungeons.stats.StatsRunnable;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;
import net.kyori.adventure.text.Component;

/**
 * A run's secrets, put out and taken as on Hypixel (research secrets_puzzles.md 1.4-1.9, recordings R1
 * and R2): chests, items on the floor, bats, Wither Essence heads and Redstone Keys, where {@link SecretData}
 * has them, and the rooms' levers. Each room's are put in the world through its {@link RoomFrame}.
 *
 * <ul>
 *   <li>When the run starts every lever is reset and the chests Hypixel has out from the start (behind
 *       levers) are put out; the rest come when someone first walks into their room (Hypixel also has some
 *       wait until you're near: those come with the room here).</li>
 *   <li>A chest holds a blessing (it stays open, harp notes play, its name floats over it and "DUNGEON
 *       BUFF!" follows a second later) or an item (a plain 27-slot "Chest" with it in the middle). Opened
 *       again: "This chest has already been searched!".</li>
 *   <li>An item on the floor counts when it's picked up; a bat when a player kills it (it gives a blessing
 *       or an item); a Wither Essence head when it's right-clicked (an essence for everyone); a Redstone Key
 *       when it's put on its room's Redstone Node (after the key's head has been taken).</li>
 *   <li>A lever: "You hear the sound of something opening...", and its wall slides into the floor the
 *       way doors open, where the wall was recorded (Tic Tac Toe's, Long Hall's); other levers just click.
 *       Pulled again: "This lever has already been used.".</li>
 * </ul>
 * The team's count goes to the action bar (for the room you're in), the tab list and the score; the finder's
 * to EXTRA STATS. Main thread.
 */
final class RunSecrets {
    /** How often it looks which rooms people are in. */
    private static final int FIND_EVERY = 10;
    /** A blessing chest (R1 01:23.8): the harp notes, this many ticks after it opens, at these pitches. */
    private static final long[] HARP_AT = {8, 12, 14, 16, 18};
    private static final float[] HARP_PITCH = {0.794f, 0.889f, 1f, 1.095f, 1.19f};
    /** Then the blessing's name over the chest and its lines (R1: 1.0 s after the chest opened). */
    private static final long BLESSING_AT = 20;
    /** UNKNOWN: in R1 the name went 5.6 s later, which may only have been the player walking away. */
    private static final long NAME_FOR = 112;
    /** A Wither Essence head's rotation in the capture frame: R1's Pirate one was 11 with the room turned twice (the only one recorded). */
    private static final int ESSENCE_ROTATION = 3;
    /** The profile the mods know a Wither Essence head by (recorded, with the Wither Key's texture). */
    static final UUID ESSENCE_PROFILE = UUID.fromString("2865274b-3097-394e-8149-ec629c72d850");
    /** The profile the mods know a Redstone Key head by (Odin's DungeonUtils; never recorded). Its texture is the item's. */
    static final UUID KEY_PROFILE = UUID.fromString("fed95410-aba1-39df-9b95-1d4f361eb66e");
    static final String KEY_ITEM = "SECRET_DUNGEON_REDSTONE_KEY";
    /**
     * A lever's wall (R1 03:51.9-03:54.0, Tic Tac Toe's): its blocks ride bats as a door's do, sinking from
     * half a second on about 0.2 a tick (0.59 every 3 ticks), the barrier gone after 1.3 s (Long Hall's too,
     * 01:44.7) and the entities after 2.1 s; 9 plate clicks 5 ticks apart under it. Long Hall's recording
     * has only the last two clicks, at the same times as Tic Tac Toe's last two, when the player had come to
     * 17-18.5 blocks from it (20-23 for the seven before), so the same clicks are taken to play at every wall.
     */
    private static final DoorAnimation.Timing WALL = new DoorAnimation.Timing(10, 0.2, 26, 42);
    private static final int WALL_CLICKS = 9;
    /** UNKNOWN how often Hypixel says the inventory is full; while they stand on it, this often. */
    private static final long NO_SPACE_EVERY = 2000;
    private static final BlockFace[] SIXTEEN = {BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST_SOUTH_WEST,
            BlockFace.WEST, BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST, BlockFace.NORTH,
            BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST, BlockFace.EAST, BlockFace.EAST_SOUTH_EAST,
            BlockFace.SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST};

    private record Pos(int x, int y, int z) {
        static Pos of(Block block) {
            return new Pos(block.getX(), block.getY(), block.getZ());
        }
    }

    /** A room with secrets: its waypoints in the world, and which of its secrets the team has found. */
    private static final class RoomState {
        final List<Spot> spots = new ArrayList<>();
        final SecretCount count;
        boolean out;
        /** Its Redstone Key's head has been taken (since 0.18.4 it's the team's, not an item). */
        boolean keyTaken;

        RoomState(SecretData.Room data) {
            this.count = new SecretCount(data.secrets());
        }
    }

    /** One waypoint in the world. */
    private static final class Spot {
        final RoomState room;
        /** Its index in the room's waypoints (what {@link SecretCount} knows it by). */
        final int index;
        final SecretData.Waypoint waypoint;
        final Block block;
        /** A chest's facing in the world; null if never recorded. */
        final Direction facing;
        /** A lever's wall, in the world. */
        final List<int[]> wall;
        final int turns;
        /** Found (a secret), taken (a key's head) or pulled (a lever). */
        boolean done;
        Mobs.Live bat;
        Item item;

        Spot(RoomState room, int index, SecretData.Waypoint waypoint, Block block, Direction facing, List<int[]> wall, int turns) {
            this.room = room;
            this.index = index;
            this.waypoint = waypoint;
            this.block = block;
            this.facing = facing;
            this.wall = wall;
            this.turns = turns;
        }

        SecretData.Kind kind() {
            return waypoint.kind();
        }

        Location center() {
            return block.getLocation().add(0.5, 0.5, 0.5);
        }
    }

    /** An item chest's menu; what's left in it when it closes falls out of the chest. */
    static final class ChestMenu implements InventoryHolder {
        private final Block chest;
        private final Inventory inventory;

        private ChestMenu(Block chest, ItemStack item) {
            this.chest = chest;
            this.inventory = Bukkit.createInventory(this, 27, Component.translatable("container.chest"));
            inventory.setItem(13, item);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        void closed() {
            for (ItemStack left : inventory.getContents()) {
                if (left != null && !left.isEmpty()) chest.getWorld().dropItem(chest.getLocation().add(0.5, 1, 0.5), left);
            }
            inventory.clear();
        }
    }

    private final DungeonRun run;
    private final World world;
    private final RunLayout layout;
    private final DungeonFloor floor;
    private final Map<Integer, RoomState> rooms = new HashMap<>();
    private final Map<Pos, Spot> blocks = new HashMap<>();
    private final Map<UUID, Spot> entities = new HashMap<>();
    private final List<Entity> names = new ArrayList<>();
    private final Map<UUID, Long> toldNoSpace = new HashMap<>();
    private final Plugin plugin;
    private int ticks;

    RunSecrets(DungeonRun run, Plugin plugin, World world, RunLayout layout, DungeonFloor floor, SecretData data) {
        this.run = run;
        this.plugin = plugin;
        this.world = world;
        this.layout = layout;
        this.floor = floor;
        for (PlacedRoom room : layout.rooms()) {
            RoomCapture capture = layout.capture(room);
            RoomFrame frame = layout.frame(room);
            SecretData.Room secrets = capture == null || frame == null ? null : data.room(capture.id());
            if (secrets == null) continue;
            RoomState state = new RoomState(secrets);
            rooms.put(room.id(), state);
            List<SecretData.Waypoint> keys = secrets.waypoints().stream().filter(w -> w.kind() == SecretData.Kind.KEY).toList();
            // UNKNOWN which one Hypixel uses where a room has two places for its key's head: one of them.
            SecretData.Waypoint key = keys.isEmpty() ? null : keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
            for (int i = 0; i < secrets.waypoints().size(); i++) {
                SecretData.Waypoint w = secrets.waypoints().get(i);
                if (w.kind() == SecretData.Kind.OTHER || (w.kind() == SecretData.Kind.KEY && w != key)) continue;
                PastePlan.Block at = frame.block(w.x(), w.y(), w.z());
                List<int[]> wall = new ArrayList<>();
                for (int[] b : w.wall()) {
                    PastePlan.Block moved = frame.block(b[0], b[1], b[2]);
                    wall.add(new int[]{moved.x(), moved.y(), moved.z()});
                }
                Direction facing = w.facing() == null ? null : w.facing().rotateClockwise(frame.turns());
                Spot spot = new Spot(state, i, w, world.getBlockAt(at.x(), at.y(), at.z()), facing, wall, frame.turns());
                state.spots.add(spot);
                switch (w.kind()) {
                    case CHEST, WITHER, LEVER, KEY -> blocks.put(Pos.of(spot.block), spot);
                    // The node's redstone block too (Skyblocker counts a click on it).
                    case NODE -> {
                        blocks.put(Pos.of(spot.block), spot);
                        blocks.put(Pos.of(spot.block.getRelative(BlockFace.DOWN)), spot);
                    }
                    default -> {
                    }
                }
            }
        }
    }

    // Counts

    /** Every secret on the floor, as Hypixel counts them room by room. */
    int total() {
        return rooms.values().stream().mapToInt(r -> r.count.total()).sum();
    }

    /** The team's. */
    int found() {
        return rooms.values().stream().mapToInt(r -> r.count.shown()).sum();
    }

    /** Whether the team has found all of a room's secrets (a room without any has). */
    boolean allFound(PlacedRoom room) {
        RoomState state = rooms.get(room.id());
        return state == null || state.count.allFound();
    }

    /** "          &72/5 Secrets" after the mana for the room they're in, if it has secrets. */
    String actionBar(Player player) {
        PlacedRoom room = layout.roomAt(player.getLocation());
        RoomState state = room == null ? null : rooms.get(room.id());
        return state == null ? "" : SecretText.actionBar(state.count.shown(), state.count.total());
    }

    private void count(Player finder, Spot spot) {
        spot.done = true;
        if (!spot.room.count.find(spot.index)) return;
        DungeonRun.Member member = run.member(finder.getUniqueId());
        if (member != null) member.secrets++;
        // At once, as Hypixel's action bar does (0.2-0.3 s after), for everyone (it's the team's count).
        for (Player player : run.players()) StatsRunnable.sendActionBar(player);
    }

    // Putting them out

    /** The run starts: levers reset, and the chests Hypixel has out from the start put out. */
    void start() {
        for (RoomState room : rooms.values()) {
            for (Spot spot : room.spots) {
                // As R1 and R2 show at the start: off, and a floor lever facing west whichever way the room is turned.
                if (spot.kind() == SecretData.Kind.LEVER && spot.block.getBlockData() instanceof Switch lever) {
                    lever.setPowered(false);
                    if (lever.getAttachedFace() == FaceAttachable.AttachedFace.FLOOR) lever.setFacing(BlockFace.WEST);
                    spot.block.setBlockData(lever, false);
                }
                if (spot.kind() == SecretData.Kind.CHEST && spot.waypoint.start()) placeChest(spot);
            }
        }
    }

    /** Every tick: the rooms people have walked into get their secrets. */
    void tick(List<Player> players) {
        if (++ticks % FIND_EVERY != 0) return;
        for (Player player : players) {
            PlacedRoom room = layout.roomAt(player.getLocation());
            RoomState state = room == null ? null : rooms.get(room.id());
            if (state != null && !state.out) putOut(state);
        }
    }

    private void putOut(RoomState room) {
        room.out = true;
        for (Spot spot : room.spots) {
            if (spot.done) continue;
            switch (spot.kind()) {
                case CHEST -> placeChest(spot);
                case WITHER -> placeHead(spot.block, ESSENCE_PROFILE, DungeonTextures.get("Wither Key"), headFacing(ESSENCE_ROTATION, spot.turns));
                // UNKNOWN which way Hypixel turns a Redstone Key's head: as the room.
                case KEY -> placeHead(spot.block, KEY_PROFILE, keyTexture(), headFacing(0, spot.turns));
                case ITEM -> dropItem(spot);
                case BAT -> spawnBat(spot);
                default -> {
                }
            }
        }
    }

    /** A chest where the capture has none (most of them: Hypixel puts them out), facing as recorded or the way that's open. */
    private void placeChest(Spot spot) {
        Material type = spot.block.getType();
        // Where the capture has something solid the waypoint is off; better no chest than a hole in a wall.
        if (type == Material.CHEST || type == Material.TRAPPED_CHEST || !spot.block.isReplaceable()) return;
        BlockData chest = Material.CHEST.createBlockData();
        // UNKNOWN for chests never seen on Hypixel (there's no rule in the recorded ones): towards an open side.
        if (chest instanceof Directional directional) directional.setFacing(face(spot.facing != null ? spot.facing : openSide(spot.block)));
        spot.block.setBlockData(chest, false);
    }

    private static Direction openSide(Block block) {
        for (Direction d : Direction.values()) {
            if (block.getRelative(face(d)).isPassable()) return d;
        }
        return Direction.NORTH;
    }

    private static BlockFace face(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockFace.NORTH;
            case EAST -> BlockFace.EAST;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
        };
    }

    /**
     * Which way a head faces in the world: {@code rotation} is its rotation in the capture frame (sixteenths
     * of a turn, 0 south, 4 west), and each of the room's quarter turns clockwise adds 4 (R1's Pirate
     * Wither Essence: 3 in the frame, 11 with the room turned twice).
     */
    static BlockFace headFacing(int rotation, int turns) {
        return SIXTEEN[Math.floorMod(rotation + 4 * turns, 16)];
    }

    /** A player head with this profile, as the Wither Essence one was recorded (R1 00:35.6). */
    private static void placeHead(Block block, UUID id, ProfileProperty texture, BlockFace facing) {
        if (!block.isReplaceable()) return;
        BlockData head = Material.PLAYER_HEAD.createBlockData();
        if (head instanceof Rotatable rotatable) rotatable.setRotation(facing);
        block.setBlockData(head, false);
        if (block.getState() instanceof Skull skull) {
            ResolvableProfile.Builder profile = ResolvableProfile.resolvableProfile().uuid(id);
            if (texture != null) profile.addProperty(texture);
            skull.setProfile(profile.build());
            skull.update(true, false);
        }
    }

    /** The Redstone Key item's texture (the private items data has it); none if it isn't there. */
    private static ProfileProperty keyTexture() {
        SkyBlockItem key = ItemRegistry.get(KEY_ITEM);
        return key == null || key.skin() == null ? null : new ProfileProperty("textures", key.skin());
    }

    /** An item secret, dropped where it's found the way Hypixel drops it (R2 01:55.3: a little up). */
    private void dropItem(Spot spot) {
        ItemStack stack = item(SecretRewards.roll(SecretRewards.Source.ITEM, floor, ThreadLocalRandom.current()).item(), SecretRewards.Source.ITEM);
        if (stack == null) return;
        Location at = spot.block.getLocation().add(0.5, 0.1, 0.5);
        spot.item = world.dropItem(at, stack, item -> {
            item.setUnlimitedLifetime(true);
            item.setCanMobPickup(false);
            item.setPersistent(false);
            item.setVelocity(new Vector(0, 0.2, 0));
        });
        entities.put(spot.item.getUniqueId(), spot);
    }

    private void spawnBat(Spot spot) {
        try {
            spot.bat = Mobs.spawn(MobKinds.SECRET_BAT, floor, SpawnOptions.NONE, spot.center());
        } catch (IllegalArgumentException e) {
            return;
        }
        entities.put(spot.bat.entity().getUniqueId(), spot);
    }

    /** The item with this id, or another of the source's if there's no such item here; null if there are none at all. */
    private ItemStack item(String id, SecretRewards.Source source) {
        SkyBlockItem item = ItemRegistry.get(id);
        if (item == null) {
            for (String other : SecretRewards.items(source, floor)) {
                item = ItemRegistry.get(other);
                if (item != null) break;
            }
        }
        return item == null ? null : ItemBuilder.build(item);
    }

    // Taking them

    /** Whether the block is one of the secrets, keys or levers (so nothing else happens when it's clicked). */
    boolean isSecret(Block block) {
        return blocks.containsKey(Pos.of(block));
    }

    /** A right click on a secret's block (or a lever's, a key's); true if it's one. */
    boolean click(Player player, Block block) {
        Spot spot = blocks.get(Pos.of(block));
        if (spot == null) return false;
        switch (spot.kind()) {
            case CHEST -> {
                if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return false;
                if (spot.done) player.sendMessage(Utils.color(SecretText.ALREADY_SEARCHED));
                else openChest(player, spot);
            }
            case WITHER -> {
                if (spot.done || !isHead(block)) return false;
                takeEssence(player, spot);
            }
            case LEVER -> {
                if (block.getType() != Material.LEVER) return false;
                if (spot.done) player.sendMessage(Utils.color(SecretText.LEVER_USED));
                else pull(player, spot);
            }
            case KEY -> {
                if (spot.done || !isHead(block)) return false;
                // UNKNOWN what Hypixel says or plays: the head goes, and the room's node takes it.
                spot.done = true;
                spot.room.keyTaken = true;
                block.setType(Material.AIR, false);
            }
            case NODE -> {
                // UNKNOWN what Hypixel says without the key, or when it's put down.
                if (spot.done || !spot.room.keyTaken) return true;
                placeHead(spot.block, KEY_PROFILE, keyTexture(), headFacing(0, spot.turns));
                count(player, spot);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private static boolean isHead(Block block) {
        return block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD;
    }

    private void openChest(Player player, Spot spot) {
        count(player, spot);
        SecretRewards.Reward reward = SecretRewards.roll(SecretRewards.Source.CHEST, floor, ThreadLocalRandom.current());
        ItemStack item = reward.isBlessing() ? null : item(reward.item(), SecretRewards.Source.CHEST);
        if (item != null) {
            openLid(spot.block);
            player.openInventory(new ChestMenu(spot.block, item).getInventory());
            return;
        }
        // An item that isn't there (no items data): a blessing instead.
        blessingChest(player, spot.block, reward.isBlessing() ? reward.blessing() : Blessing.random(ThreadLocalRandom.current()),
                reward.isBlessing() ? reward.level() : 1);
    }

    /** Its lid opens for good (Hypixel never closes a secret chest), with the sound (R1: volume 0.5, pitch 0.9 to 0.97). */
    private void openLid(Block chest) {
        if (chest.getState(false) instanceof Lidded lid) lid.open();
        world.playSound(chest.getLocation().add(0.5, 0.5, 0.5), Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.5f,
                0.9f + ThreadLocalRandom.current().nextFloat() * 0.07f);
    }

    /**
     * A chest with a blessing in it, a secret's or a puzzle's (R1 01:23.8, 03:29.7): it opens for good, five
     * harp notes, and a second later the blessing's name over the chest and the "DUNGEON BUFF!" lines (no run
     * time, unlike one picked up from a room).
     */
    void blessingChest(Player player, Block chest, Blessing blessing, int level) {
        openLid(chest);
        Location center = chest.getLocation().add(0.5, 0.5, 0.5);
        for (int i = 0; i < HARP_AT.length; i++) {
            float pitch = HARP_PITCH[i];
            run.later(HARP_AT[i], () -> world.playSound(center, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.BLOCKS, 1, pitch));
        }
        run.later(BLESSING_AT, () -> {
            // At the chest's middle, 0.125 lower (R1 entity 502846).
            ArmorStand name = world.spawn(chest.getLocation().add(0.5, -0.125, 0.5), ArmorStand.class, stand -> {
                // Not a marker, as recorded: its name floats over the chest.
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setInvulnerable(true);
                stand.setPersistent(false);
                stand.customName(Text.line("&d" + blessing.displayName()));
                stand.setCustomNameVisible(true);
            });
            names.add(name);
            run.later(NAME_FOR, () -> {
                name.remove();
                names.remove(name);
            });
            if (player.isOnline()) run.blessingFound(player, blessing, level, null);
        });
    }

    /** "You found a Wither Essence!": an essence for everyone in the run, on the profile they play on. */
    private void takeEssence(Player player, Spot spot) {
        count(player, spot);
        spot.block.setType(Material.AIR, false);
        DungeonRun.Member finder = run.member(player.getUniqueId());
        for (Player member : run.players()) {
            member.sendMessage(Utils.color(SecretText.witherEssence(member.equals(player) ? null : finder == null ? player.getName() : finder.display())));
            User user = User.ifLoaded(member.getUniqueId());
            if (user != null) {
                addEssence(user.profile(), EssenceType.WITHER, 1);
                user.save();
            }
        }
    }

    /** Onto the profile's {@code dungeons.essence.<type>}, where the room mobs' essences go too. */
    static void addEssence(Document profile, EssenceType type, int amount) {
        if (profile == null || amount <= 0) return;
        Document dungeons = profile.get("dungeons", Document.class);
        if (dungeons == null) profile.put("dungeons", dungeons = new Document());
        Document essence = dungeons.get("essence", Document.class);
        if (essence == null) dungeons.put("essence", essence = new Document());
        String key = type.name().toLowerCase(Locale.ROOT);
        int before = essence.get(key) instanceof Number n ? n.intValue() : 0;
        essence.put(key, before + amount);
    }

    /** A lever (R1 01:44.7 and 03:51.9): it clicks, and its wall slides away if it has one we know. */
    private void pull(Player player, Spot spot) {
        spot.done = true;
        if (spot.block.getBlockData() instanceof Switch lever) {
            lever.setPowered(true);
            spot.block.setBlockData(lever, false);
        }
        world.playSound(spot.center(), Sound.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3f, 0.587f);
        // At whoever pulled it, not at the lever (R1: (-123.5, 69, -194.25) for a lever at (-126, 70, -197)).
        world.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, SoundCategory.BLOCKS, 1, 1.698f);
        // Only the one who pulled it (both recorded runs were solo).
        player.sendMessage(Utils.color(SecretText.SOMETHING_OPENING));
        if (spot.wall.isEmpty()) return;
        List<BlockData> looks = new ArrayList<>();
        for (int[] b : spot.wall) looks.add(world.getBlockAt(b[0], b[1], b[2]).getBlockData());
        DoorAnimation.play(plugin, world, spot.wall, looks, run.players(), WALL);
        Location plates = platesAt(spot.wall).toLocation(world);
        for (int i = 0; i < WALL_CLICKS; i++) {
            float volume = i < 2 ? 2 : 1;
            run.later(1 + 5L * i, () -> world.playSound(plates, Sound.BLOCK_WOODEN_PRESSURE_PLATE_CLICK_ON, SoundCategory.MASTER, volume, 0.492f));
        }
    }

    /**
     * Where a wall's plate clicks are: the middle of the block under its lowest north-west corner (R1:
     * (-83.5, 69.5, -191.5) for Tic Tac Toe's at x -84..-82, y 70-71, z -192; (-126.5, 83.5, -179.5) for
     * Long Hall's at x -127..-125, y 84-88, z -180).
     */
    static Vector platesAt(List<int[]> wall) {
        int x = wall.stream().mapToInt(b -> b[0]).min().orElseThrow();
        int y = wall.stream().mapToInt(b -> b[1]).min().orElseThrow();
        int z = wall.stream().mapToInt(b -> b[2]).min().orElseThrow();
        return new Vector(x + 0.5, y - 0.5, z + 0.5);
    }

    // Items and bats

    /** One of the blessing names over opened chests (nothing may be put on it). */
    boolean isName(Entity entity) {
        return names.contains(entity);
    }

    boolean isSecret(Item item) {
        Spot spot = entities.get(item.getUniqueId());
        return spot != null && spot.item != null;
    }

    /** They're picking up an item secret. */
    void pickedUp(Player player, Item item) {
        Spot spot = entities.remove(item.getUniqueId());
        if (spot == null || spot.done) return;
        spot.item = null;
        count(player, spot);
    }

    /** They stand on an item secret with no room for it (recorded). */
    void noSpace(Player player) {
        long now = System.currentTimeMillis();
        Long last = toldNoSpace.get(player.getUniqueId());
        if (last != null && now - last < NO_SPACE_EVERY) return;
        toldNoSpace.put(player.getUniqueId(), now);
        player.sendMessage(Utils.color(SecretText.NO_SPACE));
    }

    /**
     * A secret bat died. The killer gets the secret and a blessing or an item (into their inventory, as dungeon
     * drops go); with nobody to kill it (the wiki: a trap, lava, a mob) it's lost.
     */
    void batDied(DataMob bat, Player killer, Location at) {
        for (RoomState room : rooms.values()) {
            for (Spot spot : room.spots) {
                if (spot.bat == null || spot.bat.type() != bat) continue;
                entities.remove(spot.bat.entity().getUniqueId());
                spot.bat = null;
                spot.done = true;
                if (killer == null || !killer.isOnline()) return;
                count(killer, spot);
                SecretRewards.Reward reward = SecretRewards.roll(SecretRewards.Source.BAT, floor, ThreadLocalRandom.current());
                ItemStack item = reward.isBlessing() ? null : item(reward.item(), SecretRewards.Source.BAT);
                if (item != null) {
                    for (ItemStack left : killer.getInventory().addItem(item).values()) world.dropItemNaturally(at, left);
                } else {
                    Blessing blessing = reward.isBlessing() ? reward.blessing() : Blessing.random(ThreadLocalRandom.current());
                    run.blessingFound(killer, blessing, reward.isBlessing() ? reward.level() : 1, null);
                }
                return;
            }
        }
    }

    /** Takes everything it put in the world away again (the blocks go with the next floor). */
    void dispose() {
        for (Spot spot : entities.values()) {
            if (spot.bat != null) Mobs.remove(spot.bat);
            if (spot.item != null) spot.item.remove();
        }
        entities.clear();
        for (Entity name : names) name.remove();
        names.clear();
    }
}
