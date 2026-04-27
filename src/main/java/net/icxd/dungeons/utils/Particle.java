package net.icxd.dungeons.utils;

import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Location;

public class Particle {
    private final Packet<?> packet;
    public Particle(EnumParticle type, Location location, float xOffset, float yOffset, float zOffset, float speed, int count) {
        float x = (float) location.getX();
        float y = (float) location.getY();
        float z = (float) location.getZ();
        this.packet = new PacketPlayOutWorldParticles(type, true, x, y, z, xOffset, yOffset, zOffset, speed, count, null);
    }
    public Packet<?> getPacket() { return packet; }
}