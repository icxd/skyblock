package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.util.Vector;

/**
 * The Blood Room's display cases: undead heads in niches along the walls, each in front of a
 * stained glass pane with fire behind it. The Watcher fetches his undeads from them. As on Hypixel
 * (recordings of two runs), the cases are 12 blocks out from the middle: along two walls at -4, 0
 * and 4, along the other two near the corners at -9 and 9, each at three heights. They're found
 * from the glass, so any rotation of the room works.
 *
 * <p>The heads are invisible armor stands, 1.25 below the glass, facing into the room. Hypixel had
 * a head in every case along the walls and in some of those near the corners.
 */
final class DisplayCases {
    private static final int OUT = 12;
    private static final int[] ALONG = {-9, -4, 0, 4, 9};
    private static final int CORNER = 9;
    private static final double CORNER_FILLED = 1 / 3.0;
    private static final double BELOW_GLASS = 1.25;
    /** How high above the doorways' floor to look for glass. */
    private static final int GLASS_FROM = 1;
    private static final int GLASS_TO = 15;

    /** One case: where its head stands, which wall it's on, and whose head it is. */
    static final class Case {
        final Location at;
        final int wall;
        final UndeadType type;
        ArmorStand head;

        Case(Location at, int wall, UndeadType type) {
            this.at = at;
            this.wall = wall;
            this.type = type;
        }

        /** Shows its head (in the case, or wherever it's being taken from). */
        ArmorStand spawnHead() {
            if (head == null) head = at.getWorld().spawn(at, ArmorStand.class, s -> {
                s.setVisible(false);
                s.setGravity(false);
                s.setMarker(true);
                s.setInvulnerable(true);
                s.setPersistent(false);
                s.getEquipment().setHelmet(DungeonTextures.head(type.displayName));
            });
            return head;
        }
    }

    private final List<Case> full = new ArrayList<>();

    private DisplayCases() {
    }

    /** Finds the cases around a Blood Room's middle (at the doorways' floor) and fills them. */
    static DisplayCases place(World world, Location middle) {
        DisplayCases cases = new DisplayCases();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int cx = middle.getBlockX();
        int cz = middle.getBlockZ();
        int[][] walls = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int wall = 0; wall < walls.length; wall++) {
            int dx = walls[wall][0];
            int dz = walls[wall][1];
            // Along the wall.
            int ax = -dz;
            int az = dx;
            for (int along : ALONG) {
                int gx = cx + (OUT + 1) * dx + along * ax;
                int gz = cz + (OUT + 1) * dz + along * az;
                for (int y = middle.getBlockY() + GLASS_FROM; y <= middle.getBlockY() + GLASS_TO; y++) {
                    if (!isGlass(world.getBlockAt(gx, y, gz))) continue;
                    if (Math.abs(along) == CORNER && random.nextDouble() >= CORNER_FILLED) continue;
                    Location at = new Location(world, cx + OUT * dx + along * ax + 0.5, y - BELOW_GLASS, cz + OUT * dz + along * az + 0.5);
                    at.setDirection(new Vector(-dx, 0, -dz));
                    Case c = new Case(at, wall, UndeadType.random());
                    c.spawnHead();
                    cases.full.add(c);
                }
            }
        }
        return cases;
    }

    private static boolean isGlass(Block block) {
        return block.getType().name().endsWith("_STAINED_GLASS");
    }

    boolean isEmpty() {
        return full.isEmpty();
    }

    /** A case to fetch an undead from, preferably on another wall than the last one; it's empty after that. */
    Case take(int lastWall) {
        if (full.isEmpty()) return null;
        List<Case> others = full.stream().filter(c -> c.wall != lastWall).toList();
        List<Case> from = others.isEmpty() ? full : others;
        Case c = from.get(ThreadLocalRandom.current().nextInt(from.size()));
        full.remove(c);
        return c;
    }

    void dispose() {
        for (Case c : full) {
            if (c.head != null) c.head.remove();
        }
        full.clear();
    }
}
