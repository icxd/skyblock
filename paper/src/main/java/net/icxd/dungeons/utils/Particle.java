package net.icxd.dungeons.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Particle {
    private final org.bukkit.Particle type;
    private final Location location;
    private final float xOffset;
    private final float yOffset;
    private final float zOffset;
    private final float speed;
    private final int count;

    public Particle(org.bukkit.Particle type, Location location, float xOffset, float yOffset, float zOffset, float speed, int count) {
        this.type = type;
        this.location = location;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.zOffset = zOffset;
        this.speed = speed;
        this.count = count;
    }

    /** Shows the particle to one player, even from far away (like the 1.8 packet's long-distance flag). */
    public void send(Player player) {
        player.spawnParticle(type, location, count, xOffset, yOffset, zOffset, speed, null, true);
    }
}
