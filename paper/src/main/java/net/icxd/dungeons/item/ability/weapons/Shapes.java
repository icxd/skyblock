package net.icxd.dungeons.item.ability.weapons;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * Where an ability reaches, with no world in it: whether a mob's hitbox is in a ball, a cone or along a
 * line. How Hypixel measures it (from a mob's feet, its middle or any part of it) is UNKNOWN: any part of
 * the hitbox counts for a ball or a line, so a big mob isn't missed for its size, and a cone goes by the
 * hitbox's middle.
 */
final class Shapes {
    private Shapes() {
    }

    /** Whether any part of the box is within {@code radius} of {@code center}. */
    static boolean inBall(Vector center, double radius, BoundingBox box) {
        double dx = Math.max(Math.max(box.getMinX() - center.getX(), 0), center.getX() - box.getMaxX());
        double dy = Math.max(Math.max(box.getMinY() - center.getY(), 0), center.getY() - box.getMaxY());
        double dz = Math.max(Math.max(box.getMinZ() - center.getZ(), 0), center.getZ() - box.getMaxZ());
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    /**
     * Whether the box's middle is in the cone from {@code apex} along {@code direction}, {@code length}
     * long and {@code degrees} wide (60 for Ice Spray's "60º wide": 30 either side of the middle).
     */
    static boolean inCone(Vector apex, Vector direction, double length, double degrees, BoundingBox box) {
        Vector to = box.getCenter().subtract(apex);
        double distance = to.length();
        if (distance > length) return false;
        if (distance < 1e-6) return true;
        double cos = to.dot(direction.clone().normalize()) / distance;
        return cos >= Math.cos(Math.toRadians(degrees / 2));
    }

    /**
     * How far along the line from {@code start} (going {@code direction}, at most {@code length}) it first
     * touches the box grown by {@code width} on every side; -1 if it doesn't.
     */
    static double along(Vector start, Vector direction, double length, double width, BoundingBox box) {
        if (box.clone().expand(width).contains(start)) return 0;
        var hit = box.clone().expand(width).rayTrace(start, direction.clone().normalize(), length);
        return hit == null ? -1 : hit.getHitPosition().distance(start);
    }

    /** The point {@code distance} blocks ahead of {@code feet} along the ground, the way {@code yaw} faces. */
    static Vector ahead(Vector feet, float yaw, double distance) {
        double radians = Math.toRadians(yaw);
        return feet.clone().add(new Vector(-Math.sin(radians), 0, Math.cos(radians)).multiply(distance));
    }
}
