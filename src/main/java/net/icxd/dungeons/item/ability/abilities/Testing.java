package net.icxd.dungeons.item.ability.abilities;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.ability.AbilityType;
import net.icxd.dungeons.utils.Particle;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public class Testing extends Ability {
    public Testing() {
        super("Testing", AbilityType.ABILITY, AbilityActivation.LEFT_CLICK, "&7Example ability.", 0, 0, 0);
    }

    @Override
    public void activate(Player player, SkyBlockItem item) {
        final Location[] currentLocation = {player.getLocation()};
        ArrayList<LivingEntity> checkedEntities = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Entity closestEntity = getEntity(player, currentLocation[0], checkedEntities);
                    if (closestEntity == null) {
                        cancel();
                        return;
                    }
                    Location entityLocation = closestEntity.getLocation();
                    spawnParticles(currentLocation[0], entityLocation);
                    currentLocation[0] = entityLocation;
                    checkedEntities.add((LivingEntity) closestEntity);

                    Location behindEntity = entityLocation.clone().add(entityLocation.getDirection().multiply(-1));
                    player.teleport(behindEntity);
                }
            }.runTaskLater(Dungeons.getInstance(), i * 10);
        }

    }

    private Entity getEntity(Player player, Location currentLocation, ArrayList<LivingEntity> checkedEntities) {
        return player.getWorld().getNearbyEntities(currentLocation, 10, 10, 10).stream()
                .filter(entity -> !entity.isInsideVehicle() || entity.getVehicle().getPassenger() != entity)
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> !(entity instanceof Player))
                .filter(entity -> !(entity instanceof ArmorStand))
                .filter(entity -> !checkedEntities.contains(entity))
                .min(Comparator.comparingDouble(entity -> entity.getLocation().distance(currentLocation)))
                .orElse(null);
    }

    private void spawnParticles(Location location1, Location location2) {
        double distance = location1.distance(location2);
        double x1 = location1.getX();
        double y1 = location1.getY();
        double z1 = location1.getZ();
        double x2 = location2.getX();
        double y2 = location2.getY();
        double z2 = location2.getZ();
        double x = x2 - x1;
        double y = y2 - y1;
        double z = z2 - z1;
        for (double i = 0; i < distance; i += 0.1) {
            double x3 = x1 + (x * i / distance);
            double y3 = y1 + (y * i / distance);
            double z3 = z1 + (z * i / distance);
            Color color = new Color(255, 0, 0);
            PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(EnumParticle.REDSTONE, true, (float) x3, (float) y3, (float) z3, color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 1, 0, null);

//            Packet<?> packet = new Particle(EnumParticle.REDSTONE, new Location(location1.getWorld(), x3, y3, z3), 0, 0, 0, 0, 10).getPacket();
            for (Player player : location1.getWorld().getPlayers()) {
                ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
            }
        }
    }
}
