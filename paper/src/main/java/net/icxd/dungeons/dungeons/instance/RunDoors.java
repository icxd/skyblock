package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.icxd.dungeons.dungeons.generation.DoorType;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.generation.utils.Position;
import net.icxd.dungeons.utils.Utils;

/**
 * A run's doors and keys, as on Hypixel. The entrance door opens when the run starts. Wither doors
 * (coal) and the Blood Door (red terracotta) stay shut until someone right-clicks them with the
 * team's key; normal and fairy doors are open from the start.
 *
 * <p>On Hypixel a door's key drops from the last starred mob (or the miniboss) of the room before
 * it. Until rooms have mobs it waits in that room from the start, the way Hypixel shows a dropped
 * key: a floating head with its name. Walking into it picks it up for the whole team.
 */
final class RunDoors {
    /** Hypixel's key display: a head on one invisible armor stand, the name on another, this far below the floor. */
    private static final double HEAD_BELOW = 0.71875;
    private static final double NAME_BELOW = 0.46875;
    private static final double PICKUP_REACH = 1.5;
    /** The head turns this much a tick, and bobs this much a tick between these two heights (from recordings). */
    private static final float SPIN = 2;
    private static final double BOB_STEP = 0.01;
    private static final double BOB_TOP = 0.22;
    private static final double BOB_BOTTOM = -0.18;

    /** A key waiting to be picked up: its head spins and bobs, its name stays put. */
    private static final class Key {
        final boolean blood;
        final Location at;
        final ArmorStand head;
        final ArmorStand label;
        private final Location headAt;
        private double bob;
        private boolean rising = true;
        /** A tick's pause at the top and bottom of each bob. */
        private boolean holding;

        Key(boolean blood, Location at, ArmorStand head, ArmorStand label) {
            this.blood = blood;
            this.at = at;
            this.head = head;
            this.label = label;
            this.headAt = head.getLocation();
        }

        void animate() {
            if (holding) {
                holding = false;
            } else {
                bob += rising ? BOB_STEP : -BOB_STEP;
                if (rising ? bob >= BOB_TOP - 1e-9 : bob <= BOB_BOTTOM + 1e-9) {
                    rising = !rising;
                    holding = true;
                }
            }
            headAt.setYaw((headAt.getYaw() + SPIN) % 360);
            head.teleport(headAt.clone().add(0, bob, 0));
        }

        boolean shows(Entity entity) {
            return head.equals(entity) || label.equals(entity);
        }

        void remove() {
            head.remove();
            label.remove();
        }
    }

    private final DungeonRun run;
    private final Plugin plugin;
    private final World world;
    private final RunLayout layout;
    private final Set<Door> shut = new HashSet<>();
    private final List<Key> waiting = new ArrayList<>();
    private int witherKeys;
    private boolean bloodKey;

    RunDoors(DungeonRun run, Plugin plugin, World world, RunLayout layout) {
        this.run = run;
        this.plugin = plugin;
        this.world = world;
        this.layout = layout;
        for (Door door : layout.doors()) {
            Material look = look(door.type());
            if (look == null) continue;
            shut.add(door);
            for (int[] b : layout.doorBlocks(door)) world.getBlockAt(b[0], b[1], b[2]).setType(look, false);
            if (door.type() == DoorType.WITHER || door.type() == DoorType.BLOOD) placeKey(door);
        }
    }

    /** What a shut door is made of; null for doors that are open from the start. */
    private static Material look(DoorType type) {
        return switch (type) {
            case ENTRANCE -> Material.INFESTED_CHISELED_STONE_BRICKS;
            case WITHER -> Material.COAL_BLOCK;
            case BLOOD -> Material.RED_TERRACOTTA;
            default -> null;
        };
    }

    boolean isShut(Door door) {
        return shut.contains(door);
    }

    int witherKeys() {
        return witherKeys;
    }

    boolean hasBloodKey() {
        return bloodKey;
    }

    // Keys

    private void placeKey(Door door) {
        PlacedRoom room = layout.keyRoom(door);
        Location at = keySpot(RunLayout.firstCell(room));
        boolean blood = door.type() == DoorType.BLOOD;
        String name = blood ? "Blood Key" : "Wither Key";
        Location headAt = at.clone().subtract(0, HEAD_BELOW, 0);
        headAt.setYaw(ThreadLocalRandom.current().nextFloat() * 360);
        ArmorStand head = world.spawn(headAt, ArmorStand.class, s -> {
            invisible(s);
            s.getEquipment().setHelmet(DungeonTextures.head(name));
        });
        ArmorStand label = world.spawn(at.clone().subtract(0, NAME_BELOW, 0), ArmorStand.class, s -> {
            invisible(s);
            s.setCustomName(Utils.color((blood ? "&c" : "&8") + name));
            s.setCustomNameVisible(true);
        });
        waiting.add(new Key(blood, at, head, label));
    }

    private static void invisible(ArmorStand stand) {
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setPersistent(false);
        stand.setCanTick(false);
    }

    /** How far from a cell's middle, and how far around the doorways' height, a key looks for floor. */
    private static final int KEY_SEARCH = 8;
    private static final int KEY_ABOVE = 6;
    private static final int KEY_BELOW = 5;

    /**
     * Somewhere to stand near the middle of the cell, about as high as the doorways (rooms with a
     * pit in the middle keep their key on the floor around it); failing that, the first floor down
     * the middle.
     */
    private Location keySpot(Position cell) {
        Location center = layout.center(world, cell);
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        for (int ring = 0; ring <= KEY_SEARCH; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    for (int y = RunLayout.DOOR_BOTTOM + KEY_ABOVE; y >= RunLayout.DOOR_BOTTOM - KEY_BELOW; y--) {
                        if (standable(cx + dx, y, cz + dz)) return new Location(world, cx + dx + 0.5, y, cz + dz + 0.5);
                    }
                }
            }
        }
        for (int y = RunLayout.DOOR_TOP + 8; y > 40; y--) {
            if (standable(cx, y, cz)) return new Location(world, cx + 0.5, y, cz + 0.5);
        }
        return center;
    }

    private boolean standable(int x, int y, int z) {
        Block feet = world.getBlockAt(x, y, z);
        return feet.isPassable() && feet.getRelative(0, 1, 0).isPassable() && feet.getRelative(0, -1, 0).getType().isSolid();
    }

    boolean isKey(Entity entity) {
        return waiting.stream().anyMatch(k -> k.shows(entity));
    }

    /** Every tick: the keys turn, and anyone standing on one picks it up. */
    void tick(List<Player> players) {
        for (Key key : List.copyOf(waiting)) {
            key.animate();
            for (Player player : players) {
                Location p = player.getLocation();
                double dx = p.getX() - key.at.getX();
                double dz = p.getZ() - key.at.getZ();
                if (dx * dx + dz * dz > PICKUP_REACH * PICKUP_REACH || Math.abs(p.getY() - key.at.getY()) > 2) continue;
                pickUp(key, player);
                break;
            }
        }
    }

    private void pickUp(Key key, Player player) {
        waiting.remove(key);
        key.remove();
        DungeonRun.Member member = run.member(player.getUniqueId());
        String who = (member != null ? member.display() : player.getName()) + "&f &ehas obtained ";
        if (key.blood) {
            bloodKey = true;
            run.tell(who + "&cBlood Key&e!");
            run.tell("&e&lRIGHT CLICK &7on the &cBLOOD DOOR&7 to open it. This key can only be used to open &a1&7 door!");
        } else {
            witherKeys++;
            run.tell(who + "&8Wither Key&e!");
            run.tell("&e&lRIGHT CLICK &7on a &8WITHER &7door to open it. This key can only be used to open &a1&7 door!");
        }
    }

    // Opening

    /** A right-click on a block; true if it was a shut door. */
    boolean click(Player player, Block block) {
        Door door = layout.doorAt(block.getX(), block.getY(), block.getZ());
        if (door == null || !shut.contains(door) || door.type() == DoorType.ENTRANCE) return false;
        boolean blood = door.type() == DoorType.BLOOD;
        if (blood ? !bloodKey : witherKeys <= 0) {
            player.sendMessage(Utils.color("&cYou do not have the key for this door!"));
            return true;
        }
        if (blood) bloodKey = false;
        else witherKeys--;
        open(door);
        if (blood) {
            run.tell("&cThe &c&lBLOOD DOOR&c has been opened!");
            run.tell("&5A shiver runs down your spine...");
            run.bloodDoorOpened();
        } else {
            DungeonRun.Member member = run.member(player.getUniqueId());
            run.tell((member != null ? member.rankColor + member.name : "&b" + player.getName()) + "&a opened a &8&lWITHER &adoor!");
        }
        return true;
    }

    /** Opens a door with Hypixel's animation. */
    void open(Door door) {
        if (!shut.remove(door)) return;
        Material look = look(door.type());
        DoorAnimation.play(plugin, world, layout.doorBlocks(door), look, run.players());
        run.doorOpened(door);
    }

    Door entranceDoor() {
        return layout.doorsOfType(DoorType.ENTRANCE).stream().findFirst().orElse(null);
    }

    void dispose() {
        for (Key key : waiting) key.remove();
        waiting.clear();
    }
}
